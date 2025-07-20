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


package com.torchv.infra.unstructured.parser.word.docx;

import com.torchv.infra.unstructured.parser.word.doc.DocAttributeConverter;
import com.torchv.infra.unstructured.parser.word.model.data.MarkdownElementInfo;
import lombok.Getter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Objects;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 09:38
 * @since torchv_server v1.7.3
 */
@Getter
public class DocxMarkdownImageExtractor {
    
    final Attributes attributes;
    final MarkdownElementInfo currentElement;
    
    final String url;
    final String attr;
    
    public DocxMarkdownImageExtractor(Attributes attributes, MarkdownElementInfo currentElement) throws SAXException {
        this.currentElement = currentElement;
        this.attributes = attributes;
        Map<String, String> attrMap = new DocAttributeConverter(currentElement).apply(attributes);
        this.url = attrMap.get("src");
        // 替换换行富豪
        this.attr = Objects.toString(attrMap.get("alt"), "").replaceAll("\n", "");
        
    }
}
