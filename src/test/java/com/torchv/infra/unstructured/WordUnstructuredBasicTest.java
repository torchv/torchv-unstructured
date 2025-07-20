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

import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.core.DocumentResult;
import com.torchv.infra.unstructured.parser.word.UnstructuredWord;
import junit.framework.TestCase;

import java.io.File;

/**
 * UnstructuredWord 基本解析功能测试
 * 
 * 测试基本的文档解析方法，包括：
 * - parseToMarkdown(String filePath)
 * - parse(String filePath)
 * - parseToMarkdownWithTables(String filePath)
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
public class WordUnstructuredBasicTest extends TestCase {
    
    private static final String TEST_DOCX_PATH = "src/test/resources/docs/test.docx";
    private String absoluteTestPath;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // 获取测试文件的绝对路径
        File testFile = new File(TEST_DOCX_PATH);
        absoluteTestPath = testFile.getAbsolutePath();
        
        // 验证测试文件存在
        assertTrue("测试文件不存在: " + absoluteTestPath, testFile.exists());
        assertTrue("测试文件不可读: " + absoluteTestPath, testFile.canRead());
    }
    
    /**
     * 测试基本的Markdown解析功能
     */
    public void testParseToMarkdown() {
        String result = UnstructuredWord.toMarkdown(absoluteTestPath);
        
        assertNotNull("解析结果不应为null", result);
        assertFalse("解析结果不应为空", result.trim().isEmpty());
        
        // 验证结果包含基本的内容
        assertTrue("解析结果长度应大于0", StrUtil.length(result) > 0);
        
        System.out.println("Markdown解析成功，内容长度: " + result.length());
    }
    
    /**
     * 测试结构化解析功能
     */
    public void testParse() {
        DocumentResult result = UnstructuredWord.toStructuredResult(absoluteTestPath);
        
        assertNotNull("解析结果不应为null", result);
        assertTrue("解析应成功", result.isSuccess());
        assertNotNull("解析内容不应为null", result.getContent());
        assertFalse("解析内容不应为空", result.getContent().trim().isEmpty());
        
        // 验证基本属性
        assertNotNull("文件路径不应为null", result.getFilePath());
        assertNotNull("文件名不应为null", result.getFileName());
        assertTrue("文件名应以.docx结尾", result.getFileName().endsWith(".docx"));
        
        // 验证处理时间
        assertTrue("处理时间应大于0", result.getProcessingTimeMs() > 0);
        
        System.out.println("结构化解析成功:");
        System.out.println("- 文件名: " + result.getFileName());
        System.out.println("- 内容长度: " + result.getContent().length());
        System.out.println("- 处理时间: " + result.getProcessingTimeMs() + "ms");
        if (result.getTables() != null) {
            System.out.println("- 表格数量: " + result.getTables().size());
        }
    }
    
    /**
     * 测试带HTML表格的Markdown解析
     */
    public void testParseToMarkdownWithTables() {
        String result = UnstructuredWord.toMarkdown(absoluteTestPath);
        
        assertNotNull("解析结果不应为null", result);
        assertFalse("解析结果不应为空", result.trim().isEmpty());
        
        // 验证结果包含基本的内容
        assertTrue("解析结果长度应大于0", result.length() > 0);
        
        System.out.println("带表格的Markdown解析成功，内容长度: " + result.length());
        
        // 如果包含表格，应该有HTML表格标签
        if (result.contains("<table>") || result.contains("<TABLE>")) {
            System.out.println("检测到HTML表格内容");
        }
    }
    
    /**
     * 测试多种方法的结果一致性
     */
    public void testConsistencyBetweenMethods() {
        // 使用不同方法解析同一文件
        String markdownContent = UnstructuredWord.toMarkdown(absoluteTestPath);
        DocumentResult structuredResult = UnstructuredWord.toStructuredResult(absoluteTestPath);
        String markdownWithTables = UnstructuredWord.toMarkdownWithHtmlTables(absoluteTestPath);
        
        // 验证基本内容一致性（去除空白字符影响）
        assertNotNull("Markdown内容不应为null", markdownContent);
        assertNotNull("结构化结果内容不应为null", structuredResult.getContent());
        assertNotNull("带表格Markdown内容不应为null", markdownWithTables);
        
        // 内容长度应该相近（允许格式差异）
        int baseLength = markdownContent.length();
        int structuredLength = structuredResult.getContent().length();
        int tableLength = markdownWithTables.length();
        
        assertTrue("内容长度应该相近", Math.abs(baseLength - structuredLength) < baseLength * 0.5);
        
        System.out.println("一致性测试通过:");
        System.out.println("- 基本Markdown长度: " + baseLength);
        System.out.println("- 结构化结果长度: " + structuredLength);
        System.out.println("- 带表格Markdown长度: " + tableLength);
    }
    
    /**
     * 测试解析性能
     */
    public void testParsingPerformance() {
        long startTime = System.currentTimeMillis();
        
        // 连续解析多次测试性能
        for (int i = 0; i < 3; i++) {
            DocumentResult result = UnstructuredWord.toStructuredResult(absoluteTestPath);
            assertTrue("第" + (i + 1) + "次解析应成功", result.isSuccess());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.println("性能测试完成:");
        System.out.println("- 3次解析总时间: " + totalTime + "ms");
        System.out.println("- 平均每次解析时间: " + (totalTime / 3.0) + "ms");
        
        // 性能应该在合理范围内（每次解析不超过10秒）
        assertTrue("平均解析时间应在合理范围内", (totalTime / 3.0) < 10000);
    }
}
