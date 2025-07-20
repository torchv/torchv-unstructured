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

import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.DocumentResult;
import com.torchv.infra.unstructured.core.UnstructuredConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.List;

import static com.torchv.infra.unstructured.util.UnstructuredUtils.*;

/**
 * TorchV Unstructured Word文档解析器
 * 专门用于处理Word文档(.doc/.docx)的解析器。
 * 推荐使用 {@link UnstructuredParser} 作为统一入口。
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Slf4j
public class UnstructuredWord {
    
    // ==================== 核心解析方法 ====================
    
    /**
     * 解析文档为纯文本（推荐用于全文搜索、索引）
     * @param file 文档文件
     * @return 纯文本内容
     * @throws IllegalArgumentException 当文件不存在或路径无效时
     */
    public static String toMarkdown(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("文件不存在或路径无效: " + file);
        }
        return toMarkdown(file.getAbsolutePath());
    }
    
    /**
     * 解析文档为Markdown格式（推荐用于知识库、文档处理）
     * 
     * @param filePath 文档文件路径
     * @return Markdown格式的文档内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toMarkdown(String filePath) {
        try (WordParser parser = WordParser.ragOptimized()) {
            DocumentResult result = parser.parse(filePath);
            if (!result.isSuccess()) {
                throw new RuntimeException("文档解析失败: " + result.getErrorMessage());
            }
            return result.getContent();
        } catch (Exception e) {
            log.error("解析文档失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析文档为Markdown格式（保留表格的HTML结构）
     * 
     * @param filePath 文档文件路径
     * @return 带HTML表格的Markdown格式内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toMarkdownWithHtmlTables(String filePath) {
        try (WordParser parser = createTableOptimizedParser()) {
            DocumentResult result = parser.parse(filePath);
            if (!result.isSuccess()) {
                throw new RuntimeException("文档解析失败: " + result.getErrorMessage());
            }
            return result.getContent();
        } catch (Exception e) {
            log.error("解析文档失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析文档为完整的结构化结果对象（推荐复杂场景使用）
     * 
     * @param filePath 文档文件路径
     * @return 完整的解析结果对象，包含内容、表格、图片等信息
     * @throws RuntimeException 当文件解析失败时
     */
    public static DocumentResult toStructuredResult(String filePath) {
        try (WordParser parser = createDefaultParser()) {
            return parser.parse(filePath);
        } catch (Exception e) {
            log.error("解析文档失败: {}", filePath, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
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
        try (WordParser parser = createDefaultParser()) {
            return parser.extractWordTables(filePath);
        } catch (Exception e) {
            log.error("提取表格失败: {}", filePath, e);
            throw new RuntimeException("表格提取失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 输入流方法 ====================
    
    /**
     * 从输入流解析文档为纯文本
     * 
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于类型判断）
     * @return 纯文本内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toText(InputStream inputStream, String fileName) {
        try (WordParser parser = createDefaultParser()) {
            DocumentResult result = parser.parse(inputStream, fileName);
            if (!result.isSuccess()) {
                throw new RuntimeException("文档解析失败: " + result.getErrorMessage());
            }
            return result.getContent();
        } catch (Exception e) {
            log.error("解析文档流失败: {}", fileName, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 从输入流解析文档为Markdown格式
     * 
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于类型判断）
     * @return Markdown格式的文档内容
     * @throws RuntimeException 当文件解析失败时
     */
    public static String toMarkdown(InputStream inputStream, String fileName) {
        try (WordParser parser = createDefaultParser()) {
            DocumentResult result = parser.parse(inputStream, fileName);
            if (!result.isSuccess()) {
                throw new RuntimeException("文档解析失败: " + result.getErrorMessage());
            }
            return result.getContent();
        } catch (Exception e) {
            log.error("解析文档流失败: {}", fileName, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
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
        try (WordParser parser = createDefaultParser()) {
            return parser.parse(inputStream, fileName);
        } catch (Exception e) {
            log.error("解析文档流失败: {}", fileName, e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
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
        try (WordParser parser = WordParser.ragOptimized()) {
            return parser.parse(filePath);
        } catch (Exception e) {
            log.error("知识库解析失败: {}", filePath, e);
            throw new RuntimeException("知识库解析失败: " + e.getMessage(), e);
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
        try (WordParser parser = WordParser.ragOptimized()) {
            DocumentResult result = parser.parse(filePath);
            if (!result.isSuccess()) {
                throw new RuntimeException("AI训练数据解析失败: " + result.getErrorMessage());
            }
            return result.getContent();
        } catch (Exception e) {
            log.error("AI训练数据解析失败: {}", filePath, e);
            throw new RuntimeException("AI训练数据解析失败: " + e.getMessage(), e);
        }
    }
    // ==================== 私有辅助方法 ====================
    
    /**
     * 创建默认配置的解析器
     */
    private static WordParser createDefaultParser() {
        return new WordParser(UnstructuredConfig.defaultConfig());
    }
    
    /**
     * 创建表格优化配置的解析器
     */
    private static WordParser createTableOptimizedParser() {
        UnstructuredConfig config = UnstructuredConfig.defaultConfig()
                .toBuilder()
                .tableAsHtml(true)
                .enableTableExtraction(true)
                .build();
        return new WordParser(config);
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
                        
                        if (isWordDocument(filePath) && result.getTables() != null) {
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
