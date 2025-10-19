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


package com.torchv.infra.unstructured.parser.word.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTVMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.List;

/**
 * Cell merge analyzer for DOCX tables
 * Analyzes cell merging patterns for rowspan and colspan calculation
 */
@Slf4j
public class CellMergeAnalyzer {
    
    /**
     * Calculate rowspan for a cell based on vMerge attributes
     *
     * @param startRow         The starting row of the cell
     * @param logicalColumnIndex The logical column index of the cell
     * @param allRows          All rows in the table
     * @return The rowspan value (1 if no vertical merge)
     */
    private int calculateRowspanForCell(XWPFTableRow startRow, int logicalColumnIndex, List<XWPFTableRow> allRows) {
        int rowspan = 1;
        int startRowIndex = allRows.indexOf(startRow);
        
        // 查找后续行中对应逻辑列的单元格
        for (int nextRowIndex = startRowIndex + 1; nextRowIndex < allRows.size(); nextRowIndex++) {
            XWPFTableRow nextRow = allRows.get(nextRowIndex);
            XWPFTableCell nextCell = findCellAtLogicalColumn(nextRow, logicalColumnIndex);
            
            if (nextCell != null) {
                CTTcPr tcPr = nextCell.getCTTc().getTcPr();
                if (tcPr != null && tcPr.isSetVMerge()) {
                    CTVMerge vMerge = tcPr.getVMerge();
                    // 关键修复：处理 CONTINUE 状态
                    // 在 DOCX 中，CONTINUE 状态可能表现为：
                    // 1. vMerge.getVal() == STMerge.CONTINUE
                    // 2. vMerge.getVal() == null (这是常见情况)
                    if (vMerge.getVal() == STMerge.CONTINUE || vMerge.getVal() == null) {
                        rowspan++;
                        log.debug("Found CONTINUE cell (val={}) at row {} column {}, current rowspan: {}",
                                vMerge.getVal(), nextRowIndex, logicalColumnIndex, rowspan);
                    } else {
                        log.debug("Found vMerge cell but not CONTINUE at row {} column {}: {}",
                                nextRowIndex, logicalColumnIndex, vMerge.getVal());
                        break;
                    }
                } else {
                    log.debug("No vMerge found at row {} column {}, stopping rowspan calculation",
                            nextRowIndex, logicalColumnIndex);
                    break;
                }
            } else {
                log.debug("No cell found at row {} column {}, stopping rowspan calculation",
                        nextRowIndex, logicalColumnIndex);
                break;
            }
        }
        
        return rowspan;
    }
    
    /**
     * Calculate rowspan for a cell based on vMerge attributes
     * 
     * @param table    The table containing the cell
     * @param rowIndex Current row index
     * @param colIndex Current column index
     * @return The rowspan value (1 if no vertical merge)
     */
    public int calculateRowspan(XWPFTable table, int rowIndex, int colIndex) {
        if (table == null || rowIndex >= table.getRows().size()) {
            return 1;
        }
        
        XWPFTableRow row = table.getRows().get(rowIndex);
        if (colIndex >= row.getTableCells().size()) {
            return 1;
        }
        
        // 检查当前单元格是否有 vMerge=RESTART
        XWPFTableCell cell = row.getTableCells().get(colIndex);
        CTTcPr tcPr = cell.getCTTc().getTcPr();
        if (tcPr != null && tcPr.isSetVMerge()) {
            CTVMerge vMerge = tcPr.getVMerge();
            if (vMerge.getVal() == STMerge.RESTART) {
                // 使用逻辑列索引计算 rowspan
                int logicalColumnIndex = calculateLogicalColumnIndex(row, colIndex);
                return calculateRowspanForCell(row, logicalColumnIndex, table.getRows());
            }
        }
        
        return 1;
    }
    
    /**
     * Calculate the span of a merge starting at the given position
     */
    private int calculateMergeSpan(XWPFTable table, int startRow, int logicalColIndex) {
        int span = 1;
        
        // Look at subsequent rows to count how many have vMerge=continue at this
        // logical column
        for (int rowIdx = startRow + 1; rowIdx < table.getRows().size(); rowIdx++) {
            XWPFTableRow row = table.getRows().get(rowIdx);
            
            // 需要找到逻辑列索引对应的物理单元格
            XWPFTableCell targetCell = findCellAtLogicalColumn(row, logicalColIndex);
            
            if (targetCell == null) {
                log.debug("No cell found at logical column {} in row {}", logicalColIndex, rowIdx);
                break;
            }
            
            if (targetCell.getCTTc() == null ||
                    targetCell.getCTTc().getTcPr() == null ||
                    targetCell.getCTTc().getTcPr().getVMerge() == null) {
                log.debug("Cell at logical column {} in row {} has no vMerge", logicalColIndex, rowIdx);
                break;
            }
            
            STMerge.Enum vMerge = targetCell.getCTTc().getTcPr().getVMerge().getVal();
            log.debug("Checking cell at row {} logical column {}: vMerge={}, text='{}'", rowIdx, logicalColIndex,
                    vMerge, targetCell.getText());
            
            if (vMerge == STMerge.CONTINUE) {
                span++;
                log.debug("Found vMerge=CONTINUE at row {} logical column {}, span now: {}", rowIdx, logicalColIndex,
                        span);
            } else {
                log.debug("No more vMerge=CONTINUE found, stopping at row {}", rowIdx);
                break;
            }
        }
        
        return span;
    }
    
