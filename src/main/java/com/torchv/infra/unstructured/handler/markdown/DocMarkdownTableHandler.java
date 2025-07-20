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

import com.torchv.infra.unstructured.handler.table.DocumentTableParser;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * DOC格式专用的Markdown内容处理器，支持将表格转换为HTML格式
 * 专门处理DOC格式，不影响原有的DOCX处理逻辑
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class DocMarkdownTableHandler extends ToTextContentHandler {
    
    private final StringBuilder content = new StringBuilder();
    private final Stack<String> elementStack = new Stack<>();
    private boolean inTable = false;
    private boolean inTableRow = false;
    private boolean inTableCell = false;
    private boolean skipCurrentTable = false; // 新增：用于跳过已经使用缓存处理的表格
    private StringBuilder currentTableHtml = new StringBuilder();
    
    // 用于处理标题的状态
    private boolean inHeading = false;
    private String currentHeadingLevel = "";
    
    // 用于处理段落的状态
    private boolean inParagraph = false;
    
    // DOC格式专用
    private HWPFDocument currentDocDocument;
    private List<String> cachedDocTables = new ArrayList<>();
    private int currentTableIndex = 0;
    
    // 图片处理相关
    private List<DocumentImage> pictures = new ArrayList<>();
    private int currentPictureIndex = 0;
    
    public DocMarkdownTableHandler(Writer writer) {
        super(writer);
    }
    
    public DocMarkdownTableHandler(Writer writer, String encoding) {
        super(writer);
    }
    
    public DocMarkdownTableHandler(java.io.OutputStream outputStream, String encoding) {
        super(new java.io.OutputStreamWriter(outputStream, java.nio.charset.Charset.forName(encoding)));
    }
    
    /**
     * 设置当前的DOC文档，用于表格解析
     * 
     * @param document HWPFDocument实例
     */
    public void setCurrentDocument(HWPFDocument document) {
        this.currentDocDocument = document;
        this.currentTableIndex = 0;
        
        // 使用新的DOC表格解析器预解析所有表格
        if (document != null) {
            try {
                this.cachedDocTables = DocumentTableParser.parseAllTablesToHtml(document);
                log.debug("使用新解析器预解析DOC表格完成，共{}个表格", cachedDocTables.size());
            } catch (Exception e) {
                log.error("预解析DOC表格失败: {}", e.getMessage(), e);
                this.cachedDocTables = new ArrayList<>();
            }
        }
    }
    
    /**
     * 设置图片列表
     * 
     * @param pictures 图片列表
     */
    public void setPictures(List<DocumentImage> pictures) {
        this.pictures = pictures != null ? new ArrayList<>(pictures) : new ArrayList<>();
        this.currentPictureIndex = 0;
    }
    
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        elementStack.push(qName);
        
        switch (qName.toLowerCase()) {
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                inHeading = true;
                currentHeadingLevel = "#".repeat(Integer.parseInt(qName.substring(1)));
                break;
            
            case "p":
                inParagraph = true;
                break;
            
            case "table":
                handleTableStart(attributes);
                break;
            
            case "tr":
                handleTableRowStart();
                break;
            
            case "td":
            case "th":
                handleTableCellStart(qName, attributes);
                break;
            
            case "br":
                if (inTableCell) {
                    currentTableHtml.append("<br>");
                } else {
                    content.append("\n");
                }
                break;
            
            case "img":
                handleImageStart(attributes);
                break;
        }
    }
    
    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (!elementStack.isEmpty()) {
            elementStack.pop();
        }
        
        switch (qName.toLowerCase()) {
            case "h1":
            case "h2":
            case "h3":
            case "h4":
            case "h5":
            case "h6":
                inHeading = false;
                content.append("\n\n");
                break;
            
            case "p":
                inParagraph = false;
                if (!inTable) {
                    content.append("\n\n");
                }
                break;
            
            case "table":
                handleTableEnd();
                break;
            
            case "tr":
                handleTableRowEnd();
                break;
            
            case "td":
            case "th":
                handleTableCellEnd();
                break;
        }
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String text = new String(ch, start, length);
        
        if (inHeading) {
            content.append(currentHeadingLevel).append(" ").append(text.trim());
        } else if (inTableCell && !skipCurrentTable) {
            // 表格单元格内容直接添加到HTML中（只有在不跳过当前表格时）
            currentTableHtml.append(escapeHtml(text));
        } else if (inTable && skipCurrentTable) {
            // 在跳过的表格中，忽略所有内容
        } else if (inTable) {
            // 在表格中但不在单元格中，忽略
        } else {
            // 普通文本内容
            content.append(text);
        }
    }
    
    private void handleTableStart(Attributes attributes) {
        inTable = true;
        skipCurrentTable = false;
        currentTableHtml = new StringBuilder();
        
        // 使用缓存的DOC表格
        if (currentDocDocument != null && currentTableIndex < cachedDocTables.size()) {
            String cachedTable = cachedDocTables.get(currentTableIndex);
            if (cachedTable != null && !cachedTable.isEmpty()) {
                log.debug("使用新解析器缓存的DOC表格#{}", currentTableIndex);
                log.debug("表格内容前200字符: {}",
                        cachedTable.length() > 200 ? cachedTable.substring(0, 200) + "..." : cachedTable);
                content.append("\n\n").append(cachedTable).append("\n\n");
                currentTableIndex++;
                skipCurrentTable = true; // 设置跳过标志，忽略后续的表格元素
                return;
            }
        }
        
        // 如果没有缓存表格，则开始标准的表格HTML构建
        currentTableHtml.append("<table border=\"1\" style=\"border-collapse: collapse;\">");
    }
    
    private void handleTableEnd() {
        if (inTable) {
            if (skipCurrentTable) {
                // 如果跳过了当前表格，重置状态但不输出内容
                skipCurrentTable = false;
            } else {
                // 如果没有使用缓存的表格，完成HTML构建
                if (currentTableHtml.length() > 0 && !currentTableHtml.toString()
                        .equals("<table border=\"1\" style=\"border-collapse: collapse;\">")) {
                    currentTableHtml.append("</table>");
                    content.append("\n\n").append(currentTableHtml.toString()).append("\n\n");
                }
            }
            
            inTable = false;
            currentTableHtml = new StringBuilder();
        }
    }
    
    private void handleTableRowStart() {
        if (inTable && !skipCurrentTable) {
            inTableRow = true;
            currentTableHtml.append("<tr>");
        }
    }
    
    private void handleTableRowEnd() {
        if (inTableRow && !skipCurrentTable) {
            inTableRow = false;
            currentTableHtml.append("</tr>");
        }
    }
    
    private void handleTableCellStart(String qName, Attributes attributes) {
        if (inTableRow && !skipCurrentTable) {
            inTableCell = true;
            
            // 处理合并单元格属性
            String colspan = attributes.getValue("colspan");
            String rowspan = attributes.getValue("rowspan");
            
            if ("th".equals(qName.toLowerCase())) {
                currentTableHtml.append("<th");
            } else {
                currentTableHtml.append("<td");
            }
            
            if (colspan != null && !colspan.isEmpty()) {
                currentTableHtml.append(" colspan=\"").append(colspan).append("\"");
            }
            if (rowspan != null && !rowspan.isEmpty()) {
                currentTableHtml.append(" rowspan=\"").append(rowspan).append("\"");
            }
            
            currentTableHtml.append(" style=\"border: 1px solid #ccc; padding: 8px;\">");
        }
    }
    
    private void handleTableCellEnd() {
        if (inTableCell && !skipCurrentTable) {
            inTableCell = false;
            currentTableHtml.append("</td>");
        }
    }
    
    /**
     * 处理图片元素
     */
    private void handleImageStart(Attributes attributes) {
        // 尝试在图片位置插入正确的图片标记
        if (currentPictureIndex < pictures.size()) {
            DocumentImage picture = pictures.get(currentPictureIndex);
            String imageMarkdown = String.format("\n\n![%s](%s)\n\n",
                    picture.getSourceName() != null ? picture.getSourceName() : "图片" + (currentPictureIndex + 1),
                    picture.getOssUrl() != null ? picture.getOssUrl() : "");
            
            if (inTableCell) {
                currentTableHtml.append(imageMarkdown);
            } else {
                content.append(imageMarkdown);
            }
            
            currentPictureIndex++;
            log.debug("在位置插入图片标记: {}", picture.getSourceName());
        } else {
            // 如果没有对应的图片信息，插入占位符
            String placeholder = "\n\n![图片]\n\n";
            if (inTableCell) {
                currentTableHtml.append(placeholder);
            } else {
                content.append(placeholder);
            }
            log.debug("插入图片占位符");
        }
    }
    
    /**
     * 获取当前的内容
     * 
     * @return 处理后的内容
     */
    public String getContent() {
        return content.toString();
    }
    
    /**
     * 获取解析的HTML表格内容
     * 
     * @return HTML表格内容列表
     */
    public List<String> getDocTablesHtml() {
        return cachedDocTables != null ? new ArrayList<>(cachedDocTables) : new ArrayList<>();
    }
    
    /**
     * 清理内容
     */
    public void clear() {
        content.setLength(0);
        elementStack.clear();
        inTable = false;
        inTableRow = false;
        inTableCell = false;
        inHeading = false;
        inParagraph = false;
        currentTableHtml = new StringBuilder();
        currentTableIndex = 0;
        cachedDocTables.clear();
        currentPictureIndex = 0;
    }
    
    /**
     * HTML转义
     * 
     * @param text 待转义的文本
     * @return 转义后的文本
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
