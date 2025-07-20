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


package com.torchv.infra.unstructured.parser.word.builder;

import com.torchv.infra.unstructured.parser.word.model.CellInfo;

/**
 * HTML 表格构建器
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv_server
 */
public class HtmlTableBuilder {
    
    private final String tableStyle;
    private final String cellStyle;
    
    public HtmlTableBuilder() {
        this.tableStyle = "border=\"1\" style=\"border-collapse: collapse;\"";
        this.cellStyle = "style=\"padding: 4px; border: 1px solid #000;\"";
    }
    
    public HtmlTableBuilder(String tableStyle, String cellStyle) {
        this.tableStyle = tableStyle;
        this.cellStyle = cellStyle;
    }
    
    /**
     * 开始表格标签
     */
    public String startTable() {
        return String.format("<table %s>\n", tableStyle);
    }
    
    /**
     * 结束表格标签
     */
    public String endTable() {
        return "</table>\n";
    }
    
    /**
     * 开始行标签
     */
    public String startRow() {
        return "  <tr>\n";
    }
    
    /**
     * 结束行标签
     */
    public String endRow() {
        return "  </tr>\n";
    }
    
    /**
     * 构建单元格 HTML
     */
    public String buildCell(CellInfo cellInfo) {
        StringBuilder html = new StringBuilder();
        html.append("    <td");
        
        // 添加 colspan 属性
        if (cellInfo.getColspan() > 1) {
            html.append(" colspan=\"").append(cellInfo.getColspan()).append("\"");
        }
        
        // 添加 rowspan 属性
        if (cellInfo.getRowspan() > 1) {
            html.append(" rowspan=\"").append(cellInfo.getRowspan()).append("\"");
        }
        
        // 添加样式
        html.append(" ").append(cellStyle);
        html.append(">");
        
        // 添加内容
        html.append(cellInfo.getText());
        
        html.append("</td>\n");
        return html.toString();
    }
    
    /**
     * 构建简单单元格 HTML（无合并）
     */
    public String buildSimpleCell(String text) {
        return buildCell(new CellInfo(text != null && !text.trim().isEmpty() ? text.trim() : "&nbsp;", 1, 1));
    }
    
    /**
     * 构建带合并的单元格 HTML
     */
    public String buildMergedCell(String text, int colspan, int rowspan) {
        String cellText = text != null && !text.trim().isEmpty() ? text.trim() : "&nbsp;";
        return buildCell(new CellInfo(cellText, colspan, rowspan));
    }
    
    /**
     * 获取表格样式
     */
    public String getTableStyle() {
        return tableStyle;
    }
    
    /**
     * 获取单元格样式
     */
    public String getCellStyle() {
        return cellStyle;
    }
}
