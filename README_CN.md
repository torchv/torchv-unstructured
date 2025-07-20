# TorchV Unstructured

[English](README.md) | [中文](README_CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/com.torchv.infra/torchv-unstructured.svg)](https://search.maven.org/artifact/com.torchv.infra/torchv-unstructured)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)

一个强大且开发者友好的文档解析库，专为RAG（检索增强生成）应用优化。基于Apache Tika、Apache POI和PDFBox等业界标准Java库构建，TorchV Unstructured提供了增强的解析能力，具备智能表格结构识别和内容提取功能。

## 🚀 核心特性

- **智能表格解析**：先进的表格结构分析，支持复杂单元格合并检测
- **多格式支持**：无缝处理DOC、DOCX、PDF等多种文档格式
- **RAG优化输出**：专为AI/ML管道设计的结构化内容提取
- **Markdown和HTML导出**：灵活的输出格式，保持表格结构完整性
- **图像提取**：自动提取和处理嵌入式图像
- **内存高效**：优化的大文档处理，最小化内存占用

## 📦 安装

### Maven

```xml
<dependency>
    <groupId>com.torchv.infra</groupId>
    <artifactId>torchv-unstructured</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.torchv.infra:torchv-unstructured:1.0.0'
```

## 🔧 快速开始

### 基础文档解析

```java
import com.torchv.infra.unstructured.WordUnstructured;

// 解析文档为Markdown格式（推荐用于RAG）
String content = WordUnstructured.parseToMarkdown("document.docx");
System.out.

        println(content);

        // 解析文档为带HTML表格的Markdown格式（保持表格结构）
        String contentWithTables = WordUnstructured.parseToMarkdownWithTables("document.docx");
System.out.

        println(contentWithTables);
```

### 高级表格提取

```java
import com.torchv.infra.unstructured.WordUnstructured;

import java.io.File;
import java.util.List;

// 仅提取Word文档中的表格
List<String> tables = WordUnstructured.extractTables("document.docx");
for(
        int i = 0; i <tables.

        size();

        i++){
        System.out.

        println("表格 "+(i +1) +":");
        System.out.

        println(tables.get(i));
        }

        // Word专用表格解析，提供更多控制
        String htmlTables = WordUnstructured.parseWordTablesToHtml(new File("document.docx"));
System.out.

        println(htmlTables);
```

### 文件格式支持

```java
import com.torchv.infra.unstructured.WordUnstructured;

// 检查文件格式是否支持
if(TorchVUnstructured.isSupportedFormat("document.docx")){
String content = WordUnstructured.parseToMarkdownWithTables("document.docx");
    System.out.

println("解析成功！");
}else{
        System.out.

println("不支持的文件格式");
}

// 获取所有支持的格式
List<String> supportedFormats = WordUnstructured.getSupportedFormats();
System.out.

println("支持的格式: "+String.join(", ", supportedFormats));
```

## 🎯 核心组件

### 统一入口

- **TorchVUnstructured**：主要入口类，为所有文档解析操作提供简单统一的API

### 文档解析器

- **TikaAutoUtils**：支持自动检测的通用文档解析器（底层实现）
- **WordTableParser**：专业的Word文档表格解析器
- **DocxTableParser**：高级DOCX表格结构分析器

### 内容处理器

- **ToMarkdownWithHtmlTableContentHandler**：将文档转换为带HTML表格的Markdown
- **DocMarkdownWithHtmlTableContentHandler**：专门的DOC格式处理器
- **DocXMarkdownWithHtmlTableContentHandler**：专门的DOCX格式处理器

### 表格分析

- **TableStructureAnalyzer**：智能表格结构识别
- **CellMergeAnalyzer**：高级单元格合并检测
- **HtmlTableBuilder**：清洁的HTML表格生成器

### 实用工具

- **FileMagicUtils**：文件类型检测和验证
- **ImageExtractParse**：嵌入式图像提取

## 🔍 高级用法

### RAG应用集成

```java
import com.torchv.infra.unstructured.WordUnstructured;

// 为RAG应用优化
public class RAGDocumentProcessor {

    public DocumentChunk processDocument(String filePath) {
        // 解析时保持表格结构以获得更好的上下文
        String content = WordUnstructured.parseToMarkdownWithTables(filePath);

        // 单独提取表格用于结构化数据处理
        List<String> tables = WordUnstructured.extractTables(filePath);

        return new DocumentChunk(content, tables);
    }
}
```

### 批量处理

```java
import com.torchv.infra.unstructured.WordUnstructured;

public class BatchProcessor {

    public void processBatch(List<String> filePaths) {
        filePaths.parallelStream()
                .filter(WordUnstructured::isSupportedFormat)
                .forEach(this::processFile);
    }

    private void processFile(String filePath) {
        try {
            String content = WordUnstructured.parseToMarkdownWithTables(filePath);
            // 保存或进一步处理内容
            saveProcessedContent(filePath, content);
        } catch (Exception e) {
            log.error("处理文件失败: {}", filePath, e);
        }
    }
}
```

### 错误处理和验证

```java
public class DocumentValidator {
    
    public ProcessingResult validateAndProcess(String filePath) {
        // 检查文件格式
        if (!TorchVUnstructured.isSupportedFormat(filePath)) {
            return ProcessingResult.unsupportedFormat();
        }
        
        try {
            String content = TorchVUnstructured.parseToMarkdownWithTables(filePath);
            List<String> tables = TorchVUnstructured.extractTables(filePath);
            
            return ProcessingResult.success(content, tables);
        } catch (RuntimeException e) {
            return ProcessingResult.error(e.getMessage());
        }
    }
}
```

## 🌟 为什么选择TorchV Unstructured？

### 适用于RAG应用

- **结构化输出**：清洁、结构化的内容提取，完美适配嵌入向量生成
- **表格保持**：维护表格关系，对文档理解至关重要
- **丰富元数据**：提取全面的文档元数据，增强检索效果

### 适用于开发者

- **简单API**：直观的接口设计，合理的默认配置
- **可扩展**：基于插件的架构，支持自定义内容处理器
- **生产就绪**：经过实战验证，具备全面的错误处理机制

### 性能优化

- **内存高效**：大文档的流式处理
- **快速处理**：优化算法确保快速解析
- **可扩展**：专为高吞吐量文档处理而设计

## 📚 文档

- [API文档](https://torchv.github.io/torchv-unstructured/)
- [示例仓库](https://github.com/torchv/torchv-unstructured-examples)
- [迁移指南](docs/migration.md)

## 🤝 贡献

我们欢迎贡献！请查看我们的[贡献指南](CONTRIBUTING.md)了解详情。

1. Fork这个仓库
2. 创建你的功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 打开一个Pull Request

## 📄 许可证

本项目基于Apache License 2.0许可证 - 查看[LICENSE](LICENSE)文件了解详情。

## 🙏 致谢

- [Apache Tika](https://tika.apache.org/) - 内容分析工具包
- [Apache POI](https://poi.apache.org/) - Microsoft文档的Java API
- [PDFBox](https://pdfbox.apache.org/) - PDF文档操作库

## 📞 支持

- 📧 邮箱：<xiaoymin@foxmail.com>
- 🐛 问题反馈：[GitHub Issues](https://github.com/torchv/torchv-unstructured/issues)
- 💬 讨论：[GitHub Discussions](https://github.com/torchv/torchv-unstructured/discussions)

---

由 [TorchV团队](https://www.torchv.com/) 用 ❤️ 制作
