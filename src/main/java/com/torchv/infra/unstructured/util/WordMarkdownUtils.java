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


package com.torchv.infra.unstructured.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.handler.markdown.DocMarkdownTableHandler;
import com.torchv.infra.unstructured.handler.markdown.DocxMarkdownTableHandler;
import com.torchv.infra.unstructured.handler.markdown.ExtendedMarkdownContentHandler;
import com.torchv.infra.unstructured.handler.markdown.MarkdownContentHandler;
import com.torchv.infra.unstructured.parser.word.WordTableParser;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2025/7/20 10:44
 * @since torchv-unstructured
 */
@Slf4j
public class WordMarkdownUtils {
    
    /**
     * 将文件转换成markdown文件
     * @param targetFile 目标文件
     * @return markdown文件
     */
    public static File toMarkdown(File targetFile) {
        return toMarkdown(targetFile, new ArrayList<>());
    }
    
    /**
     * 将Docx文件转换成markdown文件
     * @param docXFile docx文件
     * @return markdown文件
     */
    public static File toMarkdown(File docXFile, List<DocumentImage> pictures) {
        if (!FileUtil.exist(docXFile)) {
            return docXFile;
        }
        File target = new File(docXFile.getParentFile(), docXFile.getName() + "-convert.md");
        // 转换是否成功
        boolean transfer = false;
        try (InputStream inputStream = Files.newInputStream(new File(docXFile.getAbsolutePath()).toPath())) {
            // 创建 Parser 对象
            // 创建 Metadata 对象
            Metadata metadata = new Metadata();
            // Parser parser = new OOXMLParser();
            AutoDetectParser parser = new AutoDetectParser();
            MarkdownContentHandler toHTMLContentHandler = new MarkdownContentHandler(new FileOutputStream(target), StandardCharsets.UTF_8.displayName());
            ExtendedMarkdownContentHandler textHandler =
                    new ExtendedMarkdownContentHandler(toHTMLContentHandler, metadata);
            // 创建 ContentHandler 对象
            // ContentHandler textHandler = new XHTMLContentHandler(new BodyContentHandler(),new Metadata());
            // 创建 ParseContext 对象
            ParseContext parseContext = new ParseContext();
            TesseractOCRConfig config = new TesseractOCRConfig();
            config.setLanguage("chi_sim");
            config.setSkipOcr(false);
            parseContext.set(TesseractOCRConfig.class, config);
            // 解析 DOCX 文件
            parser.parse(inputStream, textHandler, metadata, parseContext);
            transfer = true;
        } catch (Exception e) {
            log.error("to markdown error,:{}", e.getMessage(), e);
            transfer = false;
        }
        if (transfer) {
            return clearMarkdown(target, pictures, true);
        }
        return target;
        
    }
    
    /**
     * 将Docx文件转换成markdown文件，表格转换为HTML格式
     *
     * @param docXFile docx文件
     * @return markdown文件
     */
    public static File toMarkdownWithHtmlTable(File docXFile) {
        return toMarkdownWithHtmlTable(docXFile, new ArrayList<>());
    }
    
