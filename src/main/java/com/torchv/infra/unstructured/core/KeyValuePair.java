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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Key-Value 键值对数据结构
 * 
 * 用于表示文档中提取的结构化键值对信息，特别适合 RAG 应用场景。
 * 每个键值对代表一个独立的语义单元，便于检索和匹配。
 * 
 * <h3>使用场景：</h3>
 * <ul>
 * <li>产品参数提取：如"最大输入电压: 24V"</li>
 * <li>配置说明：如"超时时间（默认）: 30秒"</li>
 * <li>FAQ 问答对：如"Q: 忘记密码怎么办？ A: 点击找回密码"</li>
 * <li>属性列表：如"姓名: 张三"</li>
 * </ul>
 * 
 * @author <a href="yixiaoshu88@163.com">yixiaoshu88@163.com</a>
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyValuePair {
    
    /**
     * 键（Key）- 属性名称或问题
     */
    private String key;
    
    /**
     * 值（Value）- 属性值或答案
     */
    private String value;
    
    /**
     * 置信度（0.0-1.0）
     * 表示该键值对提取的可信程度
     */
    @Builder.Default
    private double confidence = 1.0;
    
    /**
     * 来源类型
     * 标识该键值对从文档的哪个部分提取
     */
    private SourceType sourceType;
    
    /**
     * 在文档中的位置（段落索引或表格索引）
     */
    private int position;
    
    /**
     * 附加元数据
     * 可以存储额外的上下文信息
     */
    private String metadata;
    
    /**
     * 来源类型枚举
     */
    public enum SourceType {
        /** 从段落文本中提取 */
        PARAGRAPH,
        /** 从表格中提取 */
        TABLE,
        /** 从列表中提取 */
        LIST,
        /** 从标题中提取 */
        HEADING,
        /** 其他来源 */
        OTHER
    }
    
    /**
     * 转换为 Markdown 格式的字符串
     * 
     * @return Markdown 格式的 K-V 字符串
     */
    public String toMarkdown() {
        return String.format("- **%s**: %s", key, value);
    }
    
    /**
     * 转换为纯文本格式的字符串
     * 
     * @return 纯文本格式的 K-V 字符串
     */
    public String toPlainText() {
        return String.format("%s: %s", key, value);
    }
    
    /**
     * 转换为 JSON 格式的字符串
     * 
     * @return JSON 格式的 K-V 字符串
     */
    public String toJson() {
        return String.format("{\"key\": \"%s\", \"value\": \"%s\"}",
                escapeJson(key), escapeJson(value));
    }
    
    /**
     * 转换为带置信度的 Markdown 格式
     * 
     * @return 带置信度的 Markdown 字符串
     */
    public String toMarkdownWithConfidence() {
        return String.format("- **%s**: %s `(置信度: %.2f)`", key, value, confidence);
    }
    
    /**
     * 检查键值对是否有效
     * 
     * @return 如果键和值都不为空则返回 true
     */
    public boolean isValid() {
        return key != null && !key.trim().isEmpty()
                && value != null && !value.trim().isEmpty();
    }
    
    /**
     * 转义 JSON 特殊字符
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    @Override
    public String toString() {
        return toMarkdown();
    }
}
