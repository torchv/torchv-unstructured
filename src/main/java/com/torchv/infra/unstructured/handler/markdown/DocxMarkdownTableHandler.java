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
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.sax.SafeContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * DOC格式专用的XMarkdown处理器，支持表格转换为HTML
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class DocxMarkdownTableHandler extends SafeContentHandler {
    
    public static final String XHTML = "http://www.w3.org/1999/xhtml";
    
    /**
     * The metadata object for the document. This contains information
     * such as the author of the document, any keywords and other details.
     */
    private final Metadata metadata;
    
    /**
     * Flag to indicate whether the document has been started.
     */
    private boolean documentStarted = false;
    
    /**
     * Flags to indicate whether the document head element has been started/ended.
     */
    private boolean headStarted = false;
    private boolean headEnded = false;
    private boolean useFrameset = false;
    
    // 当前处理的Word文档
    private final ContentHandler delegateHandler;
    
    public DocxMarkdownTableHandler(ContentHandler handler, Metadata metadata) {
        super(handler);
        this.metadata = metadata;
        this.delegateHandler = handler;
    }
    
    /**
     * 设置当前处理的DOC文档
     */
    public void setCurrentDocument(HWPFDocument document) {
        // 如果底层的handler是我们的自定义DOC表格处理器，也要设置文档
        if (delegateHandler instanceof DocMarkdownTableHandler) {
            ((DocMarkdownTableHandler) delegateHandler).setCurrentDocument(document);
        }
    }
    
    /**
     * Starts an XHTML document by setting up the namespace mappings
     * when called for the first time.
     * The standard XHTML prefix is generated lazily when the first
     * element is started.
     */
    @Override
    public void startDocument() throws SAXException {
        if (!documentStarted) {
            documentStarted = true;
            super.startDocument();
            startPrefixMapping("", XHTML);
        }
    }
    
    /**
     * Generates the following XHTML prefix when called for the first time:
     * 
     * <pre>
     * &lt;html&gt;
     *   &lt;head&gt;
     *     &lt;title&gt;...&lt;/title&gt;
     *   &lt;/head&gt;
     *   &lt;body&gt;
     * </pre>
     * 
     * @param uri   URI
     * @param local local name
     * @param name  prefixed name
     * @param atts  attributes
     */
    @Override
    public void startElement(String uri, String local, String name, Attributes atts) throws SAXException {
        if (!documentStarted) {
            startDocument();
        }
        
        if (name.equals("frameset")) {
            useFrameset = true;
        }
        
        lazyStartHead();
        lazyEndHead();
        
        if (name.equals("title") || name.equals("meta")) {
            // 跳过标题和meta标签，减少不必要的输出
            return;
        }
        
        super.startElement(XHTML, local, name, atts);
    }
    
    @Override
    public void endElement(String uri, String local, String name) throws SAXException {
        lazyEndHead();
        super.endElement(XHTML, local, name);
    }
    
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // Don't pass prefix mappings to the underlying handler
    }
    
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        // Don't pass prefix mappings to the underlying handler
    }
    
    /**
     * Starts the HTML head element if it hasn't been started yet,
     * and if we haven't seen the HTML body element yet.
     */
    private void lazyStartHead() throws SAXException {
        if (!headStarted && !headEnded) {
            headStarted = true;
            super.startElement(XHTML, "html", "html", new AttributesImpl());
            super.startElement(XHTML, "head", "head", new AttributesImpl());
            
            if (metadata.get(TikaCoreProperties.TITLE) != null) {
                super.startElement(XHTML, "title", "title", new AttributesImpl());
                char[] title = metadata.get(TikaCoreProperties.TITLE).toCharArray();
                super.characters(title, 0, title.length);
                super.endElement(XHTML, "title", "title");
            }
        }
    }
    
    /**
     * Ends the HTML head element if it hasn't been ended yet,
     * and starts the HTML body element.
     */
    private void lazyEndHead() throws SAXException {
        if (!headEnded) {
            headEnded = true;
            if (headStarted) {
                super.endElement(XHTML, "head", "head");
            }
            if (useFrameset) {
                super.startElement(XHTML, "frameset", "frameset", new AttributesImpl());
            } else {
                super.startElement(XHTML, "body", "body", new AttributesImpl());
            }
        }
    }
    
    @Override
    public void endDocument() throws SAXException {
        lazyEndHead();
        if (useFrameset) {
            super.endElement(XHTML, "frameset", "frameset");
        } else {
            super.endElement(XHTML, "body", "body");
        }
        super.endElement(XHTML, "html", "html");
        super.endDocument();
    }
}