    /**
     * 将Word文件转换成markdown文件，表格转换为HTML格式
     * 同时支持DOC和DOCX格式
     *
     * @param wordFile word文件(.doc或.docx)
     * @param pictures 图片列表
     * @return markdown文件
     */
    public static File toMarkdownWithHtmlTable(File wordFile, List<DocumentImage> pictures) {
        if (!FileUtil.exist(wordFile)) {
            return wordFile;
        }
        File target = new File(wordFile.getParentFile(), wordFile.getName() + "-convert-html-table.md");
        // 转换是否成功
        boolean transfer = false;
        
        // 检测文件格式
        FileMagic fileMagic = FileMagicUtils.checkMagic(wordFile);
        if (fileMagic == FileMagic.UNKNOWN) {
            String suffix = FileUtil.getSuffix(wordFile.getName());
            if (StrUtil.equalsIgnoreCase(suffix, "docx")) {
                fileMagic = FileMagic.OOXML;
            } else if (StrUtil.equalsIgnoreCase(suffix, "doc")) {
                fileMagic = FileMagic.OLE2;
            }
        }
        
        log.info("检测到文件格式: {}, 文件: {}", fileMagic, wordFile.getAbsolutePath());
        
        // 根据文件格式选择不同的处理方式
        switch (fileMagic) {
            case OOXML -> {
                // DOCX 格式 - 使用自定义表格解析器
                log.info("DOCX格式使用自定义表格解析器");
                transfer = processDocxWithHtmlTable(wordFile, target);
            }
            case OLE2 -> {
                // DOC 格式 - 使用新的HTML表格支持
                transfer = processDocWithHtmlTable(wordFile, target);
            }
            default -> {
                log.warn("不支持的文件格式: {}, 使用默认处理方式", fileMagic);
                // 回退到原始转换
                target = toMarkdown(wordFile, pictures);
                transfer = true;
            }
        }
        
        if (transfer) {
            return clearMarkdown(target, pictures, true);
        }
        return target;
    }
    
