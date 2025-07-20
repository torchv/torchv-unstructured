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

import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.parser.word.doc.DocAttributeConverter;
import com.torchv.infra.unstructured.parser.word.doc.DocHeadersConverter;
import com.torchv.infra.unstructured.parser.word.model.data.DocumentHeaderInfo;
import com.torchv.infra.unstructured.parser.word.docx.DocxMarkdownImageExtractor;
import com.torchv.infra.unstructured.parser.word.model.data.MarkdownElementInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/4 19:23
 * @since torchv_server v1.7.3
 */
@Slf4j
public class MarkdownContentHandler extends ToTextContentHandler {
    
    protected final Map<String, String> namespaces = new HashMap<>();
    private final static Map<String, String> TABLE_MAP = new HashMap<>();
    
    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
        log.info("publicId:{},systemId:{}", publicId, systemId);
        return super.resolveEntity(publicId, systemId);
        
    }
    
    static {
        // TABLE_MAP
        TABLE_MAP.put("table", "\n");
        TABLE_MAP.put("tbody", "\n");
        TABLE_MAP.put("thead", "\n");
        TABLE_MAP.put("tr", "\n");
        TABLE_MAP.put("th", "\n");
        TABLE_MAP.put("td", "");
        TABLE_MAP.put("tfoot", "");
    }
    
    private static final Set<String> EMPTY_ELEMENTS = new HashSet<>(
            Arrays.asList("area", "base", "basefont", "br", "col", "frame", "hr", "img", "input",
                    "isindex", "link", "meta", "param"));
    
    private static final Set<String> BLOCK_ELEMENTS = new HashSet<>(
            Arrays.asList("address", "article", "aside", "blockquote", "canvas", "dd", "div", "dl",
                    "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3",
                    "h4", "h5", "h6", "header", "hr", "li", "main", "nav", "noscript", "ol", "p",
                    "pre", "section", "ul", "video", "img", "table", "tfoot", "tbody", "tr", "td", "th", "thead"));
    
    protected boolean inStartElement = false;
    protected boolean tableStart = false;
    protected String tableEndLine = "";
    private MarkdownElementInfo currentElement;
    
    public MarkdownContentHandler(OutputStream stream, String encoding)
                                                                        throws UnsupportedEncodingException {
        super(stream, encoding);
    }
    
    /**
     * Writes the XML prefix.
     */
    @Override
    public void startDocument() throws SAXException {
        currentElement = null;
        namespaces.clear();
    }
    
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        try {
            if (currentElement != null && prefix.equals(currentElement.getPrefix(uri))) {
                return;
            }
        } catch (SAXException ignore) {
        }
        namespaces.put(uri, prefix);
    }
    
    /**
     * Writes the XML start element.(Table)
     * @param currentElement current element
     * @param uri uri
     * @param localName local name
     * @param qName qName
     * @param atts attributes
     * @throws SAXException if the element could not be written
     */
    protected void writeTable(MarkdownElementInfo currentElement, String uri, String localName, String qName, Attributes atts) throws SAXException {
        write('<');
        write(currentElement.getQName(uri, localName));
        
        for (int i = 0; i < atts.getLength(); i++) {
            write(' ');
            write(currentElement.getQName(atts.getURI(i), atts.getLocalName(i)));
            write('=');
            write('"');
            char[] ch = atts.getValue(i).toCharArray();
            writeEscaped(ch, 0, ch.length, true);
            write('"');
        }
        namespaces.clear();
        tableStart = true;
        inStartElement = true;
    }
    
    /**
     * Writes the XML start element.(Image)
     * @param currentElement current element
     * @param atts attributes
     * @throws SAXException if the element could not be written
     */
    protected void writeImage(MarkdownElementInfo currentElement, Attributes atts) throws SAXException {
        DocxMarkdownImageExtractor imageInfoConvert = new DocxMarkdownImageExtractor(atts, currentElement);
        write('!');
        write('[');
        write(imageInfoConvert.getAttr());
        write(']');
        write('(');
        write(imageInfoConvert.getUrl());
        write(')');
    }
    
    /**
     * 判断当前元素是否为标题
     * @param name 标签元素，如h1,h2,h3,h4,h5,h6，或者<p  class="标题_1"></p>
     * @param attributes 属性
     * @return 是否为标题元素
     */
    protected DocumentHeaderInfo checkHeader(String name, MarkdownElementInfo currentElement, Attributes attributes) {
        DocumentHeaderInfo documentHeaderInfo = DocumentHeaderInfo.empty();
        documentHeaderInfo.initByHeaderTag(name);
        if (documentHeaderInfo.isHeader()) {
            return documentHeaderInfo;
        }
        // 判断属性
        if (attributes != null) {
            Map<String, String> mapAttr = new DocAttributeConverter(currentElement).apply(attributes);
            documentHeaderInfo = new DocHeadersConverter(name).apply(mapAttr);
        }
        return documentHeaderInfo;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        lazyCloseStartElement();
        currentElement = new MarkdownElementInfo(currentElement, namespaces);
        String name = Objects.toString(currentElement.getQName(uri, localName), StrUtil.EMPTY).toLowerCase();
        // log.info("uri:{},localName:{},qName:{},atts:{}", uri, localName, qName, atts);
        if (!BLOCK_ELEMENTS.contains(name)) {
            return;
        }
        if (TABLE_MAP.containsKey(name)) {
            writeTable(currentElement, uri, localName, qName, atts);
            tableEndLine = TABLE_MAP.get(name);
            return;
        }
        if (StrUtil.equalsIgnoreCase(name, "img")) {
            writeImage(currentElement, atts);
            return;
        }
        
        DocumentHeaderInfo headerInfo = checkHeader(name, currentElement, atts);
        if (headerInfo.isHeader()) {
            write(headerInfo.getHeaderText());
            write(" ");
            return;
        }
        namespaces.clear();
        inStartElement = true;
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (inStartElement) {
            if (TABLE_MAP.containsKey(qName.toLowerCase()) && tableStart) {
                write(" />");
            } else {
                write("\n");
                // line(qName);
            }
            inStartElement = false;
            tableStart = false;
            if (EMPTY_ELEMENTS.contains(localName)) {
                namespaces.clear();
                return;
            }
        } else {
            if (TABLE_MAP.containsKey(qName.toLowerCase())) {
                write("</");
                write(qName);
                write('>');
                write(TABLE_MAP.get(qName.toLowerCase()));
            } else {
                write("");
            }
        }
        namespaces.clear();
        // Reset the position in the tree, to avoid endless stack overflow
        // chains (see TIKA-1070)
        // currentElement = currentElement.parent;
        currentElement = currentElement.parent();
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        lazyCloseStartElement();
        writeEscaped(ch, start, start + length, false);
    }
    
    private void lazyCloseStartElement() throws SAXException {
        if (inStartElement) {
            // write('>');
            if (tableStart) {
                write('>');
                write(tableEndLine);
            } else {
                // write('\n');
                write("");
            }
            tableStart = false;
            tableEndLine = StrUtil.EMPTY;
            inStartElement = false;
        }
    }
    
    /**
     * Writes the given character as-is.
     *
     * @param ch character to be written
     * @throws SAXException if the character could not be written
     */
    protected void write(char ch) throws SAXException {
        super.characters(new char[]{ch}, 0, 1);
    }
    
    /**
     * Writes the given string of character as-is.
     *
     * @param string string of character to be written
     * @throws SAXException if the character string could not be written
     */
    protected void write(String string) throws SAXException {
        super.characters(string.toCharArray(), 0, string.length());
    }
    
    public void writeCustomString(String string) {
        try {
            this.write(string);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Writes the given characters as-is followed by the given entity.
     *
     * @param ch     character array
     * @param from   start position in the array
     * @param to     end position in the array
     * @param entity entity code
     * @return next position in the array,
     * after the characters plus one entity
     * @throws SAXException if the characters could not be written
     */
    private int writeCharsAndEntity(char[] ch, int from, int to, String entity) throws SAXException {
        super.characters(ch, from, to - from);
        write('&');
        write(entity);
        write(';');
        return to + 1;
    }
    
    /**
     * Writes the given characters with XML meta characters escaped.
     *
     * @param ch        character array
     * @param from      start position in the array
     * @param to        end position in the array
     * @param attribute whether the characters should be escaped as
     *                  an attribute value or normal character content
     * @throws SAXException if the characters could not be written
     */
    private void writeEscaped(char[] ch, int from, int to, boolean attribute) throws SAXException {
        int pos = from;
        while (pos < to) {
            if (ch[pos] == '<') {
                from = pos = writeCharsAndEntity(ch, from, pos, "lt");
            } else if (ch[pos] == '>') {
                from = pos = writeCharsAndEntity(ch, from, pos, "gt");
            } else if (ch[pos] == '&') {
                from = pos = writeCharsAndEntity(ch, from, pos, "amp");
            } else if (attribute && ch[pos] == '"') {
                from = pos = writeCharsAndEntity(ch, from, pos, "quot");
            } else {
                pos++;
            }
        }
        super.characters(ch, from, to - from);
    }
    
}
