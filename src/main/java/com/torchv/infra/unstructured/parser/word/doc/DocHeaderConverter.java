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

import java.util.function.Supplier;

/**
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * 2024/4/14 20:41
 * @since torchv_server v1.7.1
 */
@AllArgsConstructor
public class DocHeaderConverter implements Supplier<String> {
    
    final int level;
    final String text;
    
    @Override
    public String get() {
        StringBuilder stringBuilder = new StringBuilder();
        if (level > 8) {
            stringBuilder.append(text);
        } else {
            if (StrUtil.isNotBlank(text)) {
                stringBuilder.append("#".repeat(Math.max(0, level + 1)));
                stringBuilder.append(" ");
                stringBuilder.append(text);
            } else {
                stringBuilder.append(StrUtil.EMPTY);
            }
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
