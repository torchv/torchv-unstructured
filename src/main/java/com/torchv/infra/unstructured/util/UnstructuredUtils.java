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
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * 文档解析工具类
 * 
 * 提供静态工具方法用于文档格式检查、版本信息等通用功能。
 * 这些方法从 TorchVUnstructured 中抽取出来，避免与 WordParser 的功能重复。
 * 
 * @author <a href="xiaoymin@foxmail.com">xiaoymin@foxmail.com</a>
 * @since 1.0.0
 */
@Slf4j
public class UnstructuredUtils {
    
    // ==================== 版本信息 ====================
    
    /**
     * 获取库版本信息
     * 
     * @return 版本信息
     */
    public static String getVersion() {
        return "1.0.0";
    }
    
    // ==================== 格式支持检查 ====================
    
    /**
     * 获取支持的文档格式
     * 
     * @return 支持的文档格式列表
     */
    public static List<String> getSupportedFormats() {
        return List.of("docx", "doc", "pdf", "txt", "html", "xml");
    }
    
    /**
     * 检查文档格式是否支持
     * 
     * @param fileName 文件名
     * @return 是否支持该格式
     */
    public static boolean isSupportedFormat(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }
        
        String extension = getFileExtension(fileName.toLowerCase());
        return getSupportedFormats().contains(extension);
    }
    
    /**
     * 检查是否为Word文档
     * 
     * @param file 文件对象
     * @return 是否为Word文档
     */
    public static boolean isWordDocument(File file) {
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        return isWordExtension(file.getName());
    }
    
    /**
     * 判断文件是否为Word文档名扩展名
     *
     * @param fileName 文件名
     * @return 如果是docx / doc 返回true，否则返回false
     */
    public static boolean isWordExtension(String fileName) {
        String extension = getFileExtension(fileName.toLowerCase());
        return "docx".equals(extension) || "doc".equals(extension);
    }
    
    /**
     * 检查是否为Word文档
     * 
     * @param filePath 文件路径
     * @return 是否为Word文档
     */
    public static boolean isWordDocument(String filePath) {
        return isWordDocument(new File(filePath));
    }
    
    // ==================== 文件工具方法 ====================
    
    /**
     * 获取文件扩展名
     * 
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号）
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
    
    /**
     * 验证文件大小
     * 
     * @param file 文件对象
     * @param maxSizeInMB 最大文件大小（MB）
     * @return 是否在大小限制内
     */
    public static boolean isValidFileSize(File file, long maxSizeInMB) {
        if (!file.exists()) {
            return false;
        }
        
        long fileSizeMB = file.length() / (1024 * 1024);
        return fileSizeMB <= maxSizeInMB;
    }
    
    /**
     * 创建安全的临时文件名
     * 
     * @param originalFileName 原始文件名
     * @param prefix 前缀
     * @return 临时文件名
     */
    public static String createSafeTempFileName(String originalFileName, String prefix) {
        String extension = getFileExtension(originalFileName);
        String baseName = originalFileName.substring(0, originalFileName.lastIndexOf('.') > 0 ? originalFileName.lastIndexOf('.') : originalFileName.length());
        String safeName = baseName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return prefix + "_" + System.currentTimeMillis() + "_" + safeName +
                (extension.isEmpty() ? "" : "." + extension);
    }
    
    /**
     * 获取图片后缀
     * @param mimeType 图片类型
     * @return 图片后缀
     */
    public static String imageSuffix(String mimeType) {
        try {
            List<String> type = StrUtil.split(mimeType, "/");
            if (CollUtil.size(type) == 2) {
                String imageType = type.get(1);
                if (!StrUtil.equalsIgnoreCase("unknown", imageType)) {
                    return imageType;
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
        return "png";
    }
    
    // ==================== 信息输出 ====================
    
    /**
     * 打印库信息
     */
    public static void printLibraryInfo() {
        System.out.println("=== TorchV Unstructured Library v" + getVersion() + " ===");
        System.out.println("专为RAG应用优化的文档解析库");
        System.out.println("支持格式: " + String.join(", ", getSupportedFormats()));
        System.out.println();
    }
    
    /**
     * 替换 Tika 解析的表格为自定义 HTML 表格列表，按顺序替换多个表格
     */
    public static String replaceTablesWithCustomHtmlList(String tikaContent, List<String> customTablesHtmlList) {
        log.info("开始替换多个表格内容，Tika内容长度: {}, 自定义表格数量: {}",
                tikaContent.length(), customTablesHtmlList.size());
        
        if (CollUtil.isEmpty(customTablesHtmlList)) {
            log.info("没有自定义表格HTML，返回原始内容");
            return tikaContent;
        }
        
        String finalContent = tikaContent;
        
        // 1. 移除 Tika 解析的原有表格标签和内容，用占位符替换
        finalContent = finalContent.replaceAll("(?s)<table[^>]*>.*?</table>", "<!-- TABLE_PLACEHOLDER -->");
        
        // 移除残留的单独 </table> 标签
        finalContent = finalContent.replaceAll("(?m)^\\s*</table>\\s*$", "");
        
        // 移除残留的 <tr> 标签
        finalContent = finalContent.replaceAll("(?s)<tr[^>]*>.*?</tr>", "");
        
        // 移除残留的 <td> 标签
        finalContent = finalContent.replaceAll("(?s)<td[^>]*>.*?</td>", "");
        
        log.info("已移除原有表格内容并设置占位符");
        
        // 2. 按顺序替换每个表格
        for (int i = 0; i < customTablesHtmlList.size(); i++) {
            String tableHtml = customTablesHtmlList.get(i);
            
            // 清理表格 HTML 中的不可见字符
            String cleanedTableHtml = UnstructuredUtils.cleanTableHtml(tableHtml);
            
            if (finalContent.contains("<!-- TABLE_PLACEHOLDER -->")) {
                finalContent = finalContent.replaceFirst("<!-- TABLE_PLACEHOLDER -->", cleanedTableHtml);
                log.info("已在原位置替换第 {} 个表格", i + 1);
            } else {
                // 如果没有找到占位符，在文档末尾添加表格
                StringBuilder sb = new StringBuilder(finalContent);
                sb.append("\n\n").append(cleanedTableHtml).append("\n\n");
                finalContent = sb.toString();
                log.info("未找到占位符，在文档末尾添加第 {} 个表格", i + 1);
            }
        }
        
        // 3. 移除任何剩余的占位符（如果有）
        finalContent = finalContent.replaceAll("<!-- TABLE_PLACEHOLDER -->", "");
        
        // 4. 清理多余的空行
        finalContent = finalContent.replaceAll("(?m)^\\s*$\\n", "");
        
        log.info("多个表格内容替换完成，最终内容长度: {}", finalContent.length());
        return finalContent;
    }
    
    /**
     * 清理表格 HTML 中的不可见字符，特别是
     * <td>前的特殊字符
     */
    public static String cleanTableHtml(String tableHtml) {
        if (tableHtml == null || tableHtml.isEmpty()) {
            return tableHtml;
        }
        
        // 清理 <td> 前的不可见字符
        // 使用更精确的正则表达式匹配 <td> 前的各种不可见字符
        String cleaned = tableHtml.replaceAll(
                "[\\u0000-\\u001F\\u007F-\\u009F\\u00A0\\u1680\\u180E\\u2000-\\u200F\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+(<td)",
                "$1");
        
        // 清理 <tr> 前的不可见字符
        cleaned = cleaned.replaceAll(
                "[\\u0000-\\u001F\\u007F-\\u009F\\u00A0\\u1680\\u180E\\u2000-\\u200F\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+(<tr)",
                "$1");
        
        // 清理 </td> 后的不可见字符
        cleaned = cleaned.replaceAll(
                "(</td>)[\\u0000-\\u001F\\u007F-\\u009F\\u00A0\\u1680\\u180E\\u2000-\\u200F\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+",
                "$1");
        
        // 清理 </tr> 后的不可见字符
        cleaned = cleaned.replaceAll(
                "(</tr>)[\\u0000-\\u001F\\u007F-\\u009F\\u00A0\\u1680\\u180E\\u2000-\\u200F\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+",
                "$1");
        
        log.debug("清理表格 HTML 不可见字符: 原长度={}, 清理后长度={}", tableHtml.length(), cleaned.length());
        
        return cleaned;
    }
    
    /**
     * 打印使用示例
     */
    public static void printUsageExample() {
        System.out.println("使用方法:");
        System.out.println("  // 创建解析器");
        System.out.println("  WordParser parser = new WordParser();");
        System.out.println("  DocumentResult result = parser.parse(\"document.docx\");");
        System.out.println();
        System.out.println("  // 批量处理");
        System.out.println("  List<String> files = Arrays.asList(\"doc1.docx\", \"doc2.docx\");");
        System.out.println("  BatchResult batchResult = parser.parseBatch(files);");
        System.out.println();
        System.out.println("  // 仅提取表格");
        System.out.println("  List<String> tables = parser.extractWordTables(\"document.docx\");");
    }
}
