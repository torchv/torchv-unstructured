# TorchV Unstructured - K-V 格式提取功能实现总结

## 📋 功能概述

本次更新为 TorchV Unstructured 项目添加了 **K-V 格式提取**（键值对提取）功能，这是一个专为 RAG（检索增强生成）应用优化的核心特性。该功能能够智能地从文档中提取键值对信息，将文档内容原子化为独立的语义单元。

## 🎯 实现的功能

### 1. 核心数据结构

#### KeyValuePair 类
- **位置**: `src/main/java/com/torchv/infra/unstructured/core/KeyValuePair.java`
- **功能**: 表示键值对数据结构
- **主要字段**:
  - `key`: 键（属性名称或问题）
  - `value`: 值（属性值或答案）
  - `confidence`: 置信度（0.0-1.0）
  - `sourceType`: 来源类型（PARAGRAPH/TABLE/LIST/HEADING/OTHER）
  - `position`: 在文档中的位置
  - `metadata`: 附加元数据
- **主要方法**:
  - `toMarkdown()`: 转换为 Markdown 格式
  - `toPlainText()`: 转换为纯文本格式
  - `toJson()`: 转换为 JSON 格式
  - `toMarkdownWithConfidence()`: 转换为带置信度的 Markdown
  - `isValid()`: 检查键值对是否有效

### 2. 提取器类

#### KeyValueExtractor 类
- **位置**: `src/main/java/com/torchv/infra/unstructured/extractor/KeyValueExtractor.java`
- **功能**: 从文本和表格中智能提取键值对
- **支持的格式**:
  1. **冒号分隔**: `键: 值` 或 `键：值`（置信度 0.8）
  2. **Markdown 格式**: `- **键**: 值`（置信度 0.95）
  3. **等号分隔**: `键 = 值`（置信度 0.85）
  4. **问答格式**: `Q: 问题 A: 答案`（置信度 0.9）
  5. **表格格式**: 从 HTML 表格中提取（置信度 0.9）

- **主要方法**:
  - `extractFromText(String content)`: 从文本提取键值对
  - `extractFromLine(String line, int position)`: 从单行提取键值对
  - `extractFromTable(String tableHtml)`: 从表格提取键值对
  - `toMarkdown(List<KeyValuePair> pairs)`: 转换为 Markdown
  - `toPlainText(List<KeyValuePair> pairs)`: 转换为纯文本
  - `toJsonArray(List<KeyValuePair> pairs)`: 转换为 JSON 数组
  - `filterByConfidence(List<KeyValuePair> pairs, double minConfidence)`: 按置信度过滤
  - `filterBySourceType(List<KeyValuePair> pairs, SourceType type)`: 按来源类型过滤

### 3. API 集成

#### UnstructuredParser 类更新
- **位置**: `src/main/java/com/torchv/infra/unstructured/UnstructuredParser.java`
- **新增方法**:
  - `extractKeyValuePairs(String filePath)`: 提取键值对
  - `toKeyValueMarkdown(String filePath)`: 提取并转为 Markdown
  - `toKeyValueText(String filePath)`: 提取并转为纯文本
  - `toKeyValueJson(String filePath)`: 提取并转为 JSON

#### UnstructuredWord 类更新
- **位置**: `src/main/java/com/torchv/infra/unstructured/parser/word/UnstructuredWord.java`
- **新增方法**:
  - `extractKeyValuePairs(String filePath)`: Word 文档键值对提取
  - `toKeyValueMarkdown(String filePath)`: Word 文档转 Markdown
  - `toKeyValueText(String filePath)`: Word 文档转纯文本
  - `toKeyValueJson(String filePath)`: Word 文档转 JSON

#### WordParser 类更新
- **位置**: `src/main/java/com/torchv/infra/unstructured/parser/word/WordParser.java`
- **新增方法**:
  - `extractKeyValuePairs(String filePath)`: 实际的提取逻辑

#### DocumentResult 类更新
- **位置**: `src/main/java/com/torchv/infra/unstructured/core/DocumentResult.java`
- **新增字段**:
  - `keyValuePairs`: 键值对列表

## 📝 测试和示例

### 1. 单元测试
- **位置**: `src/test/java/com/torchv/infra/unstructured/KeyValueExtractionTest.java`
- **测试内容**:
  - 冒号格式提取测试
  - Markdown 格式提取测试
  - 问答格式提取测试
  - 等号格式提取测试
  - 格式转换测试
  - 过滤功能测试

