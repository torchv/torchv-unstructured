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

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.torchv.infra.unstructured.util.DocumentParserUtils;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import com.torchv.infra.unstructured.util.UnstructuredUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.usermodel.Picture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.function.Supplier;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/5/5 16:34
 * @since torchv_server v1.7.3
 */
@Slf4j
@AllArgsConstructor
public class DocImageExtractor implements Supplier<DocumentImage> {
    
    final Picture picture;
    final int index;
    @Override
    public DocumentImage get() {
        File tmpFile = null;
        FileOutputStream fios = null;
        try {
            String mimeType = picture.getMimeType();
            log.info("mime type:{}", mimeType);
            String imgSuffix = UnstructuredUtils.imageSuffix(mimeType);
            tmpFile = FileUtil.createTempFile("image_", "." + imgSuffix, true);
            fios = new FileOutputStream(tmpFile);
            // 写入文件
            picture.writeImageContent(fios);
            int indexName = index + 1;
            String imageName = "image" + indexName + "." + imgSuffix;
            return DocumentImage.of(true, imgSuffix, tmpFile, imageName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IoUtil.close(fios);
        }
        return DocumentImage.empty();
    }
}
