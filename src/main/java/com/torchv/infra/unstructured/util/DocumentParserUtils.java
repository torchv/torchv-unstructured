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


package com.torchv.infra.unstructured.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.handler.markdown.DocMarkdownTableHandler;
import com.torchv.infra.unstructured.handler.markdown.DocxMarkdownTableHandler;
import com.torchv.infra.unstructured.handler.markdown.MarkdownContentHandler;
import com.torchv.infra.unstructured.handler.markdown.ExtendedMarkdownContentHandler;
import com.torchv.infra.unstructured.parser.word.WordTableParser;
import com.torchv.infra.unstructured.parser.word.doc.DocImageExtractor;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import com.torchv.infra.unstructured.parser.word.docx.DocxImageExtractor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 09:44
 * @since torchv_server v1.7.3
 */
@Slf4j
public class DocumentParserUtils {
    
    /**
     * 提取Docx文件中的图片
     *
     * @param docXFile docx文件
     * @return 图片列表
     */
    public static List<DocumentImage> extractPictures(File docXFile) {
        List<DocumentImage> documentImages = new ArrayList<>();
        FileMagic fileMagic = FileMagicUtils.checkMagic(docXFile);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(docXFile);
            // 判断是否为unknown(有些情况下识别失败)，如果是unknown，那么用文件后缀名来判断
            if (fileMagic == FileMagic.UNKNOWN) {
                String suffix = FileUtil.getSuffix(docXFile.getName());
                if (StrUtil.equalsIgnoreCase(suffix, "docx")) {
                    fileMagic = FileMagic.OOXML;
                } else if (StrUtil.equalsIgnoreCase(suffix, "doc")) {
                    fileMagic = FileMagic.OLE2;
                }
            }
            log.info("fileMagic:{},path:{}", fileMagic, docXFile.getAbsolutePath());
            switch (fileMagic) {
                case OOXML -> {
                    // docx格式
                    XWPFDocument document = new XWPFDocument(fis);
                    List<XWPFPictureData> pictureData = document.getAllPictures();
                    // name: /word/media/image1.png
                    log.info("图片数量:{}", pictureData.size());
                    for (XWPFPictureData data : pictureData) {
                        DocumentImage documentImage = new DocxImageExtractor(data).get();
                        documentImages.add(documentImage);
                        // docPicture.del();
                    }
                    document.close();
                }
                case OLE2 -> {
                    // doc
                    fis = new FileInputStream(docXFile);
                    HWPFDocument document = new HWPFDocument(fis);
                    PicturesTable picturesTable = document.getPicturesTable();
                    List<Picture> pictures = picturesTable.getAllPictures();
                    for (int i = 0; i < pictures.size(); i++) {
                        Picture picture = pictures.get(i);
                        DocumentImage documentImage = new DocImageExtractor(picture, i).get();
                        documentImages.add(documentImage);
                    }
                    document.close();
                }
                default -> {
                    // 其他格式不处理
                    log.warn("不支持的文件格式: {}", fileMagic);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.close(fis);
        }
        return documentImages;
    }
    
    /**
     * 替换URL
     * @param line 行数据
     * @param pictures 图片
     * @return 替换后的数据
     */
    public static String replaceURL(String line, List<DocumentImage> pictures) {
        String target = line;
        if (StrUtil.containsAny(line, DocumentImage.EMBEDDED_PREFIX)) {
            for (DocumentImage picture : pictures) {
                if (StrUtil.containsAny(line, picture.embedName())) {
                    target = StrUtil.replace(line, picture.embedName(), picture.getOssUrl());
                    break;
                }
            }
        }
        return target;
    }
    
}