    /**
     * 处理DOCX格式文件，使用自定义表格解析器
     */
    private static boolean processDocxWithHtmlTable(File docxFile, File target) {
        log.info("开始处理DOCX文件: {}, 目标文件: {}", docxFile.getAbsolutePath(), target.getAbsolutePath());
        
        try (FileInputStream inputStream = new FileInputStream(docxFile)) {
            // 首先使用 WordTableParser 解析表格
            XWPFDocument document = new XWPFDocument(inputStream);
            WordTableParser wordTableParser = new WordTableParser();
            List<String> customTablesHtmlList = wordTableParser.parseToHtmlList(document);
            log.info("WordTableParser 解析出 {} 个表格", customTablesHtmlList.size());
            
            // 关闭文档以释放资源
            document.close();
            
            // 使用 Tika 解析整个文档内容（使用标准方法）
            try (OutputStream outputStream = new FileOutputStream(target)) {
                MarkdownContentHandler handler = new MarkdownContentHandler(
                        outputStream, StandardCharsets.UTF_8.displayName());
                
                // 创建 Metadata 对象
                Metadata metadata = new Metadata();
                metadata.set(Metadata.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                
                // 使用 XMarkdownContentHandler 包装
                ExtendedMarkdownContentHandler textHandler = new ExtendedMarkdownContentHandler(
                        handler, metadata);
                
                // 创建自动检测解析器
                AutoDetectParser parser = new AutoDetectParser();
                
                // 创建 ParseContext 对象
                ParseContext parseContext = new ParseContext();
                TesseractOCRConfig config = new TesseractOCRConfig();
                config.setLanguage("chi_sim");
                config.setSkipOcr(false);
                parseContext.set(TesseractOCRConfig.class, config);
                
                // 重新创建输入流用于解析
                try (FileInputStream parseStream = new FileInputStream(docxFile)) {
                    // 解析 DOCX 文件
                    parser.parse(parseStream, textHandler, metadata, parseContext);
                }
            }
            
            // 读取 Tika 生成的内容
            String tikaContent = FileUtil.readUtf8String(target);
            log.info("Tika 解析内容长度: {}", tikaContent.length());
            
            // 替换 Tika 解析的表格为自定义 HTML 表格
            String finalContent = UnstructuredUtils.replaceTablesWithCustomHtmlList(tikaContent, customTablesHtmlList);
            
            // 写入最终内容到目标文件
            try (
                    FileOutputStream outputStream = new FileOutputStream(target);
                    PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
                writer.write(finalContent);
            }
            
            log.info("DOCX处理完成");
            return true;
            
        } catch (Exception e) {
            log.error("处理DOCX文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理DOC格式文件，使用增强的DOC表格解析器
     */
    private static boolean processDocWithHtmlTable(File docFile, File target) {
        try (FileInputStream inputStream = new FileInputStream(docFile)) {
            
            // 直接使用 HWPFDocument 解析
            HWPFDocument document = new HWPFDocument(inputStream);
            
            // 创建专用的DOC handler，使用 ByteArrayOutputStream 来收集内容
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DocMarkdownTableHandler handler = new DocMarkdownTableHandler(
                    baos, StandardCharsets.UTF_8.displayName());
            handler.setCurrentDocument(document);
            
            // 创建 Metadata 对象
            Metadata metadata = new Metadata();
            
            // 使用专用的DOC XMarkdown处理器包装
            DocxMarkdownTableHandler textHandler = new DocxMarkdownTableHandler(
                    handler, metadata);
            
            // 创建自动检测解析器
            AutoDetectParser parser = new AutoDetectParser();
            
            // 创建 ParseContext 对象
            ParseContext parseContext = new ParseContext();
            TesseractOCRConfig config = new TesseractOCRConfig();
            config.setLanguage("chi_sim");
            config.setSkipOcr(false);
            parseContext.set(TesseractOCRConfig.class, config);
            
            // 重新创建输入流用于解析
            try (FileInputStream parseStream = new FileInputStream(docFile)) {
                // 解析 DOC 文件
                parser.parse(parseStream, textHandler, metadata, parseContext);
            }
            
            // 获取解析后的内容
            String content = handler.getContent();
            
            // DOC格式已经使用新的表格解析器处理了表格，不需要额外的表格处理
            String finalContent = content;
            
            // 简单的清理，移除可能的多余空行
            finalContent = finalContent.replaceAll("(?m)^\\s*$\\n", "");
            
            log.info("DOC 表格处理完成，使用新的解析器");
            
            // 写入最终内容到目标文件
            try (
                    FileOutputStream outputStream = new FileOutputStream(target);
                    PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
                writer.write(finalContent);
            }
            
            return true;
        } catch (Exception e) {
            log.error("处理DOC文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 对于Docx文件转换成markdown格式后，对文件进行Markdown清理，删除不必要的行
     * @param source 源文件
     * @param delSourceFile 是否删除源文件
     * @return 清理后的文件
     */
    @SneakyThrows
    public static File clearMarkdown(File source, boolean delSourceFile) {
        return clearMarkdown(source, new ArrayList<>(), delSourceFile);
    }
    
    /**
     * 对于Docx文件转换成markdown格式后，对文件进行Markdown清理，删除不必要的行
     * @param source 源文件
     * @param pictures 图片信息
     * @param delSourceFile 是否删除源文件
     * @return 清理后的文件
     */
    @SneakyThrows
    public static File clearMarkdown(File source, List<DocumentImage> pictures, boolean delSourceFile) {
        log.info("clear markdown file:{}", source.getAbsolutePath());
        if (!FileUtil.exist(source)) {
            return source;
        }
        String imageRegex = "# image(\\d+)\\.(png|jpg|jpeg|gif|bmp|webp)";
        File target = new File(source.getParentFile(), source.getName() + "-clear.md");
        // 如果该文件存在，先删除
        FileUtil.del(target);
        log.info("target:{}", target.getAbsolutePath());
        try (BufferedReader reader = FileUtil.getReader(source, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 附件图片，在最后，不需要，直接舍弃
                if (StrUtil.isBlank(line) || ReUtil.isMatch(imageRegex, line)) {
                    continue;
                }
                // 标题开头的，在标题前追加一个空行
                if (StrUtil.startWith(line, "#")) {
                    FileUtil.appendUtf8String("\n", target);
                    FileUtil.appendUtf8String(line, target);
                    FileUtil.appendUtf8String("\n\n", target);
                    continue;
                }
                // 对于图片判断，如果存在图片，那么进行replace，将图片的地址替换为oss地址
                String targetLine = DocumentParserUtils.replaceURL(line, pictures);
                // 判断图片
                FileUtil.appendUtf8String(targetLine, target);
                FileUtil.appendUtf8String("\n", target);
            }
        }
        if (delSourceFile) {
            FileUtil.del(source);
        }
        return target;
    }
    
}
