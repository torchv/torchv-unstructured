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


package com.torchv.infra.unstructured.parser.word.model;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 */
@Slf4j
@Getter
@Setter
public class DocumentImage {
    
    /**
     * 图片来源名称前缀，在使用Apache Tika将Docx中的内容转为Html/Markdown格式后，图片的前缀标签会带上该字符，例如: embedded:image1.png
     */
    public static final String EMBEDDED_PREFIX = "embedded:";
    /**
     * 图片来源名称，对于docx格式的图片，图片名称一般会是image1.png、image2.png ...
     */
    private String sourceName;
    /**
     * 后缀
     */
    private String suffix;
    /**
     * 保存在本地的图片文件
     */
    private File picFile;
    /**
     * 是否包含图片
     */
    private boolean picFlag;
    
    /**
     * 上传到OSS的地址，在Doc/docx图片提取后，上传到oss
     */
    private String ossUrl;
    
    private boolean needOCR;
    
    public String checkSum() {
        if (!this.picFlag || this.picFile == null || !this.picFile.exists()) {
            return "None";
        }
        return DigestUtil.md5Hex(this.picFile);
    }
    
    /**
     * 创建一个图片对象
     * @param picFlag 是否图片
     * @param suffix 图片后缀
     * @param picFile 图片文件
     * @return 图片对象
     */
    public static DocumentImage of(boolean picFlag, String suffix, File picFile) {
        DocumentImage documentImage = new DocumentImage();
        documentImage.setPicFlag(picFlag);
        documentImage.setSuffix(suffix);
        documentImage.setPicFile(picFile);
        return documentImage;
    }
    
    /**
     * 创建一个图片对象
     * @param picFlag 是否图片
     * @param suffix 图片后缀
     * @param picFile 图片文件
     * @param sourceName 图片来源名称，一般在docx格式中存在该字段
     * @return 图片对象
     */
    public static DocumentImage of(boolean picFlag, String suffix, File picFile, String sourceName) {
        DocumentImage documentImage = of(picFlag, suffix, picFile);
        documentImage.setSourceName(sourceName);
        return documentImage;
    }
    
    /**
     * 创建一个图片对象
     * @param picFlag 是否图片
     * @param suffix 图片后缀
     * @param picFile 图片文件
     * @param sourceName 图片来源名称，一般在docx格式中存在该字段
     * @param ossUrl OSS在线地址
     * @return 图片对象
     */
    public static DocumentImage of(boolean picFlag, String suffix, File picFile, String sourceName, String ossUrl) {
        DocumentImage documentImage = of(picFlag, suffix, picFile, sourceName);
        documentImage.setOssUrl(ossUrl);
        return documentImage;
    }
    
    /**
     * 空图片对象
     * @return 空对象
     */
    public static DocumentImage empty() {
        return DocumentImage.of(false, null, null);
    }
    
    /**
     * 删除该文件，一般该对象是临时文件对象，用完后需要删除
     */
    public void del() {
        if (!this.picFlag || this.picFile == null || !this.picFile.exists()) {
            return;
        }
        FileUtil.del(this.picFile);
    }
    
    /**
     * 转换成html后，或者markdown后，图片引用的原始名称，会追加一个字符串,格式为：embedded:{@link this.sourceName}
     * @return 返回图片引用的名称
     */
    public String embedName() {
        return EMBEDDED_PREFIX + this.sourceName;
    }
    
    /**
     * 文件是否存在
     * @return 文件是否存在，True-存在，False-不存在
     */
    public boolean exists() {
        if (this.picFile == null) {
            return false;
        }
        return this.picFile.exists();
    }
    /**
     * 打印该对象
     */
    public void print() {
        log.info("picFlag:{},suffix:{},picFile:{},sourceName:{}", this.picFlag, this.suffix, this.picFile, this.sourceName);
    }
}
