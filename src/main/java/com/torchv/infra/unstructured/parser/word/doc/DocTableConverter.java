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


package com.torchv.infra.unstructured.parser.word.doc;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Doc格式中table的转换类
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/4/14 20:33
 * @since torchv_server v1.7.1
 */
@AllArgsConstructor
public class DocTableConverter implements Supplier<String> {
    
    final Table table;
    
    /**
     * 表格转换为Markdown格式
     * @return Markdown格式的表格
     */
    @Override
    public String get() {
        StringBuilder tableBuilder = new StringBuilder();
        for (int j = 0; j < table.numRows(); j++) {
            TableRow tableRow = table.getRow(j);
            int cellNum = tableRow.numCells();
            List<String> cellValues = new ArrayList<>();
            for (int k = 0; k < cellNum; k++) {
                TableCell tableCell = tableRow.getCell(k);
                String cellText = tableCell.text();
                cellValues.add(cellText.replaceAll("\r", ","));
            }
            tableBuilder.append("|");
            tableBuilder.append(StrUtil.join("|", cellValues));
            tableBuilder.append("|");
            tableBuilder.append("\n");
            if (j == 0) {
                List<String> headerValues = new ArrayList<>();
                for (int k = 0; k < cellNum; k++) {
                    headerValues.add("----");
                }
                tableBuilder.append("|");
                tableBuilder.append(StrUtil.join("|", headerValues));
                tableBuilder.append("|");
                tableBuilder.append("\n");
            }
        }
        return tableBuilder.toString();
    }
}
