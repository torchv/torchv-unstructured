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

import java.util.List;
import java.util.Map;

/**
 * 文档解析结果
 * 
 * 包含解析后的文档内容、表格、图像、元数据和处理信息。
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class DocumentResult {
    
    /**
     * 文件路径
     */
    private final String filePath;
    
    /**
     * 文件名
     */
    private final String fileName;
    
    /**
     * 文件大小（字节）
     */
    private final long fileSize;
    
    /**
     * 解析后的文档内容
     */
    private final String content;
    
    /**
     * 提取的表格列表
     */
    @Builder.Default
    private final List<String> tables = List.of();
    
    /**
     * 提取的图像列表
     */
    @Builder.Default
    private final List<DocumentImage> images = List.of();
    
    /**
     * 文档元数据
     */
    @Builder.Default
    private final Map<String, Object> metadata = Map.of();
    
    /**
     * 是否解析成功
     */
    @Builder.Default
    private final boolean success = true;
    
    /**
     * 错误信息（如果解析失败）
     */
    private final String errorMessage;
    
    /**
     * 处理时间（毫秒）
     */
    private final long processingTimeMs;
    
    /**
     * 检查是否解析成功
     * 
     * @return 如果成功返回true，否则返回false
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 检查是否解析失败
     * 
     * @return 如果失败返回true，否则返回false
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 获取内容长度
     * 
     * @return 内容字符数
     */
    public int getContentLength() {
        return content != null ? content.length() : 0;
    }
    
    /**
     * 获取表格数量
     * 
     * @return 表格数量
     */
    public int getTableCount() {
        return tables != null ? tables.size() : 0;
    }
    
    /**
     * 获取图像数量
     * 
     * @return 图像数量
     */
    public int getImageCount() {
        return images != null ? images.size() : 0;
    }
    
    /**
     * 检查是否包含内容
     * 
     * @return 如果有内容返回true
     */
    public boolean hasContent() {
        return content != null && !content.trim().isEmpty();
    }
    
    /**
     * 检查是否包含表格
     * 
     * @return 如果有表格返回true
     */
    public boolean hasTables() {
        return tables != null && !tables.isEmpty();
    }
    
    /**
     * 检查是否包含图像
     * 
     * @return 如果有图像返回true
     */
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
    
    /**
     * 获取处理时间（秒）
     * 
     * @return 处理时间（秒）
     */
    public double getProcessingTimeSeconds() {
        return processingTimeMs / 1000.0;
    }
}
