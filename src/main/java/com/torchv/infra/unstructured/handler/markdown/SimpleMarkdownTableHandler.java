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


package com.torchv.infra.unstructured.handler.markdown;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 简洁的Markdown处理器，专门用于表格转HTML的场景
 * 不生成完整的HTML文档结构，只处理内容部分
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class SimpleMarkdownTableHandler extends DefaultHandler {
    
    private final MarkdownWithTableHandler contentHandler;
    private final Metadata metadata;
    private XWPFDocument currentDocument;
    
    public SimpleMarkdownTableHandler(MarkdownWithTableHandler contentHandler,
                                      Metadata metadata) {
        this.contentHandler = contentHandler;
        this.metadata = metadata;
    }
    
    /**
     * 设置当前处理的Word文档
     */
    public void setCurrentDocument(XWPFDocument document) {
        this.currentDocument = document;
        if (this.contentHandler != null) {
            this.contentHandler.setCurrentDocument(document);
        }
    }
    
    @Override
    public void startDocument() throws SAXException {
        contentHandler.startDocument();
    }
    
    @Override
    public void endDocument() throws SAXException {
        contentHandler.endDocument();
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        contentHandler.startElement(uri, localName, qName, attributes);
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        contentHandler.endElement(uri, localName, qName);
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        contentHandler.characters(ch, start, length);
    }
}
