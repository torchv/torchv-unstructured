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

/**
 * 单元格合并分析器
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class CellMergeAnalyzer {
    
    /**
     * 检查单元格是否应该被跳过（被上方单元格合并）
     */
    public boolean shouldSkipCell(XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        if (cellPr != null && cellPr.isSetVMerge()) {
            String vMergeVal = String.valueOf(cellPr.getVMerge().getVal());
            // 如果是 "continue" 或 "null"，说明这个单元格被上方单元格合并了
            return "continue".equals(vMergeVal) || "null".equals(vMergeVal);
        }
        return false;
    }
    
    /**
     * 获取单元格的列跨越数
     */
    public int getColspan(CTTcPr cellPr) {
        if (cellPr != null && cellPr.isSetGridSpan()) {
            return cellPr.getGridSpan().getVal().intValue();
        }
        return 1;
    }
    
    /**
     * 计算单元格的行跨越数
     */
    public int calculateRowspan(XWPFTable table, int startRowIndex, int cellIndex) {
        XWPFTableRow startRow = table.getRows().get(startRowIndex);
        if (cellIndex >= startRow.getTableCells().size()) {
            return 1;
        }
        
        XWPFTableCell startCell = startRow.getTableCells().get(cellIndex);
        CTTcPr startCellPr = startCell.getCTTc().getTcPr();
        
        // 检查是否有 vMerge 属性
        if (startCellPr == null || !startCellPr.isSetVMerge()) {
            return 1;
        }
        
        String vMergeVal = String.valueOf(startCellPr.getVMerge().getVal());
        log.debug("单元格 [{}][{}] vMerge值: {}", startRowIndex, cellIndex, vMergeVal);
        
        // 如果不是 "restart"，说明不是合并的起始单元格
        if (!"restart".equals(vMergeVal)) {
            return 1;
        }
        
        // 向下查找连续的 "continue" 单元格
        int rowspan = 1;
        for (int rowIndex = startRowIndex + 1; rowIndex < table.getRows().size(); rowIndex++) {
            XWPFTableRow row = table.getRows().get(rowIndex);
            if (cellIndex >= row.getTableCells().size()) {
                break;
            }
            
            XWPFTableCell cell = row.getTableCells().get(cellIndex);
            CTTcPr cellPr = cell.getCTTc().getTcPr();
            
            if (cellPr == null || !cellPr.isSetVMerge()) {
                break;
            }
            
            String mergeVal = String.valueOf(cellPr.getVMerge().getVal());
            log.debug("检查单元格 [{}][{}] vMerge值: {}", rowIndex, cellIndex, mergeVal);
            
            if ("continue".equals(mergeVal) || "null".equals(mergeVal)) {
                rowspan++;
            } else {
                break;
            }
        }
        
        return rowspan;
    }
    
    /**
     * 检查单元格是否为合并的起始单元格
     */
    public boolean isMergeStartCell(XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        if (cellPr != null && cellPr.isSetVMerge()) {
            String vMergeVal = String.valueOf(cellPr.getVMerge().getVal());
            return "restart".equals(vMergeVal);
        }
        return false;
    }
    
    /**
     * 检查单元格是否为被合并的单元格
     */
    public boolean isMergedCell(XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        if (cellPr != null && cellPr.isSetVMerge()) {
            String vMergeVal = String.valueOf(cellPr.getVMerge().getVal());
            return "continue".equals(vMergeVal) || "null".equals(vMergeVal);
        }
        return false;
    }
    
    /**
     * 检查单元格是否有列合并
     */
    public boolean hasColspan(XWPFTableCell cell) {
        CTTcPr cellPr = cell.getCTTc().getTcPr();
        return cellPr != null && cellPr.isSetGridSpan() && cellPr.getGridSpan().getVal().intValue() > 1;
    }
}
