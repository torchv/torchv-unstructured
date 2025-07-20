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

import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.parser.word.model.data.MarkdownElementInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 提取属性，以键值对的形式返回
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 14:57
 * @since torchv_server v1.7.3
 */
@Slf4j
@AllArgsConstructor
public class DocAttributeConverter implements Function<Attributes, Map<String, String>> {
    
    final MarkdownElementInfo currentElement;
    
    @Override
    public Map<String, String> apply(Attributes attributes) {
        Map<String, String> attrMap = new HashMap<>();
        if (attributes == null || currentElement == null) {
            return attrMap;
        }
        for (int i = 0; i < attributes.getLength(); i++) {
            String attrName = null;
            try {
                attrName = currentElement.getQName(attributes.getURI(i), attributes.getLocalName(i));
            } catch (SAXException e) {
                // ignore
                log.warn(e.getMessage());
            }
            attrMap.put(Objects.toString(attrName, StrUtil.EMPTY).toLowerCase(), attributes.getValue(i));
        }
        return attrMap;
    }
}
