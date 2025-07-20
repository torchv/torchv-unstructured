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


package com.torchv.infra.unstructured;

import com.torchv.infra.unstructured.core.DocumentResult;
import com.torchv.infra.unstructured.parser.word.UnstructuredWord;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

import static com.torchv.infra.unstructured.util.UnstructuredUtils.*;

/**
 * TorchV Unstructured 通用文档解析器统一入口
 * 提供简单易用的静态API来解析各种文档格式，专为RAG应用和知识库构建优化。
 * 支持Word、PDF、PPT、Excel等多种格式的统一解析。
 * 
 * <h3>快速开始：</h3>
 * 
 * <pre>{@code
 *
 * // 解析为Markdown格式（推荐知识库场景）
 * String markdown = UnstructuredParser.toMarkdown("document.docx");
 * 
 * // 解析为完整结构化结果（复杂场景）
 * DocumentResult result = UnstructuredParser.toStructuredResult("presentation.pptx");
 * 
 * // 仅提取表格数据
 * List<String> tables = UnstructuredParser.extractTables("spreadsheet.xlsx");
 * 
 * // 针对知识库优化的解析
 * DocumentResult kbResult = UnstructuredParser.forKnowledgeBase("document.docx");
 * }</pre>
 * 
 * <h3>支持的格式：</h3>
 * <ul>
 * <li><strong>Word</strong> - .doc, .docx</li>
 * <li><strong>PDF</strong> - .pdf (计划支持)</li>
 * <li><strong>PowerPoint</strong> - .ppt, .pptx (计划支持)</li>
 * <li><strong>Excel</strong> - .xls, .xlsx (计划支持)</li>
 * </ul>
 * 
 * <h3>API设计理念：</h3>
 * <ul>
 * <li><strong>toXxx()</strong> - 输出格式明确，如toText(), toMarkdown()</li>
 * <li><strong>extractXxx()</strong> - 提取特定内容，如extractTables()</li>
 * <li><strong>forXxx()</strong> - 针对特定场景优化，如forKnowledgeBase()</li>
 * </ul>
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Slf4j
public class UnstructuredParser {
    
    // ==================== 核心解析方法 ====================
    
    /**
     * 解析文档为Markdown格式（推荐用于知识库、文档处理）
     * 自动检测文件格式并选择合适的解析器
     * 
     * @param filePath 文档文件路径
     * @return Markdown格式的文档内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toMarkdown(String filePath) {
        return delegateToSpecificParser(filePath, ParserMethod.TO_MARKDOWN);
    }
    
    /**
     * 解析文档为Markdown格式（推荐用于知识库、文档处理）
     * 
     * @param file 文档文件
     * @return Markdown格式的文档内容
     * @throws IllegalArgumentException 当文件不存在或路径无效时
     */
    public static String toMarkdown(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在或路径无效: " + file);
        }
        return toMarkdown(file.getAbsolutePath());
    }
    
    /**
     * 解析文档为Markdown格式（保留表格的HTML结构）
     * 
     * @param filePath 文档文件路径
     * @return 带HTML表格的Markdown格式内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toMarkdownWithHtmlTables(String filePath) {
        return delegateToSpecificParser(filePath, ParserMethod.TO_MARKDOWN_WITH_TABLES);
    }
    
    /**
     * 解析文档为完整的结构化结果对象（推荐复杂场景使用）
     * 
     * @param filePath 文档文件路径
     * @return 完整的解析结果对象，包含内容、表格、图片等信息
     * @throws RuntimeException 当文件解析失败时
     */
    public static DocumentResult toStructuredResult(String filePath) {
        // 检测文件格式
        if (isWordDocument(filePath)) {
            return UnstructuredWord.toStructuredResult(filePath);
        }
        // TODO: 添加其他格式的支持
        // else if (UnstructuredUtils.isPdfDocument(filePath)) {
        // return PdfUnstructured.toStructuredResult(filePath);
        // }
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + filePath);
        }
    }
    
    /**
     * 仅提取文档中的表格数据（HTML格式）
     * 
     * @param filePath 文档文件路径
     * @return HTML格式的表格列表
     * @throws RuntimeException 当文件解析失败时
     */
    public static List<String> extractTables(String filePath) {
        // 检测文件格式
        if (isWordDocument(filePath)) {
            return UnstructuredWord.extractTables(filePath);
        }
        // TODO: 添加其他格式的支持
        // else if (isPdfDocument(filePath)) {
        // return PdfUnstructured.extractTables(filePath);
        // }
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + filePath);
        }
    }

    
    /**
     * 从输入流解析文档为结构化结果对象
     * 
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于类型判断）
     * @return 完整的解析结果对象
     * @throws RuntimeException 当文件解析失败时
     */
    public static DocumentResult toStructuredResult(InputStream inputStream, String fileName) {
        // 根据文件名检测格式
        if (isWordDocument(fileName)) {
            return UnstructuredWord.toStructuredResult(inputStream, fileName);
        }
        // TODO: 添加其他格式的支持
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + fileName);
        }
    }
    
    // ==================== 特定场景优化方法 ====================
    
    /**
     * 针对知识库应用优化的解析方法
     * 自动选择最适合RAG/知识库场景的配置
     * 
     * @param filePath 文档文件路径
     * @return 解析结果
     */
    public static DocumentResult forKnowledgeBase(String filePath) {
        // 检测文件格式
        if (isWordDocument(filePath)) {
            return UnstructuredWord.forKnowledgeBase(filePath);
        }
        // TODO: 添加其他格式的支持
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + filePath);
        }
    }
    
    /**
     * 针对AI训练数据处理的解析方法
     * 专门优化用于AI模型训练的数据预处理
     * 
     * @param filePath 文档文件路径
     * @return Markdown格式的训练数据
     */
    public static String forAiTraining(String filePath) {
        // 检测文件格式
        if (isWordDocument(filePath)) {
            return UnstructuredWord.forAiTraining(filePath);
        }
        // TODO: 添加其他格式的支持
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + filePath);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    /**
     * 委托给特定格式的解析器
     */
    private static String delegateToSpecificParser(String filePath, ParserMethod method) {
        // 检测文件格式
        if (isWordDocument(filePath)) {
            return switch (method) {
                case TO_TEXT -> {
                    DocumentResult result = UnstructuredWord.toStructuredResult(filePath);
                    yield result.getContent();
                }
                case TO_MARKDOWN -> UnstructuredWord.toMarkdown(filePath);
                case TO_MARKDOWN_WITH_TABLES -> UnstructuredWord.toMarkdownWithHtmlTables(filePath);
            };
        }
        // TODO: 添加其他格式的支持
        // else if (isPdfDocument(filePath)) {
        // return switch (method) {
        // case TO_TEXT -> PdfUnstructured.toText(filePath);
        // case TO_MARKDOWN -> PdfUnstructured.toMarkdown(filePath);
        // case TO_MARKDOWN_WITH_TABLES ->
        // PdfUnstructured.toMarkdownWithHtmlTables(filePath);
        // };
        // }
        else {
            throw new UnsupportedOperationException("暂不支持的文件格式: " + filePath);
        }
    }
    
    /**
     * 解析器方法枚举
     */
    private enum ParserMethod {
        TO_TEXT,
        TO_MARKDOWN,
        TO_MARKDOWN_WITH_TABLES
    }
    
    // ==================== 主方法 ====================
    
    /**
     * 主方法，用于测试和演示
     */
    public static void main(String[] args) {
        printLibraryInfo();
        
        if (args.length > 0) {
            String filePath = args[0];
            System.out.println("解析文件: " + filePath);
            
            try {
                if (isSupportedFormat(filePath)) {
                    DocumentResult result = toStructuredResult(filePath);
                    if (result.isSuccess()) {
                        System.out.println("解析成功！内容长度: " + result.getContent().length() + " 字符");
                        
                        if (result.getTables() != null) {
                            System.out.println("提取到 " + result.getTables().size() + " 个表格");
                        }
                        
                        System.out.println("处理时间: " + result.getProcessingTimeMs() + " ms");
                    } else {
                        System.err.println("解析失败: " + result.getErrorMessage());
                    }
                } else {
                    System.out.println("不支持的文件格式");
                }
            } catch (Exception e) {
                System.err.println("解析失败: " + e.getMessage());
            }
        } else {
            printUsageExample();
        }
    }
}
