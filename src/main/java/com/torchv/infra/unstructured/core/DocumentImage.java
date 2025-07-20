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


package com.torchv.infra.unstructured.core;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 文档图像信息
 * 
 * 表示从文档中提取的图像及其相关信息。
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class DocumentImage {
    
    /**
     * 图像名称/标识符
     */
    private final String name;
    
    /**
     * 图像格式/类型 (例如: png, jpeg, gif)
     */
    private final String format;
    
    /**
     * 图像数据 (Base64编码)
     */
    private final String data;
    
    /**
     * 图像宽度（像素）
     */
    private final int width;
    
    /**
     * 图像高度（像素）
     */
    private final int height;
    
    /**
     * 图像文件大小（字节）
     */
    private final long size;
    
    /**
     * 图像在文档中的位置描述
     */
    private final String position;
    
    /**
     * 图像的替代文本/描述
     */
    private final String altText;
    
    /**
     * 获取图像的数据URL格式
     * 
     * @return 数据URL字符串，可直接用于HTML img标签
     */
    public String getDataUrl() {
        if (data == null || format == null) {
            return null;
        }
        return String.format("data:image/%s;base64,%s", format, data);
    }
    
    /**
     * 检查是否有图像数据
     * 
     * @return 如果有数据返回true
     */
    public boolean hasData() {
        return data != null && !data.trim().isEmpty();
    }
    
    /**
     * 获取图像尺寸描述
     * 
     * @return 尺寸字符串，格式："宽度x高度"
     */
    public String getDimensions() {
        return String.format("%dx%d", width, height);
    }
    
    /**
     * 获取文件大小的可读格式
     * 
     * @return 格式化的文件大小字符串
     */
    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }
}
