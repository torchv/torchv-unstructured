/*
 * Copyright © 2025-2030 TorchV(xiaoymin@mengjia.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.torchv.infra.unstructured.parser.word;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.core.DocumentImage;
import com.torchv.infra.unstructured.core.DocumentResult;
import com.torchv.infra.unstructured.core.UnstructuredConfig;
import com.torchv.infra.unstructured.util.DocumentParserUtils;
import com.torchv.infra.unstructured.util.UnstructuredUtils;
import com.torchv.infra.unstructured.util.WordMarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * TorchV Unstructured 高级文档解析器
 * 
 * 提供更强大和灵活的文档解析功能，支持自定义配置、批量处理、异步操作等高级特性。
 * 与 {@link UnstructuredWord} 相比，本类提供更多控制选项。
 * 
 * <h3>基本使用：</h3>
 * 
 * <pre>{@code
 * // 使用默认配置
 * try (WordParser parser = new WordParser()) {
 *     DocumentResult result = parser.parse("document.docx");
 * }
 * 
 * // 使用自定义配置
 * try (WordParser parser = WordParser.builder()
 *         .withConfig(UnstructuredConfig.ragOptimized())
 *         .build()) {
 *     DocumentResult result = parser.parse("document.docx");
 * }
 * }</pre>
 * 
 * <h3>批量处理：</h3>
 * 
 * <pre>{@code
 * try (WordParser parser = new WordParser()) {
 *     List<String> files = Arrays.asList("doc1.docx", "doc2.pdf");
 *     BatchResult batchResult = parser.parseBatch(files);
 * }
 * }</pre>
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Slf4j
public class WordParser implements AutoCloseable {
    
    private final UnstructuredConfig config;
    // private final ExecutorService executorService;
    
    // ==================== 构造方法 ====================
    
    /**
     * 使用默认配置创建解析器
     */
    public WordParser() {
        this(UnstructuredConfig.defaultConfig());
    }
    
    /**
     * 使用指定配置创建解析器
     * 
     * @param config 解析配置
     */
    public WordParser(UnstructuredConfig config) {
        this.config = config != null ? config : UnstructuredConfig.defaultConfig();
        this.config.validate();
        
        // 创建线程池
        /*
         * this.executorService = Executors.newFixedThreadPool( this.config.getMaxConcurrentThreads(), r -> { Thread t = new Thread(r, "DocumentParser-Worker"); t.setDaemon(true); return t; });
         */
        
        log.info("DocumentParser initialized with config: {}", this.config);
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建解析器构建器
     * 
     * @return 构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 创建RAG优化的解析器
     * 
     * @return RAG优化的解析器实例
     */
    public static WordParser ragOptimized() {
        return new WordParser(UnstructuredConfig.ragOptimized());
    }
    
    /**
     * 创建高性能解析器
     * 
     * @return 高性能解析器实例
     */
    public static WordParser highPerformance() {
        return new WordParser(UnstructuredConfig.highPerformance());
    }
    
    // ==================== 主要解析方法 ====================
    
    /**
     * 解析文档
     * 
     * @param filePath 文档文件路径
     * @return 解析结果
     */
    public DocumentResult parse(String filePath) {
        return parse(new File(filePath));
    }
    
    /**
     * 解析文档
     * 
     * @param file 文档文件
     * @return 解析结果
     */
    public DocumentResult parse(File file) {
        long startTime = System.currentTimeMillis();
        
        try {
            validateFile(file);
            
            DocumentResult.DocumentResultBuilder resultBuilder = DocumentResult.builder()
                    .filePath(file.getAbsolutePath())
                    .fileName(file.getName())
                    .fileSize(file.length());
            
            // 解析主要内容
            String content = parseContent(file);
            resultBuilder.content(content);
            
            // 提取表格（如果启用）
            if (config.isEnableTableExtraction()) {
                List<String> tables = extractTables(file);
                resultBuilder.tables(tables);
            }
            
            // 提取图像（如果启用）
            if (config.isEnableImageExtraction()) {
                List<DocumentImage> images = extractImages(file);
                resultBuilder.images(images);
            }
            
            // 提取元数据（如果启用）
            if (config.isIncludeMetadata()) {
                Map<String, Object> metadata = extractMetadata(file);
                resultBuilder.metadata(metadata);
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            resultBuilder.processingTimeMs(processingTime);
            
            DocumentResult result = resultBuilder.build();
            log.info("Successfully parsed document: {} in {} ms", file.getName(), processingTime);
            
            return result;
            
        } catch (Exception e) {
            return handleParsingError(file, e, startTime);
        }
    }
    
    /**
     * 解析输入流
     * 
     * @param inputStream 输入流
     * @param fileName    文件名（用于类型判断）
     * @return 解析结果
     */
    public DocumentResult parse(InputStream inputStream, String fileName) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 创建临时文件
            File tempFile = createTempFile(inputStream, fileName);
            
            try {
                DocumentResult result = parse(tempFile);
                
                // 更新文件名信息
                return result.toBuilder()
                        .fileName(fileName)
                        .filePath(fileName)
                        .build();
                
            } finally {
                // 清理临时文件
                FileUtil.del(tempFile);
            }
            
        } catch (Exception e) {
            return handleStreamParsingError(fileName, e, startTime);
        }
    }
    
    /**
     * 异步解析文档
     * 
     * @param filePath 文档文件路径
     * @return 解析结果的Future
     */
    /*
     * public CompletableFuture<DocumentResult> parseAsync(String filePath) { return parseAsync(new File(filePath)); }
     */
    
    /**
     * 异步解析文档
     * 
     * @param file 文档文件
     * @return 解析结果的Future
     */
    /*
     * public CompletableFuture<DocumentResult> parseAsync(File file) { return CompletableFuture.supplyAsync(() -> parse(file), executorService); }
     */
    
    // ==================== 批量处理方法 ====================
    
    /**
     * 批量解析文档
     * 
     * @param filePaths 文档文件路径列表
     * @return 批量处理结果
     */
    /*
     * public BatchResult parseBatch(List<String> filePaths) { List<File> files = filePaths.stream() .map(File::new) .collect(java.util.stream.Collectors.toList()); return parseBatchFiles(files); }
     */
    
    /**
     * 批量解析文档文件
     * 
     * @param files 文档文件列表
     * @return 批量处理结果
     */
    /*
     * public BatchResult parseBatchFiles(List<File> files) { long startTime = System.currentTimeMillis();
     * 
     * List<CompletableFuture<DocumentResult>> futures = files.stream() .map(this::parseAsync) .toList();
     * 
     * // 等待所有任务完成 CompletableFuture<Void> allFutures = CompletableFuture.allOf( futures.toArray(new CompletableFuture[0]));
     * 
     * try { if (config.getProcessingTimeoutSeconds() > 0) { allFutures.get(config.getProcessingTimeoutSeconds(), TimeUnit.SECONDS); } else { allFutures.get(); } } catch (TimeoutException e) {
     * log.warn("Batch processing timed out after {} seconds", config.getProcessingTimeoutSeconds()); } catch (Exception e) { log.error("Error in batch processing", e); }
     * 
     * // 收集结果 List<DocumentResult> results = new ArrayList<>(); List<String> errors = new ArrayList<>();
     * 
     * for (CompletableFuture<DocumentResult> future : futures) { try { DocumentResult result = future.get(1, TimeUnit.SECONDS); if (result.isSuccess()) { results.add(result); } else {
     * errors.add(result.getErrorMessage()); } } catch (Exception e) { errors.add("Processing failed: " + e.getMessage()); } }
     * 
     * long totalTime = System.currentTimeMillis() - startTime;
     * 
     * return BatchResult.builder() .totalFiles(files.size()) .successCount(results.size()) .errorCount(errors.size()) .results(results) .errors(errors) .totalProcessingTimeMs(totalTime) .build(); }
     */
    
    // ==================== 专用解析方法 ====================
    
    /**
     * 仅提取Word文档表格
     * 
     * @param filePath Word文档路径
     * @return 表格列表
     */
    public List<String> extractWordTables(String filePath) {
        return extractWordTables(new File(filePath));
    }
    
    /**
     * 仅提取Word文档表格
     * 
     * @param file Word文档文件
     * @return 表格列表
     */
    public List<String> extractWordTables(File file) {
        try {
            if (!UnstructuredUtils.isWordDocument(file)) {
                return Collections.emptyList();
            }
            
            // 使用FileMagic检测实际文件格式
            FileMagic fileMagic = com.torchv.infra.unstructured.util.FileMagicUtils.checkMagic(file);
            
            // 如果FileMagic检测失败，根据文件扩展名判断
            if (fileMagic == FileMagic.UNKNOWN) {
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".docx")) {
                    fileMagic = FileMagic.OOXML;
                } else if (fileName.endsWith(".doc")) {
                    fileMagic = FileMagic.OLE2;
                }
            }
            
            log.debug("检测到文件格式: {}, 文件: {}", fileMagic, file.getName());
            
            // 根据文件格式选择相应的解析器
            switch (fileMagic) {
                case OOXML -> {
                    // DOCX格式 - 使用XWPFDocument和WordTableParser
                    try (
                            FileInputStream fis = new FileInputStream(file);
                            XWPFDocument document = new XWPFDocument(fis)) {
                        
                        WordTableParser parser = new WordTableParser();
                        List<String> tables = parser.parseToHtmlList(document);
                        log.debug("DOCX文档提取到{}个表格", tables.size());
                        return tables;
                    }
                }
                case OLE2 -> {
                    // DOC格式 - 使用HWPFDocument和DocumentTableParser
                    try (
                            FileInputStream fis = new FileInputStream(file);
                            HWPFDocument document = new HWPFDocument(fis)) {
                        
                        List<String> tables = com.torchv.infra.unstructured.handler.table.DocumentTableParser
                                .parseAllTablesToHtml(document);
                        log.debug("DOC文档提取到{}个表格", tables.size());
                        return tables;
                    }
                }
                default -> {
                    log.warn("不支持的Word文档格式: {}, 文件: {}", fileMagic, file.getName());
                    return Collections.emptyList();
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to extract tables from Word document: {}", file.getAbsolutePath(), e);
            if (config.getErrorHandlingStrategy() == UnstructuredConfig.ErrorHandlingStrategy.FAIL_FAST) {
                throw new RuntimeException("Table extraction failed", e);
            }
            return Collections.emptyList();
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private String parseContent(File file) throws IOException {
        File markdownFile = null;
        try {
            if (config.isTableAsHtml()) {
                markdownFile = WordMarkdownUtils.toMarkdownWithHtmlTable(file);
                return readFileContent(markdownFile);
            } else {
                markdownFile = WordMarkdownUtils.toMarkdown(file);
                return readFileContent(markdownFile);
            }
        } finally {
            FileUtil.del(markdownFile); // 确保临时文件被删除
        }
    }
    
    private List<String> extractTables(File file) {
        if (UnstructuredUtils.isWordDocument(file)) {
            return extractWordTables(file);
        }
        // 其他格式的表格提取可以在这里扩展
        return Collections.emptyList();
    }
    
    private List<DocumentImage> extractImages(File file) {
        try {
            if (UnstructuredUtils.isWordDocument(file)) {
                List<com.torchv.infra.unstructured.parser.word.model.DocumentImage> tikaImages = DocumentParserUtils
                        .extractPictures(file);
                
                return tikaImages.stream()
                        .map(this::convertToDocumentImage)
                        .collect(java.util.stream.Collectors.toList());
            }
            // 其他格式的图像提取可以在这里扩展
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to extract images from: {}", file.getAbsolutePath(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 转换 Tika 的 DocumentImage 到核心 DocumentImage
     */
    private DocumentImage convertToDocumentImage(
                                                 com.torchv.infra.unstructured.parser.word.model.DocumentImage tikaImage) {
        // 读取文件信息
        String name = tikaImage.getSourceName() != null ? tikaImage.getSourceName() : "unknown";
        String format = tikaImage.getSuffix() != null ? tikaImage.getSuffix() : "unknown";
        long size = 0;
        int width = 0;
        int height = 0;
        String data = "";
        
        // 如果有文件，获取基本信息
        if (tikaImage.getPicFile() != null && tikaImage.getPicFile().exists()) {
            size = tikaImage.getPicFile().length();
            name = tikaImage.getPicFile().getName();
            
            // 尝试转换为 Base64 (简化版本，实际应该根据需要实现)
            try {
                byte[] fileBytes = Files.readAllBytes(tikaImage.getPicFile().toPath());
                data = java.util.Base64.getEncoder().encodeToString(fileBytes);
            } catch (Exception e) {
                log.warn("Failed to convert image to base64: {}", tikaImage.getPicFile().getAbsolutePath(), e);
            }
        }
        
        return DocumentImage.builder()
                .name(name)
                .format(format)
                .data(data)
                .width(width)
                .height(height)
                .size(size)
                .position("") // Tika image 没有位置信息
                .altText("") // Tika image 没有alt文本
                .build();
    }
    
    private Map<String, Object> extractMetadata(File file) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileName", file.getName());
        metadata.put("fileSize", file.length());
        metadata.put("lastModified", new Date(file.lastModified()));
        metadata.put("extension", UnstructuredUtils.getFileExtension(file.getName()));
        // 可以扩展更多元数据提取
        return metadata;
    }
    
    private void validateFile(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("Not a file: " + file.getAbsolutePath());
        }
        
        long fileSizeMB = file.length() / (1024 * 1024);
        if (fileSizeMB > config.getMaxDocumentSizeInMB()) {
            throw new IllegalArgumentException(
                    String.format("File too large: %d MB > %d MB limit",
                            fileSizeMB, config.getMaxDocumentSizeInMB()));
        }
    }
    
    private File createTempFile(InputStream inputStream, String fileName) throws IOException {
        String extension = UnstructuredUtils.getFileExtension(fileName);
        File tempFile = File.createTempFile("torchv_parser_", "." + extension);
        tempFile.deleteOnExit();
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }
    
    private String readFileContent(File file) throws IOException {
        if (file == null || !file.exists()) {
            return "";
        }
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }
    
    private DocumentResult handleParsingError(File file, Exception e, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        if (config.isEnableVerboseLogging()) {
            log.error("Failed to parse document: {}", file.getAbsolutePath(), e);
        }
        
        return DocumentResult.builder()
                .filePath(file.getAbsolutePath())
                .fileName(file.getName())
                .fileSize(file.length())
                .success(false)
                .errorMessage(e.getMessage())
                .processingTimeMs(processingTime)
                .build();
    }
    
    private DocumentResult handleStreamParsingError(String fileName, Exception e, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        if (config.isEnableVerboseLogging()) {
            log.error("Failed to parse stream: {}", fileName, e);
        }
        
        return DocumentResult.builder()
                .filePath(fileName)
                .fileName(fileName)
                .success(false)
                .errorMessage(e.getMessage())
                .processingTimeMs(processingTime)
                .build();
    }
    
    // ==================== 资源清理 ====================
    
    /**
     * 关闭解析器并清理资源
     */
    public void close() {
        /*
         * if (executorService != null && !executorService.isShutdown()) { executorService.shutdown(); try { if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
         * executorService.shutdownNow(); } } catch (InterruptedException e) { executorService.shutdownNow(); Thread.currentThread().interrupt(); } }
         */
    }
    
    // ==================== 建造者模式 ====================
    
    /**
     * DocumentParser 建造者
     */
    public static class Builder {
        
        private UnstructuredConfig config;
        
        public Builder withConfig(UnstructuredConfig config) {
            this.config = config;
            return this;
        }
        
        public Builder withTableExtraction(boolean enabled) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .enableTableExtraction(enabled)
                    .build();
            return this;
        }
        
        public Builder withImageExtraction(boolean enabled) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .enableImageExtraction(enabled)
                    .build();
            return this;
        }
        
        public Builder withTableAsHtml(boolean asHtml) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .tableAsHtml(asHtml)
                    .build();
            return this;
        }
        
        public Builder withMaxDocumentSize(int maxSizeInMB) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .maxDocumentSizeInMB(maxSizeInMB)
                    .build();
            return this;
        }
        
        public Builder withOutputFormat(UnstructuredConfig.OutputFormat format) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .outputFormat(format)
                    .build();
            return this;
        }
        
        public Builder withErrorHandling(UnstructuredConfig.ErrorHandlingStrategy strategy) {
            ensureConfig();
            this.config = this.config.toBuilder()
                    .errorHandlingStrategy(strategy)
                    .build();
            return this;
        }
        
        public WordParser build() {
            return new WordParser(config != null ? config : UnstructuredConfig.defaultConfig());
        }
        
        private void ensureConfig() {
            if (config == null) {
                config = UnstructuredConfig.defaultConfig();
            }
        }
    }
}
