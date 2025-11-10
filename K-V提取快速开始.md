# K-V 格式提取 - 快速开始指南

## 🚀 5 分钟上手

### 1. 最简单的用法

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import java.util.List;

// 从 Word 文档中提取键值对
List<KeyValuePair> kvPairs = UnstructuredParser.extractKeyValuePairs("document.docx");

// 打印结果
for (KeyValuePair pair : kvPairs) {
    System.out.println(pair.getKey() + ": " + pair.getValue());
}
```

### 2. 转换为 Markdown 格式

```java
// 一行代码搞定
String markdown = UnstructuredParser.toKeyValueMarkdown("document.docx");
System.out.println(markdown);
```

输出示例：
```
- **产品名称**: TorchV Unstructured
- **版本**: 1.0.0
- **作者**: xiaoymin
```

### 3. 转换为 JSON 格式

```java
// 转换为 JSON
String json = UnstructuredParser.toKeyValueJson("document.docx");
System.out.println(json);
```

输出示例：
```json
[
  {"key": "产品名称", "value": "TorchV Unstructured"},
  {"key": "版本", "value": "1.0.0"},
  {"key": "作者", "value": "xiaoymin"}
]
```

## 📝 支持的文档格式

在你的 Word 文档中，可以使用以下任意格式：

### 格式 1: 冒号分隔
```
产品名称: TorchV Unstructured
版本: 1.0.0
作者: xiaoymin
```

### 格式 2: Markdown 格式（推荐）
```
- **产品名称**: TorchV Unstructured
- **版本**: 1.0.0
- **作者**: xiaoymin
```

### 格式 3: 等号分隔
```
timeout = 30秒
max_connections = 100
enable_cache = true
```

### 格式 4: 问答格式
```
Q: 忘记密码怎么办？ A: 点击找回密码
Q: 支持哪些支付方式？ A: 微信、支付宝、银联
```

### 格式 5: 表格
| 属性 | 值 |
|------|-----|
| 产品名称 | TorchV Unstructured |
| 版本 | 1.0.0 |

## 🎯 高级用法

### 过滤高质量结果

```java
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;

// 提取所有键值对
List<KeyValuePair> allPairs = UnstructuredParser.extractKeyValuePairs("document.docx");

// 只保留高置信度的（>= 0.8）
List<KeyValuePair> highQuality = KeyValueExtractor.filterByConfidence(allPairs, 0.8);

// 打印结果
for (KeyValuePair pair : highQuality) {
    System.out.println(pair.toMarkdownWithConfidence());
}
```

### 按来源类型过滤

```java
// 只保留来自表格的键值对
List<KeyValuePair> fromTable = KeyValueExtractor.filterBySourceType(
    allPairs, 
    KeyValuePair.SourceType.TABLE
);

// 只保留来自段落的键值对
List<KeyValuePair> fromParagraph = KeyValueExtractor.filterBySourceType(
    allPairs, 
    KeyValuePair.SourceType.PARAGRAPH
);
```

## 💡 RAG 应用示例

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import java.util.List;

public class RAGExample {
    
    public static void main(String[] args) {
        // 1. 提取键值对
        List<KeyValuePair> kvPairs = UnstructuredParser.extractKeyValuePairs("product_manual.docx");
        
        // 2. 过滤高质量结果
        List<KeyValuePair> highQuality = kvPairs.stream()
            .filter(pair -> pair.getConfidence() >= 0.8)
            .toList();
        
        // 3. 为每个键值对生成向量并存储到向量数据库
        for (KeyValuePair pair : highQuality) {
            String text = pair.getKey() + ": " + pair.getValue();
            
            // 生成向量（使用你的向量化模型）
            // float[] embedding = embeddingModel.encode(text);
            
            // 存储到向量数据库
            // vectorDB.store(embedding, pair);
            
            System.out.println("已索引: " + text);
        }
    }
}
```

## 📊 完整示例

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;
import java.util.List;

public class CompleteExample {
    
    public static void main(String[] args) {
        String filePath = "document.docx";
        
        // 方式 1: 直接获取 Markdown 格式
        System.out.println("=== Markdown 格式 ===");
        String markdown = UnstructuredParser.toKeyValueMarkdown(filePath);
        System.out.println(markdown);
        
        // 方式 2: 获取键值对列表并处理
        System.out.println("\n=== 键值对列表 ===");
        List<KeyValuePair> pairs = UnstructuredParser.extractKeyValuePairs(filePath);
        
        // 过滤高置信度
        List<KeyValuePair> highConfidence = KeyValueExtractor.filterByConfidence(pairs, 0.8);
        
        System.out.println("总共提取: " + pairs.size() + " 个键值对");
        System.out.println("高质量: " + highConfidence.size() + " 个");
        
        // 打印详细信息
        for (KeyValuePair pair : highConfidence) {
            System.out.println("\n键: " + pair.getKey());
            System.out.println("值: " + pair.getValue());
            System.out.println("置信度: " + pair.getConfidence());
            System.out.println("来源: " + pair.getSourceType());
        }
        
        // 方式 3: 转换为 JSON
        System.out.println("\n=== JSON 格式 ===");
        String json = UnstructuredParser.toKeyValueJson(filePath);
        System.out.println(json);
    }
}
```

## ❓ 常见问题

**Q: 如何提高提取准确性？**

A: 
1. 在文档中使用明确的格式（推荐 Markdown 格式）
2. 设置置信度阈值过滤低质量结果
3. 确保键不要太长（建议不超过 50 个字符）

**Q: 支持哪些文件格式？**

A: 当前支持 DOC 和 DOCX 格式。

**Q: 如何处理提取失败的情况？**

A: 使用 try-catch 捕获异常：
```java
try {
    List<KeyValuePair> pairs = UnstructuredParser.extractKeyValuePairs("document.docx");
    // 处理结果
} catch (Exception e) {
    System.err.println("提取失败: " + e.getMessage());
}
```

## 📚 更多资源

- [完整使用说明](./使用说明.md)
- [K-V 功能详细说明](./K-V格式提取功能说明.md)
- [示例代码](./src/test/java/com/torchv/infra/unstructured/examples/KeyValueExtractionExample.java)
- [单元测试](./src/test/java/com/torchv/infra/unstructured/KeyValueExtractionTest.java)

---

**开始使用吧！** 🎉

