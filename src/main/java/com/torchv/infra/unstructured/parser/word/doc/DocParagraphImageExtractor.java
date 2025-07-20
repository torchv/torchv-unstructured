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

import cn.hutool.core.util.RandomUtil;
import com.torchv.infra.unstructured.parser.word.model.DocumentImage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Picture;

import java.util.function.Supplier;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/4/14 20:58
 * @since torchv_server v1.7.1
 */
@Slf4j
@AllArgsConstructor
public class DocParagraphImageExtractor implements Supplier<DocumentImage> {
    
    final PicturesTable picturesTable;
    final Paragraph paragraph;
    
    @Override
    public DocumentImage get() {
        try {
            CharacterRun characterRun = paragraph.getCharacterRun(0);
            if (picturesTable.hasPicture(characterRun)) {
                Picture picture = picturesTable.extractPicture(characterRun, true);
                return new DocImageExtractor(picture, RandomUtil.randomInt(1, 100)).get();
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return DocumentImage.empty();
    }
}
