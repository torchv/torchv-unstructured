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


package com.torchv.infra.unstructured.handler.table;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DOC格式表格解析器 - 专门处理DOC格式的表格合并单元格
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
@Slf4j
public class DocumentTableParser {
    
    /**
     * 解析DOC文档中的所有表格为HTML
     * 
     * @param document HWPFDocument实例
     * @return HTML表格字符串列表
     */
    public static List<String> parseAllTablesToHtml(HWPFDocument document) {
        List<String> htmlTables = new ArrayList<>();
        Set<Integer> processedTablePositions = new HashSet<>();
        
        try {
            Range range = document.getRange();
            int numParagraphs = range.numParagraphs();
            log.debug("DOC文档中共有{}个段落", numParagraphs);
            
            for (int i = 0; i < numParagraphs; i++) {
                try {
                    if (range.getParagraph(i).isInTable()) {
                        Table table = range.getTable(range.getParagraph(i));
                        int tableStartOffset = table.getStartOffset();
                        
                        if (!processedTablePositions.contains(tableStartOffset)) {
                            processedTablePositions.add(tableStartOffset);
                            String htmlTable = parseTableToHtml(table, htmlTables.size());
                            if (!htmlTable.isEmpty()) {
                                htmlTables.add(htmlTable);
                                log.debug("成功解析表格#{}, 起始位置: {}", htmlTables.size(), tableStartOffset);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("跳过段落{}: {}", i, e.getMessage());
                }
            }
            
            log.debug("DOC文档中共解析到{}个表格", htmlTables.size());
            
        } catch (Exception e) {
            log.error("解析DOC表格时发生错误: {}", e.getMessage(), e);
        }
        
        return htmlTables;
    }
    
    /**
     * 解析单个表格为HTML - 基于DOC格式的真实结构特征
     * 
     * @param table      表格实例
     * @param tableIndex 表格索引
     * @return HTML字符串
     */
    public static String parseTableToHtml(Table table, int tableIndex) {
        if (table == null) {
            return "";
        }
        
        try {
            StringBuilder html = new StringBuilder();
            html.append("<table border=\"1\" style=\"border-collapse: collapse;\">\n");
            
            int numRows = table.numRows();
            log.debug("表格#{}共有{}行", tableIndex, numRows);
            
            // 分析表格的真实列结构
            int maxCols = analyzeTableColumns(table);
            log.debug("表格#{}最大列数: {}", tableIndex, maxCols);
            
            // 创建原始表格数据矩阵
            String[][] cellContents = new String[numRows][];
            int[] rowCellCounts = new int[numRows];
            // 提取所有单元格内容
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                TableRow row = table.getRow(rowIndex);
                int numCells = row.numCells();
                rowCellCounts[rowIndex] = numCells;
                cellContents[rowIndex] = new String[numCells];
                
                log.debug("第{}行共有{}个单元格", rowIndex, numCells);
                
                for (int cellIndex = 0; cellIndex < numCells; cellIndex++) {
                    TableCell cell = row.getCell(cellIndex);
                    String cellText = extractCellText(cell);
                    cellContents[rowIndex][cellIndex] = cellText;
                    log.debug("  原始单元格[{},{}]: '{}' (长度:{})", rowIndex, cellIndex, cellText, cellText.length());
                }
            }
            
            // 生成HTML - 不使用复杂的矩阵占位，而是根据数据特征判断
            for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
                html.append("  <tr>\n");
                
                int numCells = rowCellCounts[rowIndex];
                
                for (int cellIndex = 0; cellIndex < numCells; cellIndex++) {
                    String cellText = cellContents[rowIndex][cellIndex];
                    
                    // 基于表格结构的通用合并检测
                    MergeInfo mergeInfo = detectMergeByStructure(table, rowIndex, cellIndex, cellText,
                            cellContents, rowCellCounts, maxCols);
                    
                    // 跳过被rowspan占用的空单元格
                    if (shouldSkipCellByStructure(rowIndex, cellIndex, cellText, cellContents, rowCellCounts)) {
                        log.debug("    跳过空单元格[{},{}]: '{}'", rowIndex, cellIndex, cellText);
                        continue;
                    }
                    
                    // 生成单元格HTML
                    html.append("    <td");
                    
                    if (mergeInfo.colspan > 1) {
                        html.append(" colspan=\"").append(mergeInfo.colspan).append("\"");
                    }
                    if (mergeInfo.rowspan > 1) {
                        html.append(" rowspan=\"").append(mergeInfo.rowspan).append("\"");
                    }
                    
                    html.append(" style=\"padding: 4px; border: 1px solid #000;\">");
                    html.append(cellText.isEmpty() ? "&nbsp;" : escapeHtml(cellText));
                    html.append("</td>\n");
                    
                    log.debug("    输出单元格[{},{}]='{}' -> rowspan={}, colspan={}",
                            rowIndex, cellIndex, cellText, mergeInfo.rowspan, mergeInfo.colspan);
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
     * 分析表格的列结构
     */
    private static int analyzeTableColumns(Table table) {
        int maxCols = 0;
        for (int i = 0; i < table.numRows(); i++) {
            maxCols = Math.max(maxCols, table.getRow(i).numCells());
        }
        return maxCols;
    }
    
    /**
     * 基于DOC表格结构检测合并单元格 - 通用算法
     */
    private static MergeInfo detectMergeByStructure(Table table, int rowIndex, int cellIndex, String cellText,
                                                    String[][] cellContents, int[] rowCellCounts, int maxCols) {
        int colspan = 1;
        int rowspan = 1;
        
        try {
            // 1. 检测列合并 - 基于行单元格数量差异
            colspan = detectColspanByStructure(rowIndex, cellIndex, cellContents, rowCellCounts, maxCols);
            
            // 2. 检测行合并 - 基于相邻行的空单元格模式
            rowspan = detectRowspanByStructure(rowIndex, cellIndex, cellText, cellContents, rowCellCounts);
            
        } catch (Exception e) {
            log.debug("检测合并单元格时发生错误: {}", e.getMessage());
        }
        
        log.debug("    单元格[{},{}]='{}' -> colspan={}, rowspan={}",
                rowIndex, cellIndex, cellText, colspan, rowspan);
        
        return new MergeInfo(colspan, rowspan);
    }
    
    /**
     * 基于表格结构检测列合并 - 通用算法
     * 核心思想：分析行内单元格分布，推断当前单元格应该占用多少列
     */
    private static int detectColspanByStructure(int rowIndex, int cellIndex, String[][] cellContents,
                                                int[] rowCellCounts, int maxCols) {
        
        int currentRowCells = rowCellCounts[rowIndex];
        
        // 如果当前行的单元格数量等于或大于最大列数，无列合并
        if (currentRowCells >= maxCols) {
            return 1;
        }
        
        // 基于单元格数量和位置推断colspan
        // 如果当前行只有1个单元格，那么它应该占满整行
        if (currentRowCells == 1) {
            return maxCols;
        }
        
        // 如果当前行有2个单元格，且最大列数是3，那么可能的分布是：
        // - 第一个单元格 colspan=2，第二个单元格 colspan=1
        // - 第一个单元格 colspan=1，第二个单元格 colspan=2
        // 通过分析上下行的结构来判断
        
        // 简化策略：均匀分配列数，但考虑剩余空间
        int averageColspan = maxCols / currentRowCells;
        int remainingCols = maxCols % currentRowCells;
        
        // 如果是最后一个单元格，可能需要占用剩余的列
        if (cellIndex == currentRowCells - 1 && remainingCols > 0) {
            return averageColspan + remainingCols;
        }
        
        return averageColspan > 0 ? averageColspan : 1;
    }
    
    /**
     * 基于表格结构检测行合并 - 通用算法
     * 核心思想：检查向下相邻行的对应位置是否为空或被占用，推断rowspan
     */
    private static int detectRowspanByStructure(int rowIndex, int cellIndex, String cellText,
                                                String[][] cellContents, int[] rowCellCounts) {
        
        // 如果当前单元格为空，不进行行合并
        if (cellText.trim().isEmpty()) {
            return 1;
        }
        
        int rowspan = 1;
        
        // 检查从下一行开始，相同列位置的单元格状态
        for (int nextRow = rowIndex + 1; nextRow < cellContents.length; nextRow++) {
            // 检查该行是否有足够的单元格（考虑列合并的影响）
            if (cellIndex >= rowCellCounts[nextRow]) {
                // 下一行的单元格数量不够，可能是因为上面有rowspan
                // 但我们需要更谨慎地判断
                break;
            }
            
            String nextCellText = cellContents[nextRow][cellIndex];
            
            // 如果下一行对应位置为空，可能是rowspan的延续
            if (nextCellText.trim().isEmpty()) {
                // 进一步检查：这个空单元格是否真的被上面的rowspan占用
                // 看看这一行其他位置是否有内容
                boolean hasContentInRow = false;
                for (int i = 0; i < rowCellCounts[nextRow]; i++) {
                    if (!cellContents[nextRow][i].trim().isEmpty()) {
                        hasContentInRow = true;
                        break;
                    }
                }
                
                // 如果这一行确实有其他内容，那么当前空位置很可能是被rowspan占用
                if (hasContentInRow) {
                    rowspan++;
                } else {
                    // 如果整行都是空的，可能不是rowspan，而是空行
                    break;
                }
            } else {
                // 遇到非空单元格，rowspan结束
                break;
            }
        }
        
        return rowspan;
    }
    
    /**
     * 基于表格结构判断是否跳过单元格 - 通用算法
     * 核心思想：识别被rowspan占用的空单元格位置，避免重复输出
     */
    private static boolean shouldSkipCellByStructure(int rowIndex, int cellIndex, String cellText,
                                                     String[][] cellContents, int[] rowCellCounts) {
        
        // 如果单元格有内容，不跳过
        if (!cellText.trim().isEmpty()) {
            return false;
        }
        
        // 向上查找，看是否有单元格的rowspan覆盖到当前位置
        for (int prevRow = rowIndex - 1; prevRow >= 0; prevRow--) {
            // 检查前面的行是否有该位置的单元格
            if (cellIndex < rowCellCounts[prevRow]) {
                String prevCellText = cellContents[prevRow][cellIndex];
                
                // 如果前面的单元格有内容，检查它的rowspan是否覆盖到当前位置
                if (!prevCellText.trim().isEmpty()) {
                    // 计算该单元格的rowspan（重新计算以确保一致性）
                    int calculatedRowspan = calculateRowspanFromPosition(prevRow, cellIndex, cellContents,
                            rowCellCounts);
                    
                    // 如果rowspan覆盖到当前行，则跳过
                    if (prevRow + calculatedRowspan > rowIndex) {
                        return true;
                    }
                }
            }
        }
        
        return false; // 不跳过
    }
    
    /**
     * 从指定位置计算rowspan值 - 辅助方法
     */
    private static int calculateRowspanFromPosition(int startRow, int cellIndex, String[][] cellContents,
                                                    int[] rowCellCounts) {
        String startCellText = cellContents[startRow][cellIndex];
        if (startCellText.trim().isEmpty()) {
            return 1;
        }
        
        int rowspan = 1;
        for (int nextRow = startRow + 1; nextRow < cellContents.length; nextRow++) {
            if (cellIndex >= rowCellCounts[nextRow]) {
                break;
            }
            
            String nextCellText = cellContents[nextRow][cellIndex];
            if (nextCellText.trim().isEmpty()) {
                // 检查这一行是否有其他内容
                boolean hasContentInRow = false;
                for (int i = 0; i < rowCellCounts[nextRow]; i++) {
                    if (!cellContents[nextRow][i].trim().isEmpty()) {
                        hasContentInRow = true;
                        break;
                    }
                }
                
                if (hasContentInRow) {
                    rowspan++;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        return rowspan;
    }
    
    /**
     * 提取单元格文本内容
     */
    private static String extractCellText(TableCell cell) {
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
    
    /**
     * HTML转义
     */
    private static String escapeHtml(String text) {
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
     * 合并信息类
     */
    static class MergeInfo {
        
        public final int colspan;
        public final int rowspan;
        
        public MergeInfo(int colspan, int rowspan) {
            this.colspan = colspan;
            this.rowspan = rowspan;
        }
    }
    
}
