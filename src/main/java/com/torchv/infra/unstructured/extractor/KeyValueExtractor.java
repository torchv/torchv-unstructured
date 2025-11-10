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


package com.torchv.infra.unstructured.extractor;

import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.core.KeyValuePair;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Key-Value 键值对提取器
 * 
 * 从文本内容中智能提取键值对信息，支持多种常见格式。
 * 专为 RAG 应用优化，将文档信息原子化为独立的语义单元。
 * 
 * <h3>支持的格式：</h3>
 * <ul>
 * <li>冒号分隔：键: 值</li>
 * <li>等号分隔：键 = 值</li>
 * <li>Markdown 格式：- **键**: 值</li>
 * <li>问答格式：Q: 问题 A: 答案</li>
 * <li>表格行：| 键 | 值 |</li>
 * </ul>
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Slf4j
public class KeyValueExtractor {
    
    // 常见的 K-V 分隔符模式
    private static final Pattern COLON_PATTERN = Pattern.compile("^([^:：]+)[：:]\\s*(.+)$");
    private static final Pattern EQUALS_PATTERN = Pattern.compile("^([^=]+)=\\s*(.+)$");
    private static final Pattern MARKDOWN_PATTERN = Pattern.compile("^[-*]\\s*\\*\\*([^*]+)\\*\\*[：:]?\\s*(.+)$");
    private static final Pattern QA_PATTERN = Pattern.compile("^[QqＱｑ问][：:]\\s*(.+?)\\s+[AaＡａ答][：:]\\s*(.+)$");
    
    /**
     * 从文本内容中提取所有键值对
     * 
     * @param content 文本内容
     * @return 键值对列表
     */
    public static List<KeyValuePair> extractFromText(String content) {
        List<KeyValuePair> pairs = new ArrayList<>();
        
        if (StrUtil.isBlank(content)) {
            return pairs;
        }
        
        String[] lines = content.split("\n");
        int position = 0;
        
        for (String line : lines) {
            line = line.trim();
            if (StrUtil.isBlank(line)) {
                continue;
            }
            
            KeyValuePair pair = extractFromLine(line, position);
            if (pair != null && pair.isValid()) {
                pairs.add(pair);
            }
            position++;
        }
        
        log.debug("从文本中提取到 {} 个键值对", pairs.size());
        return pairs;
    }
    
    /**
     * 从单行文本中提取键值对
     * 
     * @param line     文本行
     * @param position 位置索引
     * @return 键值对，如果无法提取则返回 null
     */
    public static KeyValuePair extractFromLine(String line, int position) {
        if (StrUtil.isBlank(line)) {
            return null;
        }
        
        line = line.trim();
        
        // 尝试匹配 Markdown 格式
        Matcher markdownMatcher = MARKDOWN_PATTERN.matcher(line);
        if (markdownMatcher.matches()) {
            return KeyValuePair.builder()
                    .key(markdownMatcher.group(1).trim())
                    .value(markdownMatcher.group(2).trim())
                    .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                    .position(position)
                    .confidence(0.95)
                    .build();
        }
        
        // 尝试匹配问答格式
        Matcher qaMatcher = QA_PATTERN.matcher(line);
        if (qaMatcher.matches()) {
            return KeyValuePair.builder()
                    .key("Q: " + qaMatcher.group(1).trim())
                    .value("A: " + qaMatcher.group(2).trim())
                    .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                    .position(position)
                    .confidence(0.9)
                    .build();
        }
        
        // 尝试匹配冒号分隔
        Matcher colonMatcher = COLON_PATTERN.matcher(line);
        if (colonMatcher.matches()) {
            String key = colonMatcher.group(1).trim();
            String value = colonMatcher.group(2).trim();
            
            // 过滤掉可能是标题或其他非 K-V 的内容
            if (isLikelyKeyValue(key, value)) {
                return KeyValuePair.builder()
                        .key(key)
                        .value(value)
                        .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                        .position(position)
                        .confidence(0.8)
                        .build();
            }
        }
        
        // 尝试匹配等号分隔
        Matcher equalsMatcher = EQUALS_PATTERN.matcher(line);
        if (equalsMatcher.matches()) {
            return KeyValuePair.builder()
                    .key(equalsMatcher.group(1).trim())
                    .value(equalsMatcher.group(2).trim())
                    .sourceType(KeyValuePair.SourceType.PARAGRAPH)
                    .position(position)
                    .confidence(0.85)
                    .build();
        }
        
        return null;
    }
    
