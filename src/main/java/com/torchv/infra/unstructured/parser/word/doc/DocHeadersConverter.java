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


package com.torchv.infra.unstructured.parser.word.doc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.parser.word.model.data.DocumentHeaderInfo;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 15:15
 * @since torchv_server v1.7.3
 */
@AllArgsConstructor
public class DocHeadersConverter implements Function<Map<String, String>, DocumentHeaderInfo> {
    
    /**
     * Html标签，例如：h1,h2,h3,h4,h5,h6,h7,h8,h9
     * 或者带有属性的p标签
     */
    final String tagName;// trace
    
    @Override
    public DocumentHeaderInfo apply(Map<String, String> attr) {
        // 获取Header信息
        DocumentHeaderInfo headerInfo = DocumentHeaderInfo.empty();
        if (CollUtil.isEmpty(attr)) {
            return headerInfo;
        }
        if (headerInfo.containsHeader(tagName)) {
            // 包含Header信息
            headerInfo.setHeader(true);
            headerInfo.setHeaderText(headerInfo.headerText(tagName));
            return headerInfo;
        }
        // 判断属性
        String className = attr.get("class");
        if (StrUtil.isNotBlank(className)) {
            // 获取Header等级
            if (ReUtil.isMatch(DocumentHeaderInfo.HEADER_CHINESE_PATTERN, className)) {
                headerInfo = getClassHeader(className, DocumentHeaderInfo.HEADER_CHINESE_PATTERN);
            } else if (ReUtil.isMatch(DocumentHeaderInfo.HEADER_ENGLISH_PATTERN, className)) {
                headerInfo = getClassHeader(className, DocumentHeaderInfo.HEADER_ENGLISH_PATTERN);
            }
        }
        return headerInfo;
    }
    
    private DocumentHeaderInfo getClassHeader(String className, Pattern pattern) {
        DocumentHeaderInfo headerInfo = DocumentHeaderInfo.empty();
        if (StrUtil.isNotBlank(className)) {
            if (ReUtil.isMatch(pattern, className)) {
                String headerLevel = Objects.toString(ReUtil.get(pattern, className, 1), "0");
                int level = NumberUtil.parseInt(headerLevel, 0);
                if (level > 0) {
                    headerInfo.setHeader(true);
                    headerInfo.setLevel(level);
                    headerInfo.setHeaderText(headerInfo.headerLevelText());
                    return headerInfo;
                }
            }
        }
        return headerInfo;
        
    }
}
