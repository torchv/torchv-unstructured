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


package com.torchv.infra.unstructured.core;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.HashSet;

/**
 * TorchV Unstructured 解析配置类
 * 
 * 提供灵活的配置选项来控制文档解析行为，支持不同的使用场景和性能需求。
 * 
 * <h3>使用示例：</h3>
 * 
 * <pre>{@code
 * // 使用默认配置
 * UnstructuredConfig config = UnstructuredConfig.defaultConfig();
 * 
 * // 自定义配置
 * UnstructuredConfig config = UnstructuredConfig.builder()
 *         .enableTableExtraction(true)
 *         .tableAsHtml(true)
 *         .enableImageExtraction(false)
 *         .maxDocumentSizeInMB(50)
 *         .build();
 * }</pre>
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UnstructuredConfig {
    
    // ==================== 表格解析配置 ====================
    
    /**
     * 是否启用表格提取功能
     * 默认: true
     */
    @Builder.Default
    private boolean enableTableExtraction = true;
    
    /**
     * 表格是否以HTML格式输出
     * 当为true时，表格将保持HTML格式以保留结构
     * 当为false时，表格将转换为Markdown格式
     * 默认: true（推荐用于RAG应用）
     */
    @Builder.Default
    private boolean tableAsHtml = false;
    
    /**
     * 是否保持表格结构完整性
     * 包括合并单元格、样式等复杂结构
     * 默认: true
     */
    @Builder.Default
    private boolean preserveTableStructure = true;
    
    /**
     * 表格解析的最大复杂度阈值
     * 超过此阈值的复杂表格将使用简化解析
     * 默认: 100（行数×列数）
     */
    @Builder.Default
    private int maxTableComplexity = 100;
    
    // ==================== 图像解析配置 ====================
    
    /**
     * 是否启用图像提取功能
     * 默认: false（减少处理时间）
     */
    @Builder.Default
    private boolean enableImageExtraction = false;
    
    /**
     * 单个图像的最大尺寸限制（MB）
     * 超过此大小的图像将被跳过
     * 默认: 10MB
     */
    @Builder.Default
    private int maxImageSizeInMB = 10;
    
    /**
     * 支持的图像格式
     * 默认支持常见的图像格式
     */
    @Builder.Default
    private Set<String> supportedImageFormats = new HashSet<String>() {
        
        {
            add("jpg");
            add("jpeg");
            add("png");
            add("gif");
            add("bmp");
            add("svg");
        }
    };
    
    /**
     * 图像提取的基础路径
     * 如果为null，将使用临时目录
     */
    private String imageOutputBasePath;
    
    // ==================== 输出格式配置 ====================
    
    /**
     * 输出格式类型
     */
    public enum OutputFormat {
        /** Markdown格式 */
        MARKDOWN,
        /** 纯文本格式 */
        PLAIN_TEXT,
        /** HTML格式 */
        HTML,
        /** 结构化JSON格式 */
        JSON
    }
    
    /**
     * 主要输出格式
     * 默认: MARKDOWN（最适合RAG应用）
     */
    @Builder.Default
    private OutputFormat outputFormat = OutputFormat.MARKDOWN;
    
    /**
     * 是否在输出中包含文档元数据
     * 如作者、创建时间、修改时间等
     * 默认: true
     */
    @Builder.Default
    private boolean includeMetadata = true;
    
    /**
     * 是否在输出中包含页眉页脚
     * 默认: false
     */
    @Builder.Default
    private boolean includeHeaderFooter = false;
    
    /**
     * 内容分段策略
     */
    public enum ContentSegmentation {
        /** 不分段，返回完整内容 */
        NONE,
        /** 按段落分段 */
        PARAGRAPH,
        /** 按页面分段 */
        PAGE,
        /** 按章节分段 */
        SECTION
    }
    
    /**
     * 内容分段策略
     * 默认: NONE
     */
    @Builder.Default
    private ContentSegmentation contentSegmentation = ContentSegmentation.NONE;
    
    // ==================== 性能配置 ====================
    
    /**
     * 文档大小限制（MB）
     * 超过此大小的文档将被拒绝处理
     * 默认: 100MB
     */
    @Builder.Default
    private int maxDocumentSizeInMB = 100;
    
    /**
     * 是否启用流式处理
     * 对于大文档，流式处理可以减少内存占用
     * 默认: true
     */
    @Builder.Default
    private boolean enableStreamProcessing = true;
    
    /**
     * 处理超时时间（秒）
     * 0表示无超时限制
     * 默认: 300秒（5分钟）
     */
    @Builder.Default
    private int processingTimeoutSeconds = 300;
    
    /**
     * 并发处理的最大线程数
     * 用于批量处理场景
     * 默认: 4
     */
    @Builder.Default
    private int maxConcurrentThreads = 4;
    
    // ==================== 错误处理配置 ====================
    
    /**
     * 遇到错误时的处理策略
     */
    public enum ErrorHandlingStrategy {
        /** 抛出异常，停止处理 */
        FAIL_FAST,
        /** 跳过错误，继续处理 */
        SKIP_ERRORS,
        /** 记录错误，返回部分结果 */
        LOG_AND_CONTINUE
    }
    
    /**
     * 错误处理策略
     * 默认: FAIL_FAST
     */
    @Builder.Default
    private ErrorHandlingStrategy errorHandlingStrategy = ErrorHandlingStrategy.FAIL_FAST;
    
    /**
     * 是否启用详细错误日志
     * 默认: true
     */
    @Builder.Default
    private boolean enableVerboseLogging = true;
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建默认配置
     * 适用于大多数RAG应用场景
     * 
     * @return 默认配置实例
     */
    public static UnstructuredConfig defaultConfig() {
        return UnstructuredConfig.builder().build();
    }
    
    /**
     * 创建RAG优化配置
     * 专门为RAG应用优化的配置
     * 
     * @return RAG优化配置实例
     */
    public static UnstructuredConfig ragOptimized() {
        return UnstructuredConfig.builder()
                .enableTableExtraction(true)
                .tableAsHtml(true)
                .preserveTableStructure(true)
                .enableImageExtraction(false) // RAG通常不需要图像
                .outputFormat(OutputFormat.MARKDOWN)
                .includeMetadata(true)
                .includeHeaderFooter(false)
                .contentSegmentation(ContentSegmentation.PARAGRAPH)
                .enableStreamProcessing(true)
                .errorHandlingStrategy(ErrorHandlingStrategy.LOG_AND_CONTINUE)
                .build();
    }
    
    /**
     * 创建RAG优化配置
     * 专门为RAG应用优化的配置
     *
     * @return RAG优化配置实例
     */
    public static UnstructuredConfig ragMarkdownOptimized() {
        return UnstructuredConfig.builder()
                .enableTableExtraction(true)
                .tableAsHtml(false) // 使用Markdown表格格式
                .preserveTableStructure(true)
                .enableImageExtraction(false) // RAG通常不需要图像
                .outputFormat(OutputFormat.MARKDOWN)
                .includeMetadata(true)
                .includeHeaderFooter(false)
                .contentSegmentation(ContentSegmentation.PARAGRAPH)
                .enableStreamProcessing(true)
                .errorHandlingStrategy(ErrorHandlingStrategy.LOG_AND_CONTINUE)
                .build();
    }
    
    /**
     * 创建高性能配置
     * 适用于需要快速处理大量文档的场景
     * 
     * @return 高性能配置实例
     */
    public static UnstructuredConfig highPerformance() {
        return UnstructuredConfig.builder()
                .enableTableExtraction(true)
                .tableAsHtml(false) // Markdown表格处理更快
                .preserveTableStructure(false)
                .enableImageExtraction(false)
                .outputFormat(OutputFormat.PLAIN_TEXT)
                .includeMetadata(false)
                .includeHeaderFooter(false)
                .contentSegmentation(ContentSegmentation.NONE)
                .maxDocumentSizeInMB(50)
                .enableStreamProcessing(true)
                .processingTimeoutSeconds(60)
                .maxConcurrentThreads(8)
                .errorHandlingStrategy(ErrorHandlingStrategy.SKIP_ERRORS)
                .enableVerboseLogging(false)
                .build();
    }
    
    /**
     * 创建完整功能配置
     * 启用所有功能，适用于需要完整文档信息的场景
     * 
     * @return 完整功能配置实例
     */
    public static UnstructuredConfig fullFeature() {
        return UnstructuredConfig.builder()
                .enableTableExtraction(true)
                .tableAsHtml(true)
                .preserveTableStructure(true)
                .enableImageExtraction(true)
                .maxImageSizeInMB(20)
                .outputFormat(OutputFormat.MARKDOWN)
                .includeMetadata(true)
                .includeHeaderFooter(true)
                .contentSegmentation(ContentSegmentation.SECTION)
                .maxDocumentSizeInMB(200)
                .processingTimeoutSeconds(600)
                .errorHandlingStrategy(ErrorHandlingStrategy.LOG_AND_CONTINUE)
                .enableVerboseLogging(true)
                .build();
    }
    
    // ==================== 验证方法 ====================
    
    /**
     * 验证配置的合理性
     * 
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (maxDocumentSizeInMB <= 0) {
            throw new IllegalArgumentException("maxDocumentSizeInMB must be positive");
        }
        
        if (maxImageSizeInMB <= 0) {
            throw new IllegalArgumentException("maxImageSizeInMB must be positive");
        }
        
        if (maxTableComplexity <= 0) {
            throw new IllegalArgumentException("maxTableComplexity must be positive");
        }
        
        if (processingTimeoutSeconds < 0) {
            throw new IllegalArgumentException("processingTimeoutSeconds cannot be negative");
        }
        
        if (maxConcurrentThreads <= 0) {
            throw new IllegalArgumentException("maxConcurrentThreads must be positive");
        }
        
        if (supportedImageFormats != null && supportedImageFormats.isEmpty()) {
            throw new IllegalArgumentException("supportedImageFormats cannot be empty if image extraction is enabled");
        }
    }
    
    /**
     * 检查是否为RAG优化配置
     * 
     * @return 如果配置适合RAG应用返回true
     */
    public boolean isRagOptimized() {
        return enableTableExtraction &&
                tableAsHtml &&
                outputFormat == OutputFormat.MARKDOWN &&
                includeMetadata;
    }
    
    /**
     * 检查是否为高性能配置
     * 
     * @return 如果配置为高性能返回true
     */
    public boolean isHighPerformance() {
        return !enableImageExtraction &&
                !includeHeaderFooter &&
                contentSegmentation == ContentSegmentation.NONE &&
                errorHandlingStrategy == ErrorHandlingStrategy.SKIP_ERRORS;
    }
    
    @Override
    public String toString() {
        return String.format("UnstructuredConfig{" +
                "table=%s, image=%s, output=%s, performance=%s}",
                enableTableExtraction,
                enableImageExtraction,
                outputFormat,
                enableStreamProcessing);
    }
}