    /**
     * 从表格内容中提取键值对
     * 假设表格第一列是键，第二列是值
     *
     * @param tableHtml HTML 格式的表格
     * @return 键值对列表
     */
    public static List<KeyValuePair> extractFromTable(String tableHtml) {
        List<KeyValuePair> pairs = new ArrayList<>();
        
        if (StrUtil.isBlank(tableHtml)) {
            return pairs;
        }
        
        // 简单的 HTML 表格解析（提取 <td> 内容）
        Pattern tdPattern = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = tdPattern.matcher(tableHtml);
        
        List<String> cells = new ArrayList<>();
        while (matcher.find()) {
            String cellContent = matcher.group(1).trim();
            // 移除 HTML 标签
            cellContent = cellContent.replaceAll("<[^>]+>", "").trim();
            if (StrUtil.isNotBlank(cellContent)) {
                cells.add(cellContent);
            }
        }
        
        // 假设每两个单元格组成一个键值对
        int position = 0;
        for (int i = 0; i < cells.size() - 1; i += 2) {
            String key = cells.get(i);
            String value = cells.get(i + 1);
            
            if (StrUtil.isNotBlank(key) && StrUtil.isNotBlank(value)) {
                pairs.add(KeyValuePair.builder()
                        .key(key)
                        .value(value)
                        .sourceType(KeyValuePair.SourceType.TABLE)
                        .position(position++)
                        .confidence(0.9)
                        .build());
            }
        }
        
        log.debug("从表格中提取到 {} 个键值对", pairs.size());
        return pairs;
    }
    
    /**
     * 判断是否像键值对
     * 过滤掉标题、长句子等非 K-V 内容
     *
     * @param key   键
     * @param value 值
     * @return 如果像键值对返回 true
     */
    private static boolean isLikelyKeyValue(String key, String value) {
        // 键不应该太长（通常不超过 50 个字符）
        if (key.length() > 50) {
            return false;
        }
        
        // 值不应该为空
        if (StrUtil.isBlank(value)) {
            return false;
        }
        
        // 键不应该包含太多标点符号（可能是句子）
        long punctuationCount = key.chars().filter(c -> "，。！？,.!?;；".indexOf(c) >= 0).count();
        if (punctuationCount > 2) {
            return false;
        }
        
        // 键应该相对简短且有意义
        return key.length() >= 2;
    }
    
    /**
     * 将键值对列表转换为 Markdown 格式
     *
     * @param pairs 键值对列表
     * @return Markdown 格式的字符串
     */
    public static String toMarkdown(List<KeyValuePair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (KeyValuePair pair : pairs) {
            sb.append(pair.toMarkdown()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 将键值对列表转换为纯文本格式
     *
     * @param pairs 键值对列表
     * @return 纯文本格式的字符串
     */
    public static String toPlainText(List<KeyValuePair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (KeyValuePair pair : pairs) {
            sb.append(pair.toPlainText()).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * 将键值对列表转换为 JSON 数组格式
     *
     * @param pairs 键值对列表
     * @return JSON 数组格式的字符串
     */
    public static String toJsonArray(List<KeyValuePair> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < pairs.size(); i++) {
            sb.append("  ").append(pairs.get(i).toJson());
            if (i < pairs.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 过滤低置信度的键值对
     *
     * @param pairs             键值对列表
     * @param minConfidence     最小置信度阈值（0.0-1.0）
     * @return 过滤后的键值对列表
     */
    public static List<KeyValuePair> filterByConfidence(List<KeyValuePair> pairs, double minConfidence) {
        if (pairs == null || pairs.isEmpty()) {
            return new ArrayList<>();
        }
        
        return pairs.stream()
                .filter(pair -> pair.getConfidence() >= minConfidence)
                .toList();
    }
    
    /**
     * 按来源类型过滤键值对
     *
     * @param pairs      键值对列表
     * @param sourceType 来源类型
     * @return 过滤后的键值对列表
     */
    public static List<KeyValuePair> filterBySourceType(List<KeyValuePair> pairs,
                                                        KeyValuePair.SourceType sourceType) {
        if (pairs == null || pairs.isEmpty()) {
            return new ArrayList<>();
        }
        
        return pairs.stream()
                .filter(pair -> pair.getSourceType() == sourceType)
                .toList();
    }
}
