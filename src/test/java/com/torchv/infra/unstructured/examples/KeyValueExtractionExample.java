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


package com.torchv.infra.unstructured.examples;

import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;

import java.util.List;

/**
 * K-V 格式提取示例
 * 
 * 演示如何使用 TorchV Unstructured 提取文档中的键值对信息
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
public class KeyValueExtractionExample {
    
    public static void main(String[] args) {
        System.out.println("=== TorchV Unstructured K-V 格式提取示例 ===\n");
        
        // 示例 1: 从文本中提取键值对
        example1_ExtractFromText();
        
        // 示例 2: 提取不同格式的键值对
        example2_DifferentFormats();
        
        // 示例 3: 转换为不同输出格式
        example3_OutputFormats();
        
        // 示例 4: 过滤和筛选
        example4_FilteringAndFiltering();
        
        // 示例 5: 从 Word 文档提取（如果有测试文件）
        // example5_ExtractFromWordDocument();
    }
    
    /**
     * 示例 1: 从文本中提取键值对
     */
    private static void example1_ExtractFromText() {
        System.out.println("【示例 1】从文本中提取键值对");
        System.out.println("----------------------------------------");
        
        String text = """
                产品名称: TorchV Unstructured
                版本: 1.0.0
                作者: xiaoymin
                许可证: Apache 2.0
                """;
        
        List<KeyValuePair> pairs = KeyValueExtractor.extractFromText(text);
        
        System.out.println("提取到 " + pairs.size() + " 个键值对：");
        for (KeyValuePair pair : pairs) {
            System.out.println("  " + pair.toMarkdown());
        }
        System.out.println();
    }
    
    /**
     * 示例 2: 提取不同格式的键值对
     */
    private static void example2_DifferentFormats() {
        System.out.println("【示例 2】提取不同格式的键值对");
        System.out.println("----------------------------------------");
        
        // Markdown 格式
        String markdownText = """
                - **最大输入电压**: 24V
                - **工作温度**: -20℃ ~ 60℃
                - **功率**: 100W
                """;
        
        System.out.println("Markdown 格式:");
        List<KeyValuePair> mdPairs = KeyValueExtractor.extractFromText(markdownText);
        mdPairs.forEach(p -> System.out.println("  " + p.toPlainText()));
        
        // 问答格式
        String qaText = """
                Q: 忘记密码怎么办？ A: 点击登录页"找回密码"，通过邮箱重置。
                Q: 支持哪些支付方式？ A: 微信、支付宝、银联。
                """;
        
        System.out.println("\n问答格式:");
        List<KeyValuePair> qaPairs = KeyValueExtractor.extractFromText(qaText);
        qaPairs.forEach(p -> System.out.println("  " + p.toPlainText()));
        
        // 等号格式
        String equalsText = """
                timeout = 30秒
                max_connections = 100
                enable_cache = true
                """;
        
        System.out.println("\n等号格式:");
        List<KeyValuePair> eqPairs = KeyValueExtractor.extractFromText(equalsText);
        eqPairs.forEach(p -> System.out.println("  " + p.toPlainText()));
        
        System.out.println();
    }
    
    /**
     * 示例 3: 转换为不同输出格式
     */
    private static void example3_OutputFormats() {
        System.out.println("【示例 3】转换为不同输出格式");
        System.out.println("----------------------------------------");
        
        List<KeyValuePair> pairs = List.of(
                KeyValuePair.builder().key("姓名").value("张三").confidence(0.95).build(),
                KeyValuePair.builder().key("部门").value("技术研发部").confidence(0.90).build(),
                KeyValuePair.builder().key("入职日期").value("2023-05-15").confidence(0.85).build());
        
        // Markdown 格式
        System.out.println("Markdown 格式:");
        System.out.println(KeyValueExtractor.toMarkdown(pairs));
        
        // 纯文本格式
        System.out.println("纯文本格式:");
        System.out.println(KeyValueExtractor.toPlainText(pairs));
        
        // JSON 格式
        System.out.println("JSON 格式:");
        System.out.println(KeyValueExtractor.toJsonArray(pairs));
        
        System.out.println();
    }
    
    /**
     * 示例 4: 过滤和筛选
     */
    private static void example4_FilteringAndFiltering() {
        System.out.println("【示例 4】过滤和筛选");
        System.out.println("----------------------------------------");
        
        List<KeyValuePair> pairs = List.of(
                KeyValuePair.builder()
                        .key("高置信度项")
                        .value("值1")
                        .confidence(0.95)
                        .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                        .build(),
                KeyValuePair.builder()
                        .key("中置信度项")
                        .value("值2")
                        .confidence(0.75)
                        .sourceType(KeyValuePair.SourceType.TABLE)
                        .build(),
                KeyValuePair.builder()
                        .key("低置信度项")
                        .value("值3")
                        .confidence(0.60)
                        .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                        .build());
        
        // 按置信度过滤
        System.out.println("置信度 >= 0.8 的键值对:");
        List<KeyValuePair> highConfidence = KeyValueExtractor.filterByConfidence(pairs, 0.8);
        highConfidence.forEach(p -> System.out.println("  " + p.toMarkdownWithConfidence()));
        
        // 按来源类型过滤
        System.out.println("\n来自表格的键值对:");
        List<KeyValuePair> fromTable = KeyValueExtractor.filterBySourceType(pairs, KeyValuePair.SourceType.TABLE);
        fromTable.forEach(p -> System.out.println("  " + p.toMarkdown()));
        
        System.out.println();
    }
}
