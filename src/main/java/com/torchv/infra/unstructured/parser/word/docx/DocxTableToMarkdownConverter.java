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


package com.torchv.infra.unstructured.parser.word.docx;

import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.parser.word.model.data.DocumentTableResult;
import lombok.AllArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 将Word的docx中的表格数据转换为Markdown语法
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/4/19 14:51
 * @since torchv_server v1.7.2
 */
@AllArgsConstructor
public class DocxTableToMarkdownConverter implements Supplier<String> {
    
    /**
     * 原始文档对象，用来获取图片数据
     */
    final XWPFDocument document;
    /**
     * 表格对象
     */
    final XWPFTable table;
    
    /**
     * 判断是否为嵌入表格
     * @return True-有嵌入表格，False-简单表格
     */
    private boolean embedTable() {
        boolean embedTable = false;
        List<XWPFTableRow> rows = this.table.getRows();
        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> cells = row.getTableCells();
            for (XWPFTableCell cell : cells) {
                List<IBodyElement> cellElements = cell.getBodyElements();
                for (IBodyElement cellElement : cellElements) {
                    if (cellElement instanceof XWPFTable cellTable) {
                        embedTable = true;
                        break;
                    }
                }
            }
            if (embedTable) {
                break;
            }
        }
        return embedTable;
    }
    
    @Override
    public String get() {
        StringBuilder tableBuilder = new StringBuilder();
        // 判断当前表格中，Cell是否存在嵌入表格
        if (this.embedTable()) {
            tableBuilder.append(html());
        } else {
            tableBuilder.append(markdown());
        }
        return tableBuilder.toString();
    }
    
    public String markdown() {
        DocumentTableResult tableResult = new DocumentTableResult();
        // 默认是简单的表格，直接使用markdown
        tableResult.setEmbed(false);
        StringBuilder tableBuilder = new StringBuilder();
        List<XWPFTableRow> rows = table.getRows();
        List<String> rowValues = new ArrayList<>();
        int rowNum = 0;
        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> cells = row.getTableCells();
            List<String> cellValues = new ArrayList<>();
            for (XWPFTableCell cell : cells) {
                if (cell != null) {
                    List<IBodyElement> cellElements = cell.getBodyElements();
                    for (IBodyElement cellElement : cellElements) {
                        if (cellElement instanceof XWPFParagraph paragraph) {
                            paragraph.getRuns().get(0).getEmbeddedPictures();
                            String paragraphText = paragraph.getText();
                            cellValues.add(paragraphText.replaceAll("\n", ""));
                        }
                    }
                } else {
                    // 当前cell是空的
                    cellValues.add(StrUtil.EMPTY);
                }
            }
            rowValues.add("|" + String.join("|", cellValues) + "|");
        }
        return "";
    }
    
    /**
     * 如果是嵌入的表格，那么转换为html
     * @return html格式的表格
     */
    public String html() {
        return "";
    }
    
}