### 2. 示例代码
- **位置**: `src/test/java/com/torchv/infra/unstructured/examples/KeyValueExtractionExample.java`
- **示例内容**:
  - 从文本中提取键值对
  - 提取不同格式的键值对
  - 转换为不同输出格式
  - 过滤和筛选

## 📚 文档更新

### 1. 使用说明文档
- **文件**: `使用说明.md`
- **更新内容**:
  - 核心特性中添加 K-V 格式提取
  - API 说明中添加新方法
  - DocumentResult 和 KeyValuePair 数据结构说明
  - 完整的 K-V 提取使用示例
  - RAG 应用场景说明

### 2. K-V 功能专项文档
- **文件**: `K-V格式提取功能说明.md`
- **内容**:
  - 功能概述
  - 快速开始
  - 支持的格式详解
  - 数据结构说明
  - 高级用法
  - RAG 应用场景
  - API 参考
  - 性能建议
  - 常见问题

### 3. README 更新
- **文件**: `README_CN.md`
- **更新内容**:
  - 核心特性中添加 K-V 格式提取（标记为 NEW）
  - 快速开始中添加 K-V 提取示例
  - 添加支持的 K-V 格式说明
  - 添加 K-V 格式优势说明

## 🔧 技术实现细节

### 1. 正则表达式模式
```java
// 冒号分隔模式
Pattern COLON_PATTERN = Pattern.compile("^([^:：]+)[：:]\\s*(.+)$");

// 等号分隔模式
Pattern EQUALS_PATTERN = Pattern.compile("^([^=]+)=\\s*(.+)$");

// Markdown 格式模式
Pattern MARKDOWN_PATTERN = Pattern.compile("^[-*]\\s*\\*\\*([^*]+)\\*\\*[：:]?\\s*(.+)$");

// 问答格式模式
Pattern QA_PATTERN = Pattern.compile("^[QqＱｑ问][：:]\\s*(.+?)\\s+[AaＡａ答][：:]\\s*(.+)$");
```

### 2. 智能过滤
- 键长度限制（不超过 50 个字符）
- 标点符号数量检查（避免误识别句子）
- 键的最小长度要求（至少 2 个字符）

### 3. 置信度评分
- Markdown 格式：0.95（最高）
- 问答格式：0.9
- 表格格式：0.9
- 等号分隔：0.85
- 冒号分隔：0.8

## 📊 文件清单

### 新增文件
1. `src/main/java/com/torchv/infra/unstructured/core/KeyValuePair.java`
2. `src/main/java/com/torchv/infra/unstructured/extractor/KeyValueExtractor.java`
3. `src/test/java/com/torchv/infra/unstructured/KeyValueExtractionTest.java`
4. `src/test/java/com/torchv/infra/unstructured/examples/KeyValueExtractionExample.java`
5. `K-V格式提取功能说明.md`
6. `K-V功能实现总结.md`（本文件）

### 修改文件
1. `src/main/java/com/torchv/infra/unstructured/UnstructuredParser.java`
2. `src/main/java/com/torchv/infra/unstructured/parser/word/UnstructuredWord.java`
3. `src/main/java/com/torchv/infra/unstructured/parser/word/WordParser.java`
4. `src/main/java/com/torchv/infra/unstructured/core/DocumentResult.java`
5. `使用说明.md`
6. `README_CN.md`

## ✅ 功能特点

1. **智能识别**: 支持 5 种常见的 K-V 格式
2. **置信度评分**: 每个键值对都有置信度评分
3. **来源追踪**: 记录键值对的来源类型和位置
4. **灵活输出**: 支持 Markdown、纯文本、JSON 多种输出格式
5. **过滤功能**: 支持按置信度和来源类型过滤
6. **RAG 优化**: 专为 RAG 应用场景设计
7. **易于集成**: 简单的 API 设计，易于集成到现有项目

## 🎯 使用场景

1. **产品手册问答系统**: 提取产品参数和规格
2. **员工信息查询**: 提取员工档案信息
3. **FAQ 自动问答**: 提取问答对
4. **配置文件解析**: 提取配置项
5. **知识库构建**: 为 RAG 系统构建结构化知识库

## 🚀 后续优化方向

1. 支持更多文件格式（PDF、PPT 等）
2. 支持自定义 K-V 格式模式
3. 提供更多的过滤和排序选项
4. 优化表格解析逻辑，支持更复杂的表格结构
5. 添加批量处理和并行处理支持
6. 提供配置选项控制提取行为

## 📞 联系方式

- **作者**: xiaoymin
- **邮箱**: xiaoymin@foxmail.com
- **项目**: TorchV Unstructured

---

**实现日期**: 2025-11-06  
**版本**: 1.0.0  
**状态**: ✅ 已完成

