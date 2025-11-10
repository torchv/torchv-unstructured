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

import com.torchv.infra.unstructured.core.KeyValuePair;
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * K-V 格式提取功能测试
 *
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
public class KeyValueExtractionTest {
    
    @Test
    public void testExtractColonFormat() {
        String text = """
                产品名称: TorchV Unstructured
                版本: 1.0.0
                作者: xiaoymin
                """;
        
        List<KeyValuePair> pairs = KeyValueExtractor.extractFromText(text);
        System.out.println(pairs);
        
        // assertNotNull(pairs);
        // assertEquals(3, pairs.size());
        //
        // assertEquals("产品名称", pairs.get(0).getKey());
        // assertEquals("TorchV Unstructured", pairs.get(0).getValue());
        //
        // assertEquals("版本", pairs.get(1).getKey());
        // assertEquals("1.0.0", pairs.get(1).getValue());
    }
    
    @Test
    public void testExtractMarkdownFormat() {
        String text = """
                - **最大输入电压**: 24V
                - **工作温度**: -20℃ ~ 60℃
                - **功率**: 100W
                """;
        
        List<KeyValuePair> pairs = KeyValueExtractor.extractFromText(text);
        
        assertNotNull(pairs);
        assertEquals(3, pairs.size());
        
        assertEquals("最大输入电压", pairs.get(0).getKey());
        assertEquals("24V", pairs.get(0).getValue());
        assertTrue(pairs.get(0).getConfidence() >= 0.9);
    }
    
    @Test
    public void testExtractQAFormat() {
        String text = """
                Q: 忘记密码怎么办？ A: 点击登录页"找回密码"，通过邮箱重置。
                Q: 支持哪些支付方式？ A: 微信、支付宝、银联。
                """;
        
        List<KeyValuePair> pairs = KeyValueExtractor.extractFromText(text);
        
        assertNotNull(pairs);
        assertEquals(2, pairs.size());
        
        assertTrue(pairs.get(0).getKey().startsWith("Q:"));
        assertTrue(pairs.get(0).getValue().startsWith("A:"));
    }
    
    @Test
    public void testExtractEqualsFormat() {
        String text = """
                timeout = 30秒
                max_connections = 100
                enable_cache = true
                """;
        
        List<KeyValuePair> pairs = KeyValueExtractor.extractFromText(text);
        
        assertNotNull(pairs);
        assertEquals(3, pairs.size());
        
        assertEquals("timeout", pairs.get(0).getKey());
        assertEquals("30秒", pairs.get(0).getValue());
    }
    
    @Test
    public void testToMarkdown() {
        List<KeyValuePair> pairs = List.of(
                KeyValuePair.builder().key("姓名").value("张三").build(),
                KeyValuePair.builder().key("部门").value("技术部").build());
        
        String markdown = KeyValueExtractor.toMarkdown(pairs);
        
        assertNotNull(markdown);
        assertTrue(markdown.contains("- **姓名**: 张三"));
        assertTrue(markdown.contains("- **部门**: 技术部"));
    }
    
    @Test
    public void testToJson() {
        List<KeyValuePair> pairs = List.of(
                KeyValuePair.builder().key("name").value("TorchV").build(),
                KeyValuePair.builder().key("version").value("1.0.0").build());
        
        String json = KeyValueExtractor.toJsonArray(pairs);
        
        assertNotNull(json);
        assertTrue(json.contains("\"key\": \"name\""));
        assertTrue(json.contains("\"value\": \"TorchV\""));
    }
    
    @Test
    public void testFilterByConfidence() {
        List<KeyValuePair> pairs = List.of(
                KeyValuePair.builder().key("key1").value("value1").confidence(0.95).build(),
                KeyValuePair.builder().key("key2").value("value2").confidence(0.75).build(),
                KeyValuePair.builder().key("key3").value("value3").confidence(0.85).build());
        
        List<KeyValuePair> filtered = KeyValueExtractor.filterByConfidence(pairs, 0.8);
        
        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().allMatch(p -> p.getConfidence() >= 0.8));
    }
}
