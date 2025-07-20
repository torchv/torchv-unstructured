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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCX 表格解析器 - 正确处理合并单元格
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class DocxTableExtractor {
    
    /**
     * 解析 DOCX 文档中的所有表格为 HTML
     */
    public static List<String> parseAllTablesToHtml(XWPFDocument document) {
        List<String> tablesHtml = new ArrayList<>();
        
        List<XWPFTable> tables = document.getTables();
        log.info("DOCX 文档中共有 {} 个表格", tables.size());
        
        for (int i = 0; i < tables.size(); i++) {
            XWPFTable table = tables.get(i);
            String tableHtml = parseTableToHtml(table, i + 1);
            if (tableHtml != null && !tableHtml.trim().isEmpty()) {
                tablesHtml.add(tableHtml);
                log.debug("成功解析表格 #{}", i + 1);
            }
        }
        
        return tablesHtml;
    }
    
    /**
     * 解析单个表格为 HTML
     */
    public static String parseTableToHtml(XWPFTable table, int tableIndex) {
        try {
            HtmlTableBuilder builder = new HtmlTableBuilder();
            StringBuilder html = new StringBuilder();
            
            html.append(builder.startTable());
            
            List<XWPFTableRow> rows = table.getRows();
            log.debug("表格 #{} 共有 {} 行", tableIndex, rows.size());
            
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                XWPFTableRow row = rows.get(rowIndex);
                List<XWPFTableCell> cells = row.getTableCells();
                
                html.append(builder.startRow());
                log.debug("第 {} 行共有 {} 个单元格", rowIndex, cells.size());
                
                for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                    XWPFTableCell cell = cells.get(cellIndex);
                    
                    // 获取单元格内容
                    String cellText = extractCellText(cell);
                    
                    // 获取合并信息
                    CellMergeInfo mergeInfo = getCellMergeInfo(cell, rowIndex, cellIndex, table);
                    
                    // 跳过垂直合并的继续单元格
                    if (mergeInfo.rowspan == 0) {
                        log.debug("跳过垂直合并继续单元格 [{}][{}]", rowIndex, cellIndex);
                        continue;
                    }
                    
                    // 构建单元格 HTML
                    String cellHtml = builder.buildMergedCell(cellText, mergeInfo.colspan, mergeInfo.rowspan);
                    html.append(cellHtml);
                    
                    log.debug("单元格 [{}][{}]: '{}'，colspan={}, rowspan={}",
                            rowIndex, cellIndex, cellText, mergeInfo.colspan, mergeInfo.rowspan);
                }
                
                html.append(builder.endRow());
            }
            
            html.append(builder.endTable());
            return html.toString();
            
        } catch (Exception e) {
            log.error("解析表格 #{} 失败: {}", tableIndex, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 提取单元格文本内容
     */
    private static String extractCellText(XWPFTableCell cell) {
        StringBuilder cellText = new StringBuilder();
        
        // 获取单元格中的所有段落
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            XWPFParagraph paragraph = paragraphs.get(i);
            String paraText = paragraph.getText();
            
            if (paraText != null && !paraText.trim().isEmpty()) {
                if (cellText.length() > 0) {
                    cellText.append(" ");
                }
                cellText.append(paraText.trim());
            }
        }
        
        return cellText.toString().trim();
    }
    
    /**
     * 获取单元格合并信息
     */
    private static CellMergeInfo getCellMergeInfo(XWPFTableCell cell, int rowIndex, int cellIndex, XWPFTable table) {
        CellMergeInfo mergeInfo = new CellMergeInfo();
        
        try {
            // 获取单元格的 CTTc 对象
            if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null) {
                
                // 检查水平合并 (colspan)
                if (cell.getCTTc().getTcPr().getGridSpan() != null) {
                    BigInteger gridSpan = cell.getCTTc().getTcPr().getGridSpan().getVal();
                    if (gridSpan != null) {
                        mergeInfo.colspan = gridSpan.intValue();
                        log.debug("发现水平合并: colspan={}", mergeInfo.colspan);
                    }
                }
                
                // 检查垂直合并 (rowspan)
                if (cell.getCTTc().getTcPr().getVMerge() != null) {
                    String vMergeValue = null;
                    if (cell.getCTTc().getTcPr().getVMerge().getVal() != null) {
                        vMergeValue = cell.getCTTc().getTcPr().getVMerge().getVal().toString();
                    }
                    
                    if (vMergeValue == null || "restart".equals(vMergeValue)) {
                        // 这是合并的起始单元格，计算跨越的行数
                        mergeInfo.rowspan = calculateRowspan(table, rowIndex, cellIndex);
                        log.debug("发现垂直合并起始: rowspan={}", mergeInfo.rowspan);
                    } else if ("continue".equals(vMergeValue)) {
                        // 这是合并的继续单元格，应该被跳过
                        mergeInfo.rowspan = 0; // 标记为跳过
                        log.debug("发现垂直合并继续单元格，将被跳过");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取单元格合并信息失败: {}", e.getMessage());
        }
        
        return mergeInfo;
    }
    
    /**
     * 计算垂直合并的行数
     */
    private static int calculateRowspan(XWPFTable table, int startRowIndex, int cellIndex) {
        int rowspan = 1;
        List<XWPFTableRow> rows = table.getRows();
        
        // 从下一行开始检查
        for (int rowIndex = startRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            XWPFTableRow row = rows.get(rowIndex);
            List<XWPFTableCell> cells = row.getTableCells();
            
            if (cellIndex < cells.size()) {
                XWPFTableCell cell = cells.get(cellIndex);
                if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null) {
                    if (cell.getCTTc().getTcPr().getVMerge() != null) {
                        String vMergeValue = null;
                        if (cell.getCTTc().getTcPr().getVMerge().getVal() != null) {
                            vMergeValue = cell.getCTTc().getTcPr().getVMerge().getVal().toString();
                        }
                        if ("continue".equals(vMergeValue)) {
                            rowspan++;
                            continue;
                        }
                    }
                }
            }
            break; // 不再是合并的继续单元格
        }
        
        return rowspan;
    }
    
    /**
     * 单元格合并信息
     */
    private static class CellMergeInfo {
        
        int colspan = 1;
        int rowspan = 1;
    }
}
