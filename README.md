# TorchV Unstructured

[English](README.md) | [中文](README_CN.md)

[![Maven Central](https://img.shields.io/maven-central/v/com.torchv.infra/torchv-unstructured.svg)](https://search.maven.org/artifact/com.torchv.infra/torchv-unstructured)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)

A powerful and developer-friendly document parsing library optimized for RAG (Retrieval Augmented Generation) applications. Built on top of industry-standard Java libraries like Apache Tika, Apache POI, and PDFBox, TorchV Unstructured provides enhanced parsing capabilities with intelligent table structure recognition and content extraction.

## 🚀 Key Features

- **Intelligent Table Parsing**: Advanced table structure analysis with proper cell merging detection
- **Multi-format Support**: Seamless handling of DOC, DOCX, PDF, and other document formats
- **RAG-Optimized Output**: Structured content extraction designed for AI/ML pipelines
- **Markdown & HTML Export**: Flexible output formats with preserved table structures
- **Image Extraction**: Automatic extraction and handling of embedded images
- **Memory Efficient**: Optimized for processing large documents with minimal memory footprint

## 📦 Installation

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

## 🔧 Quick Start

### Basic Document Parsing

```java
import com.torchv.infra.unstructured.UnstructuredParser;

// Parse document to markdown (recommended for RAG)
String content = UnstructuredParser.toMarkdown("document.docx");
System.out.println(content);

// Parse document to markdown with HTML tables (preserving table structure)
String contentWithTables = UnstructuredParser.toMarkdownWithHtmlTables("document.docx");
System.out.println(contentWithTables);
```

### Advanced Table Extraction

```java
import com.torchv.infra.unstructured.UnstructuredParser;

import java.io.File;
import java.util.List;

// Extract only tables from Word documents
List<String> tables = UnstructuredParser.extractTables("document.docx");
for (int i = 0; i < tables.size(); i++) {
    System.out.println("Table " + (i + 1) + ":");
    System.out.println(tables.get(i));
}

// Get structured result with more control
DocumentResult result = UnstructuredParser.toStructuredResult("document.docx");
if (result.isSuccess()) {
    System.out.println("Content: " + result.getContent());
    System.out.println("Tables: " + result.getTables());
}
```

### File Format Support

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.util.UnstructuredUtils;

// Check if file format is supported
if (UnstructuredUtils.isSupportedFormat("document.docx")) {
    String content = UnstructuredParser.toMarkdownWithHtmlTables("document.docx");
    System.out.println("Parsing successful!");
} else {
    System.out.println("Unsupported file format");
}

// Get all supported formats
List<String> supportedFormats = UnstructuredUtils.getSupportedFormats();
System.out.println("Supported formats: " + String.join(", ", supportedFormats));
```

## 🎯 Core Components

### Unified Entry Point

- **UnstructuredParser**: Main entry class providing simple and unified API for all document parsing operations

### Document Parsers

- **UnstructuredWord**: Universal Word document parsing with auto-detection  
- **TikaAutoUtils**: Generic document parsing with auto-detection (underlying implementation)
- **WordTableParser**: Specialized Word document table parser
- **DocxTableParser**: Advanced DOCX table structure analyzer

### Content Handlers

- **ToMarkdownWithHtmlTableContentHandler**: Converts documents to Markdown with HTML tables
- **DocMarkdownWithHtmlTableContentHandler**: Specialized DOC format handler
- **DocXMarkdownWithHtmlTableContentHandler**: Specialized DOCX format handler

### Table Analysis

- **TableStructureAnalyzer**: Intelligent table structure recognition
- **CellMergeAnalyzer**: Advanced cell merging detection
- **HtmlTableBuilder**: Clean HTML table generation

### Utilities

- **FileMagicUtils**: File type detection and validation
- **ImageExtractParse**: Embedded image extraction

## 🔍 Advanced Usage

### RAG Application Integration

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.DocumentResult;

// Optimized for RAG applications
public class RAGDocumentProcessor {

    public DocumentChunk processDocument(String filePath) {
        // Parse with table structure preservation for better context
        String content = UnstructuredParser.toMarkdownWithHtmlTables(filePath);

        // Extract tables separately for structured data processing
        List<String> tables = UnstructuredParser.extractTables(filePath);

        return new DocumentChunk(content, tables);
    }
}
```

### Batch Processing

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.util.UnstructuredUtils;

public class BatchProcessor {

    public void processBatch(List<String> filePaths) {
        filePaths.parallelStream()
                .filter(UnstructuredUtils::isSupportedFormat)
                .forEach(this::processFile);
    }

    private void processFile(String filePath) {
        try {
            String content = UnstructuredParser.toMarkdownWithHtmlTables(filePath);
            // Save or further process the content
            saveProcessedContent(filePath, content);
        } catch (Exception e) {
            log.error("Failed to process file: {}", filePath, e);
        }
    }
}
```

### Error Handling and Validation

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.util.UnstructuredUtils;

public class DocumentValidator {
    
    public ProcessingResult validateAndProcess(String filePath) {
        // Check file format
        if (!UnstructuredUtils.isSupportedFormat(filePath)) {
            return ProcessingResult.unsupportedFormat();
        }
        
        try {
            String content = UnstructuredParser.toMarkdownWithHtmlTables(filePath);
            List<String> tables = UnstructuredParser.extractTables(filePath);
            
            return ProcessingResult.success(content, tables);
        } catch (RuntimeException e) {
            return ProcessingResult.error(e.getMessage());
        }
    }
}
```

## 🌟 Why Choose TorchV Unstructured?

### For RAG Applications

- **Structured Output**: Clean, structured content extraction perfect for embedding generation
- **Table Preservation**: Maintains table relationships crucial for document understanding
- **Metadata Rich**: Extracts comprehensive document metadata for enhanced retrieval

### For Developers

- **Simple API**: Intuitive interfaces with sensible defaults
- **Extensible**: Plugin-based architecture for custom content handlers
- **Production Ready**: Battle-tested with comprehensive error handling

### Performance Optimized

- **Memory Efficient**: Streaming processing for large documents
- **Fast Processing**: Optimized algorithms for quick parsing
- **Scalable**: Designed for high-throughput document processing

## 📚 Documentation

- [API Documentation](https://torchv.github.io/torchv-unstructured/)
- [Examples Repository](https://github.com/torchv/torchv-unstructured-examples)
- [Migration Guide](docs/migration.md)

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Apache Tika](https://tika.apache.org/) - Content analysis toolkit
- [Apache POI](https://poi.apache.org/) - Java API for Microsoft Documents
- [PDFBox](https://pdfbox.apache.org/) - PDF document manipulation

## 📞 Support

- 📧 Email: <xiaoymin@foxmail.com>
- 🐛 Issues: [GitHub Issues](https://github.com/torchv/torchv-unstructured/issues)
- 💬 Discussions: [GitHub Discussions](https://github.com/torchv/torchv-unstructured/discussions)

---

Made with ❤️ by [TorchV Team](https://www.torchv.com/)