    /**
     * 在给定行中找到指定逻辑列索引的单元格
     */
    private XWPFTableCell findCellAtLogicalColumn(XWPFTableRow row, int targetLogicalColIndex) {
        int currentLogicalColIndex = 0;
        
        for (XWPFTableCell cell : row.getTableCells()) {
            // 如果当前逻辑列索引匹配目标，返回这个单元格
            if (currentLogicalColIndex == targetLogicalColIndex) {
                log.debug("Found cell at logical column {}: '{}'", targetLogicalColIndex, cell.getText());
                return cell;
            }
            
            // 计算这个单元格的colspan来更新逻辑列索引
            int colspan = 1;
            if (cell.getCTTc() != null && cell.getCTTc().getTcPr() != null) {
                colspan = getColspan(cell.getCTTc().getTcPr());
            }
            
            currentLogicalColIndex += colspan;
            
            // 如果逻辑列索引已经超过目标，说明目标列被之前的单元格跨越了
            if (currentLogicalColIndex > targetLogicalColIndex) {
                log.debug("Target logical column {} is spanned by previous cell: '{}'", targetLogicalColIndex,
                        cell.getText());
                return null;
            }
        }
        
        log.debug("No cell found at logical column {} (only {} logical columns in this row)", targetLogicalColIndex,
                currentLogicalColIndex);
        return null;
    }
    
    /**
     * Calculate the logical column index for a physical cell index
     * This accounts for cells with colspan that take up multiple logical columns
     * 
     * @param row              The table row
     * @param physicalColIndex Physical column index (0-based)
     * @return Logical column index
     */
    private int calculateLogicalColumnIndex(XWPFTableRow row, int physicalColIndex) {
        int logicalColIndex = 0;
        
        for (int i = 0; i < physicalColIndex && i < row.getTableCells().size(); i++) {
            XWPFTableCell cell = row.getTableCells().get(i);
            CTTcPr cellPr = cell.getCTTc().getTcPr();
            logicalColIndex += getColspan(cellPr);
        }
        
        return logicalColIndex;
    }
    
    /**
     * Get colspan for a cell based on hMerge attributes and gridSpan
     * 
     * @param cellPr Cell properties
     * @return The colspan value (1 if no horizontal merge)
     */
    public int getColspan(CTTcPr cellPr) {
        if (cellPr == null) {
            return 1;
        }
        
        // Check gridSpan first as it's more reliable for colspan
        if (cellPr.getGridSpan() != null) {
            int gridSpan = cellPr.getGridSpan().getVal().intValue();
            log.debug("Found gridSpan: {}", gridSpan);
            return gridSpan;
        }
        
        // Fallback to hMerge if available
        if (cellPr.getHMerge() != null) {
            // This would need more complex logic to calculate the actual span
            // For now, just return 1
            log.debug("Found hMerge but no gridSpan, returning 1");
            return 1;
        }
        
        return 1;
    }
    
    /**
     * Check if a cell should be skipped due to being part of a merge
     * 
     * @param cell The cell to check
     * @return true if the cell should be skipped
     */
    public boolean shouldSkipCell(XWPFTableCell cell) {
        if (cell.getCTTc() == null ||
                cell.getCTTc().getTcPr() == null ||
                cell.getCTTc().getTcPr().getVMerge() == null) {
            return false;
        }
        
        STMerge.Enum vMerge = cell.getCTTc().getTcPr().getVMerge().getVal();
        // 修复：CONTINUE 状态既可能是 STMerge.CONTINUE，也可能是 null
        boolean shouldSkip = (vMerge == STMerge.CONTINUE || vMerge == null);
        
        if (shouldSkip) {
            log.debug("Cell should be skipped due to vMerge=CONTINUE or null (text: '{}')",
                    cell.getText().trim());
        }
        
        return shouldSkip;
    }
}