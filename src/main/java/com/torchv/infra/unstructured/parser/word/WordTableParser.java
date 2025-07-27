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


package com.torchv.infra.unstructured.parser.word;

import com.torchv.infra.unstructured.parser.word.builder.HtmlTableBuilder;
import com.torchv.infra.unstructured.parser.word.model.CellInfo;
import com.torchv.infra.unstructured.parser.word.analyzer.CellMergeAnalyzer;
import com.torchv.infra.unstructured.parser.word.analyzer.TableValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;

import java.util.ArrayList;
import java.util.List;

/**
 * Word 表格解析器
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class WordTableParser {
    
    private final CellMergeAnalyzer cellMergeAnalyzer;
    private final HtmlTableBuilder htmlTableBuilder;
    
    public WordTableParser() {
        this.cellMergeAnalyzer = new CellMergeAnalyzer();
        this.htmlTableBuilder = new HtmlTableBuilder();
    }
    
    public WordTableParser(CellMergeAnalyzer cellMergeAnalyzer, HtmlTableBuilder htmlTableBuilder) {
        this.cellMergeAnalyzer = cellMergeAnalyzer;
        this.htmlTableBuilder = htmlTableBuilder;
    }
    
    /**
     * 解析 Word 文档中的所有表格为 HTML
     */
    public String parseToHtml(XWPFDocument document) {
        List<XWPFTable> tables = document.getTables();
        log.info("开始解析Word表格，共 {} 个表格", tables.size());
        
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < tables.size(); i++) {
            XWPFTable table = tables.get(i);
            log.info("解析第 {} 个表格，包含 {} 行", i + 1, table.getRows().size());
            
            html.append(parseTableToHtml(table));
            
            if (i < tables.size() - 1) {
                html.append("<br/><br/>\n");
            }
        }
        
        return html.toString();
    }
    
    /**
     * 解析 Word 文档中的所有表格为 HTML 列表
     */
    public List<String> parseToHtmlList(XWPFDocument document) {
        List<XWPFTable> tables = document.getTables();
        log.info("开始解析Word表格，共 {} 个表格", tables.size());
        
        List<String> htmlTables = new ArrayList<>();
        for (int i = 0; i < tables.size(); i++) {
            XWPFTable table = tables.get(i);
            log.info("解析第 {} 个表格，包含 {} 行", i + 1, table.getRows().size());
            
            String tableHtml = parseTableToHtml(table);
            htmlTables.add(tableHtml);
        }
        
        return htmlTables;
    }
    
    /**
     * 解析单个表格为 HTML
     */
    public String parseTableToHtml(XWPFTable table) {
        StringBuilder html = new StringBuilder();
        html.append(htmlTableBuilder.startTable());
        
        // 预分析表格以构建逻辑网格映射
        int[][] logicalGrid = buildLogicalGrid(table);
        
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            XWPFTableRow row = table.getRows().get(rowIndex);
            html.append(htmlTableBuilder.startRow());
            
            for (int cellIndex = 0; cellIndex < row.getTableCells().size(); cellIndex++) {
                XWPFTableCell cell = row.getTableCells().get(cellIndex);
                
                // 检查是否应该跳过此单元格（被上方单元格合并）
                if (cellMergeAnalyzer.shouldSkipCell(cell)) {
                    log.debug("跳过被合并的单元格 [{}][{}]", rowIndex, cellIndex);
                    continue;
                }
                
                // 获取当前单元格的逻辑列索引
                int logicalColIndex = logicalGrid[rowIndex][cellIndex];
                
                // 获取单元格信息
                CellInfo cellInfo = analyzeCellInfo(table, rowIndex, cellIndex, logicalColIndex, cell);
                
                log.debug("处理单元格 [{}][{}] (逻辑列:{}): 文本='{}', colspan={}, rowspan={}",
                        rowIndex, cellIndex, logicalColIndex, cellInfo.getText(), cellInfo.getColspan(),
                        cellInfo.getRowspan());
                
                // 生成 HTML 单元格
                html.append(htmlTableBuilder.buildCell(cellInfo));
            }
            
            html.append(htmlTableBuilder.endRow());
        }
        
        html.append(htmlTableBuilder.endTable());
        return html.toString();
    }
    
    /**
     * 分析单元格信息
     */
    private CellInfo analyzeCellInfo(XWPFTable table, int rowIndex, int physicalColIndex, int logicalColIndex,
                                     XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        
        int colspan = cellMergeAnalyzer.getColspan(cellPr);
        int rowspan = cellMergeAnalyzer.calculateRowspan(table, rowIndex, physicalColIndex);
        
        String text = cell.getText();
        if (text == null || text.trim().isEmpty()) {
            text = "&nbsp;";
        } else {
            text = text.trim();
        }
        
        return new CellInfo(text, colspan, rowspan);
    }
    
    /**
     * 构建逻辑网格映射
     * 这个方法创建一个映射表，将每个物理单元格索引映射到逻辑列索引
     */
    private int[][] buildLogicalGrid(XWPFTable table) {
        int rowCount = table.getRows().size();
        int maxCells = 0;
        
        // 首先找到最大单元格数量
        for (XWPFTableRow row : table.getRows()) {
            maxCells = Math.max(maxCells, row.getTableCells().size());
        }
        
        int[][] grid = new int[rowCount][maxCells];
        
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            XWPFTableRow row = table.getRows().get(rowIndex);
            int logicalColIndex = 0;
            
            for (int cellIndex = 0; cellIndex < row.getTableCells().size(); cellIndex++) {
                XWPFTableCell cell = row.getTableCells().get(cellIndex);
                
                // 跳过被垂直合并的单元格，不会占用逻辑列位置
                if (cellMergeAnalyzer.shouldSkipCell(cell)) {
                    grid[rowIndex][cellIndex] = -1; // 标记为跳过
                    continue;
                }
                
                // 记录当前单元格的逻辑列索引
                grid[rowIndex][cellIndex] = logicalColIndex;
                
                // 获取colspan以计算下一个逻辑列位置
                CTTcPr cellPr = cell.getCTTc().getTcPr();
                int colspan = cellMergeAnalyzer.getColspan(cellPr);
                logicalColIndex += colspan;
            }
        }
        
        return grid;
    }
    
    /**
     * 获取表格中所有文本内容（用于调试和验证）
     */
    public List<String> extractAllTexts(XWPFTable table) {
        List<String> texts = new ArrayList<>();
        
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                String text = cell.getText();
                if (text != null && !text.trim().isEmpty()) {
                    texts.add(text.trim());
                }
            }
        }
        
        return texts;
    }
    
    /**
     * 验证解析结果是否包含预期内容
     */
    public TableValidationResult validateContent(String htmlResult, String[] expectedTexts) {
        List<String> found = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        for (String expectedText : expectedTexts) {
            if (htmlResult.contains(expectedText)) {
                found.add(expectedText);
            } else {
                missing.add(expectedText);
            }
        }
        
        return new TableValidationResult(found, missing);
    }
}
