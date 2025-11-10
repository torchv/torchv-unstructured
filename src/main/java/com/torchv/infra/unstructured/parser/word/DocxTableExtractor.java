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
        System.out.println("****** getCellMergeInfo 被调用: [" + rowIndex + "][" + cellIndex + "] ******");
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
            
            // 总是运行网格分析作为补充检查
            System.out.println("调用网格分析: 行" + rowIndex + " 列" + cellIndex);
            int gridRowspan = calculateRowspanByGridAnalysis(table, rowIndex, cellIndex);
            System.out.println("网格分析结果: rowspan=" + gridRowspan + ", 当前rowspan=" + mergeInfo.rowspan);
            if (gridRowspan > 1 && mergeInfo.rowspan == 1) {
                mergeInfo.rowspan = gridRowspan;
                System.out.println("通过网格分析推断rowspan: [" + rowIndex + "][" + cellIndex + "] rowspan=" + gridRowspan);
            }
            
        } catch (Exception e) {
            log.warn("获取单元格合并信息失败: {}", e.getMessage());
        }
        
        return mergeInfo;
    }
    
    /**
     * 计算垂直合并的行数 - 简化直接计算方法
     */
    private static int calculateRowspan(XWPFTable table, int startRowIndex, int cellIndex) {
        List<XWPFTableRow> rows = table.getRows();
        
        if (startRowIndex >= rows.size()) {
            return 1;
        }
        
        XWPFTableRow startRow = rows.get(startRowIndex);
        List<XWPFTableCell> startCells = startRow.getTableCells();
        
        if (cellIndex >= startCells.size()) {
            return 1;
        }
        
        XWPFTableCell startCell = startCells.get(cellIndex);
        
        // 检查起始单元格是否有vMerge
        if (startCell.getCTTc() == null || startCell.getCTTc().getTcPr() == null ||
                startCell.getCTTc().getTcPr().getVMerge() == null) {
            return 1;
        }
        
        String vMergeValue = null;
        if (startCell.getCTTc().getTcPr().getVMerge().getVal() != null) {
            vMergeValue = startCell.getCTTc().getTcPr().getVMerge().getVal().toString();
        }
        
        // 只有restart或null才是合并的起始单元格
        if (vMergeValue != null && !"restart".equals(vMergeValue)) {
            return 1;
        }
        
        // 调试信息
        log.info("表格第{}行第{}列单元格检测到vMerge起始，开始计算rowspan", startRowIndex, cellIndex);
        
        // 计算逻辑列位置 - 考虑前面单元格的colspan
        int logicalColumn = 0;
        for (int i = 0; i < cellIndex; i++) {
            XWPFTableCell prevCell = startCells.get(i);
            int colspan = 1;
            if (prevCell.getCTTc() != null && prevCell.getCTTc().getTcPr() != null &&
                    prevCell.getCTTc().getTcPr().getGridSpan() != null) {
                BigInteger gridSpan = prevCell.getCTTc().getTcPr().getGridSpan().getVal();
                if (gridSpan != null) {
                    colspan = gridSpan.intValue();
                }
            }
            logicalColumn += colspan;
        }
        
        log.info("计算得到逻辑列位置为: {}", logicalColumn);
        
        // 从下一行开始检查合并的单元格
        int rowspan = 1;
        for (int rowIdx = startRowIndex + 1; rowIdx < rows.size(); rowIdx++) {
            XWPFTableRow row = rows.get(rowIdx);
            
            // 找到对应逻辑列位置的单元格
            XWPFTableCell targetCell = findCellAtLogicalColumn(row, logicalColumn);
            
            if (targetCell == null) {
                log.debug("第{}行逻辑列{}位置未找到单元格，停止计算", rowIdx, logicalColumn);
                break;
            }
            
            // 检查这个单元格是否是合并的延续
            if (targetCell.getCTTc() != null && targetCell.getCTTc().getTcPr() != null &&
                    targetCell.getCTTc().getTcPr().getVMerge() != null) {
                
                String targetVMergeValue = null;
                if (targetCell.getCTTc().getTcPr().getVMerge().getVal() != null) {
                    targetVMergeValue = targetCell.getCTTc().getTcPr().getVMerge().getVal().toString();
                }
                
                log.debug("第{}行逻辑列{}位置单元格vMerge值: {}", rowIdx, logicalColumn, targetVMergeValue);
                
                if ("continue".equals(targetVMergeValue)) {
                    rowspan++;
                } else {
                    // restart或其他值，停止计算
                    log.debug("遇到非continue的vMerge值，停止计算");
                    break;
                }
            } else {
                // 没有vMerge，停止
                log.debug("第{}行逻辑列{}位置单元格没有vMerge属性，停止计算", rowIdx, logicalColumn);
                break;
            }
        }
        
        log.info("最终计算的rowspan值: {}", rowspan);
        return rowspan;
    }
    
    /**
     * 通过网格结构分析计算rowspan - 基于单元格数量差异推断
     */
    private static int calculateRowspanByGridAnalysis(XWPFTable table, int startRowIndex, int cellIndex) {
        System.out.println("开始网格分析: 表格行数=" + table.getRows().size() + ", 起始行=" + startRowIndex + ", 单元格=" + cellIndex);
        
        List<XWPFTableRow> rows = table.getRows();
        
        if (startRowIndex >= rows.size() - 1) {
            System.out.println("最后一行，返回rowspan=1");
            return 1; // 最后一行不可能有rowspan
        }
        
        XWPFTableRow currentRow = rows.get(startRowIndex);
        List<XWPFTableCell> currentCells = currentRow.getTableCells();
        
        // 计算当前行到此单元格为止的逻辑列数（考虑colspan）
        int logicalColumnIndex = 0;
        for (int i = 0; i < cellIndex; i++) {
            XWPFTableCell prevCell = currentCells.get(i);
            int colspan = getColspan(prevCell);
            logicalColumnIndex += colspan;
        }
        
        // 获取当前单元格的colspan
        int currentColspan = getColspan(currentCells.get(cellIndex));
        
        // 检查后续行在相同逻辑列位置是否缺少单元格
        int rowspan = 1;
        for (int nextRowIndex = startRowIndex + 1; nextRowIndex < rows.size(); nextRowIndex++) {
            XWPFTableRow nextRow = rows.get(nextRowIndex);
            List<XWPFTableCell> nextCells = nextRow.getTableCells();
            
            // 计算下一行的逻辑列数分布
            int[] nextRowLogicalColumns = calculateLogicalColumns(nextRow);
            
            // 检查在对应逻辑列位置是否有单元格覆盖
            boolean hasCellInLogicalRange = false;
            for (int i = 0; i < nextRowLogicalColumns.length; i++) {
                int nextLogicalStart = nextRowLogicalColumns[i];
                int nextColspan = getColspan(nextCells.get(i));
                int nextLogicalEnd = nextLogicalStart + nextColspan - 1;
                
                // 检查是否与当前单元格的逻辑列范围重叠
                if (!(nextLogicalEnd < logicalColumnIndex ||
                        nextLogicalStart > logicalColumnIndex + currentColspan - 1)) {
                    hasCellInLogicalRange = true;
                    break;
                }
            }
            
            if (!hasCellInLogicalRange) {
                // 下一行在这个逻辑列范围没有单元格，可能被当前单元格合并
                rowspan++;
            } else {
                // 下一行有单元格覆盖，停止计算
                break;
            }
        }
        
        System.out.println("网格分析计算完成，最终rowspan=" + rowspan);
        return rowspan;
    }
    
    /**
     * 计算行中每个单元格对应的逻辑列起始位置
     */
    private static int[] calculateLogicalColumns(XWPFTableRow row) {
        List<XWPFTableCell> cells = row.getTableCells();
        int[] logicalColumns = new int[cells.size()];
        int logicalCol = 0;
        
        for (int i = 0; i < cells.size(); i++) {
            logicalColumns[i] = logicalCol;
            int colspan = getColspan(cells.get(i));
            logicalCol += colspan;
        }
        
        return logicalColumns;
    }
    
    /**
     * 获取单元格的colspan值
     */
    private static int getColspan(XWPFTableCell cell) {
        try {
            if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null &&
                    cell.getCTTc().getTcPr().getGridSpan() != null) {
                BigInteger gridSpan = cell.getCTTc().getTcPr().getGridSpan().getVal();
                if (gridSpan != null) {
                    return gridSpan.intValue();
                }
            }
        } catch (Exception e) {
            log.warn("获取colspan失败: {}", e.getMessage());
        }
        return 1;
    }
    
    /**
     * 在指定行的指定逻辑列位置找到单元格
     */
    private static XWPFTableCell findCellAtLogicalColumn(XWPFTableRow row, int targetLogicalColumn) {
        List<XWPFTableCell> cells = row.getTableCells();
        int logicalColumn = 0;
        
        for (int cellIdx = 0; cellIdx < cells.size(); cellIdx++) {
            XWPFTableCell cell = cells.get(cellIdx);
            
            // 获取当前单元格的colspan
            int colspan = getColspan(cell);
            
            // 检查目标逻辑列是否在当前单元格的范围内
            if (targetLogicalColumn >= logicalColumn && targetLogicalColumn < logicalColumn + colspan) {
                return cell;
            }
            
            logicalColumn += colspan;
        }
        
        return null;
    }
    
    /**
     * 单元格合并信息
     */
    private static class CellMergeInfo {
        
        int colspan = 1;
        int rowspan = 1;
    }
}
