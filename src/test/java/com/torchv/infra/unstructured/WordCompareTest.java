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


package com.torchv.infra.unstructured;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2025/8/9 18:30
 * @since torchv-unstructured
 */
@Slf4j
public class WordCompareTest {
    
    @Test
    public void test_parse_doc() {
        String filePath = "src/test/resources/docs/test.doc";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>$Title$</title>\n" +
                "</head>\n" +
                "<body>").append("\n");
        // 仅提取Word文档中的表格
        List<String> tables = UnstructuredParser.extractTables(filePath);
        for (int i = 0; i < tables.size(); i++) {
            System.out.println("表格 " + (i + 1) + ":");
            System.out.println(tables.get(i));
            stringBuilder.append("<div class=\"table\" id=\"table-").append(i + 1).append("\">")
                    .append("<h2>表格 ").append(i + 1).append("</h2>\n")
                    .append(tables.get(i))
                    .append("</div>\n");
        }
        stringBuilder.append("\n" +
                "</body>\n" +
                "</html>");
        FileUtil.writeString(stringBuilder.toString(), new File("src/test/resources/docs/1001-doc.html"), "UTF-8");
        
    }
    
    @Test
    public void test_parse_docx() {
        String filePath = "src/test/resources/docs/test.docx";
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>$Title$</title>\n" +
                "</head>\n" +
                "<body>").append("\n");
        // 仅提取Word文档中的表格
        List<String> tables = UnstructuredParser.extractTables(filePath);
        for (int i = 0; i < tables.size(); i++) {
            System.out.println("表格 " + (i + 1) + ":");
            System.out.println(tables.get(i));
            stringBuilder.append("<div class=\"table\" id=\"table-").append(i + 1).append("\">")
                    .append("<h2>表格 ").append(i + 1).append("</h2>\n")
                    .append(tables.get(i))
                    .append("</div>\n");
        }
        stringBuilder.append("\n" +
                "</body>\n" +
                "</html>");
        FileUtil.writeString(stringBuilder.toString(), new File("src/test/resources/docs/1001-docx.html"), "UTF-8");
    }
}
