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


package com.torchv.infra.unstructured.parser.word.model.data;

import org.xml.sax.SAXException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 09:37
 * @since torchv_server v1.7.3
 */
public record MarkdownElementInfo(MarkdownElementInfo parent,
                                  Map<String, String> namespaces) {

    public MarkdownElementInfo(MarkdownElementInfo parent, Map<String, String> namespaces) {
        this.parent = parent;
        if (namespaces.isEmpty()) {
            this.namespaces = Collections.emptyMap();
        } else {
            this.namespaces = new HashMap<>(namespaces);
        }
    }

    public String getPrefix(String uri) throws SAXException {
        String prefix = namespaces.get(uri);
        if (prefix != null) {
            return prefix;
        } else if (parent != null) {
            return parent.getPrefix(uri);
        } else if (uri == null || uri.isEmpty()) {
            return "";
        } else {
            throw new SAXException("Namespace " + uri + " not declared");
        }
    }

    public String getQName(String uri, String localName) throws SAXException {
        String prefix = getPrefix(uri);
        if (!prefix.isEmpty()) {
            return prefix + ":" + localName;
        } else {
            return localName;
        }
    }
}
