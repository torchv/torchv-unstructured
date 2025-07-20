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
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.tika.sax.ToTextContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.Writer;
import java.util.*;

/**
 * 增强的 Markdown 内容处理器，支持将表格转换为 HTML 格式
 * 支持 DOCX 和 DOC 格式的表格处理
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class MarkdownWithTableHandler extends ToTextContentHandler {
    
    private final StringBuilder content = new StringBuilder();
    private final Stack<String> elementStack = new Stack<>();
    private boolean inTable = false;
    private boolean inTableRow = false;
    private boolean inTableCell = false;
    private StringBuilder currentTableHtml = new StringBuilder();
    
    // 用于处理标题的状态
    private boolean inHeading = false;
    private String currentHeadingLevel = "";
    
    // 用于处理段落的状态
    private boolean inParagraph = false;
    private boolean needsNewline = false;
    // DOC 格式支持
    private HWPFDocument currentDocDocument;
    private XWPFDocument currentDocxDocument;
    private List<String> cachedDocTables = new ArrayList<>();
    private int currentTableIndex = 0;
    
    public MarkdownWithTableHandler(Writer writer) {
        super(writer);
    }
    
    public MarkdownWithTableHandler(Writer writer, String encoding) {
        super(writer);
    }
    
    public MarkdownWithTableHandler(java.io.OutputStream outputStream, String encoding) {
        super(new java.io.OutputStreamWriter(outputStream, java.nio.charset.Charset.forName(encoding)));
    }
    
    /**
     * 设置当前的 DOC 文档，用于表格解析
     * 
     * @param document HWPFDocument 实例
     */
    public void setCurrentDocument(HWPFDocument document) {
        this.currentDocDocument = document;
        this.currentDocxDocument = null;
        this.currentTableIndex = 0;
        
        // 预解析所有表格，避免重复解析
        if (document != null) {
            try {
                this.cachedDocTables = parseAllDocTablesToHtml(document);
                log.debug("预解析 DOC 表格完成，共 {} 个表格", cachedDocTables.size());
            } catch (Exception e) {
                log.error("预解析 DOC 表格失败: {}", e.getMessage(), e);
                this.cachedDocTables = new ArrayList<>();
            }
        }
    }
    
    /**
     * 设置当前的 DOCX 文档
     * 
     * @param document XWPFDocument 实例
     */
    public void setCurrentDocument(XWPFDocument document) {
        this.currentDocxDocument = document;
        this.currentDocDocument = null;
        this.currentTableIndex = 0;
        this.cachedDocTables = new ArrayList<>();
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
        } else if (inTableCell) {
            // 表格单元格内容直接添加到HTML中
            currentTableHtml.append(escapeHtml(text));
        } else if (inTable) {
            // 在表格中但不在单元格中，忽略
        } else {
            // 普通文本内容
            content.append(text);
        }
    }
    
    private void handleTableStart(Attributes attributes) {
        inTable = true;
        currentTableHtml = new StringBuilder();
        
        // 如果是DOC格式，尝试使用缓存的表格
        if (currentDocDocument != null && currentTableIndex < cachedDocTables.size()) {
            String cachedTable = cachedDocTables.get(currentTableIndex);
            if (cachedTable != null && !cachedTable.isEmpty()) {
                log.debug("使用缓存的 DOC 表格 #{}", currentTableIndex);
                content.append("\n\n").append(cachedTable).append("\n\n");
                currentTableIndex++;
                // 跳过正常的表格处理
                return;
            }
        }
        
        // 开始标准的表格HTML构建
        currentTableHtml.append("<table border=\"1\" style=\"border-collapse: collapse;\">");
    }
    
    private void handleTableEnd() {
        if (inTable) {
            // 如果没有使用缓存的表格，完成HTML构建
            if (currentTableHtml.length() > 0 && !currentTableHtml.toString()
                    .equals("<table border=\"1\" style=\"border-collapse: collapse;\">")) {
                currentTableHtml.append("</table>");
                content.append("\n\n").append(currentTableHtml.toString()).append("\n\n");
            }
            
            inTable = false;
            currentTableHtml = new StringBuilder();
        }
    }
    
    private void handleTableRowStart() {
        if (inTable) {
            inTableRow = true;
            currentTableHtml.append("<tr>");
        }
    }
    
    private void handleTableRowEnd() {
        if (inTableRow) {
            inTableRow = false;
            currentTableHtml.append("</tr>");
        }
    }
    
    private void handleTableCellStart(String qName, Attributes attributes) {
        if (inTableRow) {
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
        if (inTableCell) {
            inTableCell = false;
            currentTableHtml.append("</td>");
        }
    }
    
    /**
     * 写入HTML表格（用于外部调用）
     * 
     * @param tableHtml HTML表格字符串
     */
    public void writeHtmlTable(String tableHtml) {
        if (tableHtml != null && !tableHtml.trim().isEmpty()) {
            content.append("\n\n").append(tableHtml).append("\n\n");
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
     * 获取解析的 HTML 表格内容
     * 
     * @return HTML 表格内容列表
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
    
    /**
     * 解析 DOC 文档中的表格并转换为 HTML
     * 
     * @param document HWPFDocument 实例
     * @return 所有表格的 HTML 字符串列表
     */
    private List<String> parseAllDocTablesToHtml(HWPFDocument document) {
        List<String> htmlTables = new ArrayList<>();
        Set<Integer> processedTablePositions = new HashSet<>();
        
        try {
            Range range = document.getRange();
            
            // 遍历文档中的所有段落，查找表格
            int numParagraphs = range.numParagraphs();
            log.debug("DOC 文档中共有 {} 个段落", numParagraphs);
            
            for (int i = 0; i < numParagraphs; i++) {
                try {
                    if (range.getParagraph(i).isInTable()) {
                        Table table = range.getTable(range.getParagraph(i));
                        
                        // 使用表格的起始位置作为唯一标识，避免重复处理
                        int tableStartOffset = table.getStartOffset();
                        if (!processedTablePositions.contains(tableStartOffset)) {
                            processedTablePositions.add(tableStartOffset);
                            String htmlTable = parseDocTableToHtml(table);
                            if (!htmlTable.isEmpty()) {
                                htmlTables.add(htmlTable);
                                log.debug("成功解析表格 #{}, 起始位置: {}", htmlTables.size(), tableStartOffset);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("跳过段落 {}: {}", i, e.getMessage());
                }
            }
            
            log.debug("DOC 文档中共解析到 {} 个表格", htmlTables.size());
            
        } catch (Exception e) {
            log.error("解析 DOC 表格时发生错误: {}", e.getMessage(), e);
        }
        
        return htmlTables;
    }
    
    /**
     * 解析单个 DOC 表格并转换为 HTML
     * 
     * @param table Table 实例
     * @return HTML 字符串
     */
    private String parseDocTableToHtml(Table table) {
        if (table == null) {
            return "";
        }
        
        try {
            StringBuilder html = new StringBuilder();
            html.append("<table border=\"1\" style=\"border-collapse: collapse;\">\n");
            
            int numRows = table.numRows();
            log.debug("表格共有 {} 行", numRows);
            
            // 创建一个二维数组来跟踪哪些单元格已经被合并单元格占用
            boolean[][] occupied = new boolean[numRows][];
            
            // 首先遍历所有行，确定每行的最大列数
            int maxCols = 0;
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                TableRow row = table.getRow(rowIndex);
                maxCols = Math.max(maxCols, row.numCells());
            }
            
            // 初始化占用数组
            for (int i = 0; i < numRows; i++) {
                occupied[i] = new boolean[maxCols];
            }
            
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                TableRow row = table.getRow(rowIndex);
                html.append("  <tr>\n");
                
                int numCells = row.numCells();
                log.debug("第 {} 行共有 {} 个单元格", rowIndex, numCells);
                
                int colIndex = 0; // 实际列索引
                for (int cellIndex = 0; cellIndex < numCells; cellIndex++) {
                    // 跳过被合并单元格占用的列
                    while (colIndex < maxCols && occupied[rowIndex][colIndex]) {
                        colIndex++;
                    }
                    
                    if (colIndex >= maxCols) {
                        break; // 超出列范围
                    }
                    
                    TableCell cell = row.getCell(cellIndex);
                    String cellText = extractDocCellText(cell);
                    
                    // 检测合并单元格
                    int colspan = detectColspan(table, rowIndex, cellIndex, cellText);
                    int rowspan = detectRowspan(table, rowIndex, cellIndex, cellText);
                    
                    // 标记被此单元格占用的区域
                    for (int r = rowIndex; r < Math.min(rowIndex + rowspan, numRows); r++) {
                        for (int c = colIndex; c < Math.min(colIndex + colspan, maxCols); c++) {
                            occupied[r][c] = true;
                        }
                    }
                    
                    // 生成单元格HTML
                    html.append("    <td");
                    
                    if (colspan > 1) {
                        html.append(" colspan=\"").append(colspan).append("\"");
                    }
                    if (rowspan > 1) {
                        html.append(" rowspan=\"").append(rowspan).append("\"");
                    }
                    
                    html.append(" style=\"border: 1px solid #ccc; padding: 8px;\">");
                    html.append(cellText.isEmpty() ? "&nbsp;" : escapeHtml(cellText));
                    html.append("</td>\n");
                    
                    colIndex++;
                }
                
                html.append("  </tr>\n");
            }
            
            html.append("</table>\n");
            return html.toString();
            
        } catch (Exception e) {
            log.error("解析表格时发生错误: {}", e.getMessage(), e);
            return "<table border=\"1\" style=\"border-collapse: collapse;\"><tr><td>表格解析失败: " + e.getMessage()
                    + "</td></tr></table>";
        }
    }
    
    /**
     * 检测列合并（colspan）
     */
    private int detectColspan(Table table, int rowIndex, int cellIndex, String cellText) {
        int colspan = 1;
        try {
            TableRow row = table.getRow(rowIndex);
            
            // 检查右侧单元格是否为空或相同内容
            for (int nextIndex = cellIndex + 1; nextIndex < row.numCells(); nextIndex++) {
                TableCell nextCell = row.getCell(nextIndex);
                String nextCellText = extractDocCellText(nextCell);
                
                if (nextCellText.isEmpty() ||
                        (nextCellText.equals(cellText) && !cellText.isEmpty())) {
                    colspan++;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("检测列合并时发生错误: {}", e.getMessage());
        }
        return colspan;
    }
    
    /**
     * 检测行合并（rowspan）
     */
    private int detectRowspan(Table table, int rowIndex, int cellIndex, String cellText) {
        int rowspan = 1;
        try {
            // 检查下方单元格是否为空或相同内容
            for (int nextRowIndex = rowIndex + 1; nextRowIndex < table.numRows(); nextRowIndex++) {
                TableRow nextRow = table.getRow(nextRowIndex);
                if (cellIndex < nextRow.numCells()) {
                    TableCell nextRowCell = nextRow.getCell(cellIndex);
                    String nextRowText = extractDocCellText(nextRowCell);
                    
                    if (nextRowText.isEmpty() ||
                            (nextRowText.equals(cellText) && !cellText.isEmpty())) {
                        rowspan++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("检测行合并时发生错误: {}", e.getMessage());
        }
        return rowspan;
    }
    
    /**
     * 提取 DOC 单元格文本内容
     * 
     * @param cell TableCell 实例
     * @return 单元格文本内容
     */
    private String extractDocCellText(TableCell cell) {
        if (cell == null) {
            return "";
        }
        
        try {
            String text = cell.text();
            if (text == null) {
                return "";
            }
            
            // 清理文本内容
            text = text.trim();
            
            // 移除末尾的表格单元格结束符（\u0007）
            if (text.endsWith("\u0007")) {
                text = text.substring(0, text.length() - 1);
            }
            
            // 移除其他控制字符
            text = text.replaceAll("[\u0000-\u001F\u007F]", "");
            
            return text.trim();
            
        } catch (Exception e) {
            log.error("提取单元格文本时发生错误: {}", e.getMessage(), e);
            return "";
        }
    }
}
