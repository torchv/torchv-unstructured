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

/**
 * 批量处理结果
 * 
 * 包含批量文档处理的统计信息和详细结果。
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Data
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class BatchResult {
    
    /**
     * 总文件数
     */
    private final int totalFiles;
    
    /**
     * 成功处理的文件数
     */
    private final int successCount;
    
    /**
     * 失败的文件数
     */
    private final int errorCount;
    
    /**
     * 成功处理的结果列表
     */
    @Builder.Default
    private final List<DocumentResult> results = List.of();
    
    /**
     * 错误信息列表
     */
    @Builder.Default
    private final List<String> errors = List.of();
    
    /**
     * 总处理时间（毫秒）
     */
    private final long totalProcessingTimeMs;
    
    /**
     * 获取成功率
     * 
     * @return 成功率（0.0 - 1.0）
     */
    public double getSuccessRate() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return (double) successCount / totalFiles;
    }
    
    /**
     * 获取成功率百分比
     * 
     * @return 成功率百分比字符串
     */
    public String getSuccessRatePercentage() {
        return String.format("%.1f%%", getSuccessRate() * 100);
    }
    
    /**
     * 获取失败率
     * 
     * @return 失败率（0.0 - 1.0）
     */
    public double getErrorRate() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return (double) errorCount / totalFiles;
    }
    
    /**
     * 获取平均处理时间（毫秒）
     * 
     * @return 平均处理时间
     */
    public double getAverageProcessingTimeMs() {
        if (totalFiles == 0) {
            return 0.0;
        }
        return (double) totalProcessingTimeMs / totalFiles;
    }
    
    /**
     * 获取总处理时间（秒）
     * 
     * @return 总处理时间（秒）
     */
    public double getTotalProcessingTimeSeconds() {
        return totalProcessingTimeMs / 1000.0;
    }
    
    /**
     * 检查是否全部成功
     * 
     * @return 如果全部成功返回true
     */
    public boolean isAllSuccess() {
        return errorCount == 0 && successCount == totalFiles;
    }
    
    /**
     * 检查是否有失败
     * 
     * @return 如果有失败返回true
     */
    public boolean hasErrors() {
        return errorCount > 0;
    }
    
    /**
     * 获取批处理摘要
     * 
     * @return 摘要字符串
     */
    public String getSummary() {
        return String.format(
                "Batch Processing Summary: %d total, %d success, %d errors (%.1f%% success rate), %.2f seconds",
                totalFiles, successCount, errorCount, getSuccessRate() * 100, getTotalProcessingTimeSeconds());
    }
}
