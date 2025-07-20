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

import com.torchv.infra.unstructured.core.DocumentResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.List;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2025/7/19 23:11
 * @since torchv-unstructured
 */
@Slf4j
public class WordParserTest {
    
    @Test
    public void test_word() {
        String filePath = "src/test/resources/docs/test.docx";
        String content = UnstructuredParser.toMarkdown(filePath);
        log.info(content);
    }
    
    @Test
    public void test_parse() {
        log.info("开始解析Word文档");
        String filePath = "src/test/resources/docs/test.docx";
        DocumentResult content = UnstructuredParser.toStructuredResult(filePath);
        log.info(content.getContent());
    }
    
    @Test
    public void test_parse_image() {
        String filePath = "src/test/resources/docs/deploy_llm.docx";
        DocumentResult content = UnstructuredParser.toStructuredResult(filePath);
        log.info(content.getContent());

        log.info("文档标题: {}", content.getFileName());
        content.getImages().forEach(image -> {
            log.info("图片名称: {}, 图片路径: {}", image.getName(), image.getData());
        });
    }
    
    @Test
    public void test_parse_image1() {
        String filePath = "src/test/resources/docs/deploy_llm.docx";
        String content = UnstructuredParser.toMarkdown(filePath);
        log.info(content);
    }
    
    @Test
    public void test_parse_image2() {
        String filePath = "src/test/resources/docs/test.doc";
        String content = UnstructuredParser.toMarkdown(filePath);
        log.info(content);
    }

    @Test
    public void test_parse_3() {
        String filePath = "src/test/resources/docs/test.docx";
        // 仅提取Word文档中的表格
        List<String> tables = UnstructuredParser.extractTables(filePath);
        for (int i = 0; i < tables.size(); i++) {
            System.out.println("表格 " + (i + 1) + ":");
            System.out.println(tables.get(i));
        }

        log.info("表格数量: {}", tables.size());
        // 获取结构化结果，提供更多控制

    }
}
