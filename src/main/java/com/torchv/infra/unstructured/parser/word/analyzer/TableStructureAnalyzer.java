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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 表格结构分析器
 * 用于分析复杂的表格结构和合并情况
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class TableStructureAnalyzer {
    
    private final CellMergeAnalyzer cellMergeAnalyzer;
    
    public TableStructureAnalyzer() {
        this.cellMergeAnalyzer = new CellMergeAnalyzer();
    }
    
    public TableStructureAnalyzer(CellMergeAnalyzer cellMergeAnalyzer) {
        this.cellMergeAnalyzer = cellMergeAnalyzer;
    }
    
    /**
     * 分析表格结构
     */
    public TableStructure analyzeTable(XWPFTable table) {
        TableStructure structure = new TableStructure();
        
        // 基本信息
        structure.setRowCount(table.getRows().size());
        structure.setColumnCount(calculateMaxColumns(table));
        
        // 分析每个单元格
        for (int rowIndex = 0; rowIndex < table.getRows().size(); rowIndex++) {
            XWPFTableRow row = table.getRows().get(rowIndex);
            
            for (int cellIndex = 0; cellIndex < row.getTableCells().size(); cellIndex++) {
                XWPFTableCell cell = row.getTableCells().get(cellIndex);
                
                CellStructure cellStructure = analyzeCellStructure(table, rowIndex, cellIndex, cell);
                structure.addCell(rowIndex, cellIndex, cellStructure);
            }
        }
        
        return structure;
    }
    
    /**
     * 分析单元格结构
     */
    private CellStructure analyzeCellStructure(XWPFTable table, int rowIndex, int cellIndex, XWPFTableCell cell) {
        CellStructure cellStructure = new CellStructure();
        
        cellStructure.setRowIndex(rowIndex);
        cellStructure.setCellIndex(cellIndex);
        cellStructure.setText(cell.getText());
        
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        
        // 分析合并信息
        cellStructure.setColspan(cellMergeAnalyzer.getColspan(cellPr));
        cellStructure.setRowspan(cellMergeAnalyzer.calculateRowspan(table, rowIndex, cellIndex));
        cellStructure.setShouldSkip(cellMergeAnalyzer.shouldSkipCell(cell));
        cellStructure.setMergeStart(cellMergeAnalyzer.isMergeStartCell(cell));
        cellStructure.setMerged(cellMergeAnalyzer.isMergedCell(cell));
        
        return cellStructure;
    }
    
    /**
     * 计算表格最大列数
     */
    private int calculateMaxColumns(XWPFTable table) {
        int maxColumns = 0;
        for (XWPFTableRow row : table.getRows()) {
            maxColumns = Math.max(maxColumns, row.getTableCells().size());
        }
        return maxColumns;
    }
    
    /**
     * 表格结构信息
     */
    public static class TableStructure {
        
        private int rowCount;
        private int columnCount;
        private final Map<String, CellStructure> cells = new HashMap<>();
        
        public void addCell(int rowIndex, int cellIndex, CellStructure cellStructure) {
            cells.put(rowIndex + "," + cellIndex, cellStructure);
        }
        
        public CellStructure getCell(int rowIndex, int cellIndex) {
            return cells.get(rowIndex + "," + cellIndex);
        }
        
        public Collection<CellStructure> getAllCells() {
            return cells.values();
        }
        
        public int getRowCount() {
            return rowCount;
        }
        
        public void setRowCount(int rowCount) {
            this.rowCount = rowCount;
        }
        
        public int getColumnCount() {
            return columnCount;
        }
        
        public void setColumnCount(int columnCount) {
            this.columnCount = columnCount;
        }
        
        public Map<String, CellStructure> getCells() {
            return cells;
        }
        
        /**
         * 获取有合并的单元格数量
         */
        public int getMergedCellCount() {
            return (int) cells.values().stream()
                    .filter(cell -> cell.getColspan() > 1 || cell.getRowspan() > 1)
                    .count();
        }
        
        /**
         * 获取跳过的单元格数量
         */
        public int getSkippedCellCount() {
            return (int) cells.values().stream()
                    .filter(CellStructure::isShouldSkip)
                    .count();
        }
        
        @Override
        public String toString() {
            return String.format("TableStructure{rows=%d, cols=%d, cells=%d, merged=%d, skipped=%d}",
                    rowCount, columnCount, cells.size(), getMergedCellCount(), getSkippedCellCount());
        }
    }
    
    /**
     * 单元格结构信息
     */
    public static class CellStructure {
        
        private int rowIndex;
        private int cellIndex;
        private String text;
        private int colspan = 1;
        private int rowspan = 1;
        private boolean shouldSkip = false;
        private boolean isMergeStart = false;
        private boolean isMerged = false;
        
        public int getRowIndex() {
            return rowIndex;
        }
        
        public void setRowIndex(int rowIndex) {
            this.rowIndex = rowIndex;
        }
        
        public int getCellIndex() {
            return cellIndex;
        }
        
        public void setCellIndex(int cellIndex) {
            this.cellIndex = cellIndex;
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public int getColspan() {
            return colspan;
        }
        
        public void setColspan(int colspan) {
            this.colspan = colspan;
        }
        
        public int getRowspan() {
            return rowspan;
        }
        
        public void setRowspan(int rowspan) {
            this.rowspan = rowspan;
        }
        
        public boolean isShouldSkip() {
            return shouldSkip;
        }
        
        public void setShouldSkip(boolean shouldSkip) {
            this.shouldSkip = shouldSkip;
        }
        
        public boolean isMergeStart() {
            return isMergeStart;
        }
        
        public void setMergeStart(boolean mergeStart) {
            isMergeStart = mergeStart;
        }
        
        public boolean isMerged() {
            return isMerged;
        }
        
        public void setMerged(boolean merged) {
            isMerged = merged;
        }
        
        @Override
        public String toString() {
            return String.format(
                    "CellStructure{[%d,%d] text='%s', colspan=%d, rowspan=%d, skip=%s, mergeStart=%s, merged=%s}",
                    rowIndex, cellIndex, text, colspan, rowspan, shouldSkip, isMergeStart, isMerged);
        }
    }
}
