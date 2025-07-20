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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.poifs.filesystem.FileMagic;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since torchv-unstructured v1.0.0
 */
@Slf4j
public class FileMagicUtils {
    
    /**
     * 检查文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public static boolean isPDF(String fileName) {
        return fileName.endsWith(".pdf");
    }
    
    /**
     * 检查文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public static boolean isWord(String fileName) {
        return fileName.endsWith(".doc") || fileName.endsWith(".docx");
    }
    
    /**
     * 检查文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public static boolean isMarkdown(String fileName) {
        return fileName.endsWith(".md");
    }
    
    /**
     * 检查文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public static boolean isPpt(String fileName) {
        return fileName.endsWith(".ppt") || fileName.endsWith(".pptx");
    }
    
    /**
     * 检查文件类型
     * @param fileName 文件名
     * @return 文件类型
     */
    public static boolean isExcel(String fileName) {
        return fileName.endsWith(".xls") || fileName.endsWith(".xlsx");
    }
    
    public static FileMagic checkMagic(InputStream inputStream) {
        FileMagic fm = null;
        try {
            InputStream is = FileMagic.prepareToCheckMagic(inputStream);
            fm = FileMagic.valueOf(is);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            fm = FileMagic.UNKNOWN;
        }
        return fm;
    }
    
    /**
     * 检查文件类型
     * @param file 文件
     * @return 文件类型
     */
    public static FileMagic checkMagic(File file) {
        if (!FileUtil.exist(file)) {
            return FileMagic.UNKNOWN;
        }
        FileMagic fileMagic = null;
        InputStream inputStream = null;
        try {
            inputStream = Files.newInputStream(file.toPath());
            fileMagic = checkMagic(inputStream);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            fileMagic = FileMagic.UNKNOWN;
        } finally {
            IoUtil.close(inputStream);
        }
        return fileMagic;
    }
}
