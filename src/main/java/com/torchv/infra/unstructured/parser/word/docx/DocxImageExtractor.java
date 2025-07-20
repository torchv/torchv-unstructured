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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import lombok.AllArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;

import java.io.File;
import java.util.function.Supplier;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 10:43
 * @since torchv_server v1.7.3
 */
@AllArgsConstructor
public class DocxImageExtractor implements Supplier<DocumentImage> {
    
    /**
     * docx对象中的图片原始数据
     */
    final XWPFPictureData pictureData;
    
    @Override
    public DocumentImage get() {
        String fileName = pictureData.getFileName();
        String suffix = FileUtil.extName(fileName);
        if (StrUtil.isBlank(suffix) && pictureData.getPictureTypeEnum() != null) {
            suffix = pictureData.getPictureTypeEnum().name().toLowerCase();
        }
        File tmpFile = FileUtil.createTempFile("image_", "." + suffix, true);
        // 输出到文件
        FileUtil.writeBytes(pictureData.getData(), tmpFile);
        return DocumentImage.of(true, suffix, tmpFile, fileName);
    }
}
