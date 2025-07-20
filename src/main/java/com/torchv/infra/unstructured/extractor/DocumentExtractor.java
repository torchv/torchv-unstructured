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


package com.torchv.infra.unstructured.extractor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.extractor.embedded.EmbeddedDocumentExtractor;
import com.torchv.infra.unstructured.handler.markdown.MarkdownContentHandler;
import com.torchv.infra.unstructured.handler.markdown.ExtendedMarkdownContentHandler;
import com.torchv.infra.unstructured.util.WordMarkdownUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ParseRecord;
import org.apache.tika.parser.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/8/6 19:49
 * @since torchv-unstructured
 */
@Slf4j
public class DocumentExtractor {
    
    /**
     * 基于Apache Tika项目，自动获取内容预览
     * @param source 源文件
     * @param line 多少行
     * @return 预览内容
     */
    public static String getPreView(File source, int line) {
        int realLine = 4000;
        if (line < realLine) {
            realLine = line;
        }
        File targetFile = null;
        File clearFile = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        List<String> lines = new ArrayList<>();
        try {
            targetFile = FileUtil.createTempFile("tika_custom", ".md", true);
            fis = new FileInputStream(source);
            fos = new FileOutputStream(targetFile);
            // 创建 Parser 对象
            Parser parser = new AutoDetectParser();
            // 创建 Metadata 对象
            Metadata metadata = new Metadata();
            MarkdownContentHandler toHTMLContentHandler = new MarkdownContentHandler(fos, "utf-8");
            ExtendedMarkdownContentHandler textHandler =
                    new ExtendedMarkdownContentHandler(toHTMLContentHandler, metadata);
            // 创建 ContentHandler 对象
            // ContentHandler textHandler = new XHTMLContentHandler(new BodyContentHandler(),new Metadata());
            ParseRecord parseRecord = new ParseRecord();
            // 创建 ParseContext 对象
            ParseContext parseContext = new ParseContext();
            parseContext.set(ParseRecord.class, parseRecord);
            EmbeddedDocumentExtractor embeddedDocumentExtractor = new EmbeddedDocumentExtractor();
            parseContext.set(org.apache.tika.extractor.EmbeddedDocumentExtractor.class, embeddedDocumentExtractor);
            parseContext.set(Parser.class, parser);
            // 解析 DOCX 文件
            parser.parse(fis, textHandler, metadata, parseContext);
            clearFile = WordMarkdownUtils.clearMarkdown(targetFile, false);
            BufferedReader reader = null;
            try {
                reader = FileUtil.getReader(clearFile, StandardCharsets.UTF_8);
                int maxLine = 0;
                for (String lineStr : IoUtil.lineIter(reader)) {
                    lines.add(lineStr);
                    maxLine++;
                    if (maxLine > realLine) {
                        break;
                    }
                }
            } finally {
                IoUtil.close(reader);
            }
            
            return CollUtil.join(lines, "\n");
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        } finally {
            log.info("close.");
            IoUtil.close(fos);
            IoUtil.close(fis);
            FileUtil.del(targetFile);
            FileUtil.del(clearFile);
        }
        return StrUtil.EMPTY;
    }
}
