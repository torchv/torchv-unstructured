# TorchV Unstructured - K-V 格式提取功能说明

## 📋 功能概述

**K-V 格式提取**是 TorchV Unstructured 1.0.0 版本新增的核心功能，专为 RAG（检索增强生成）应用优化。该功能能够智能地从文档中提取键值对（Key-Value Pairs）信息，将文档内容原子化为独立的语义单元，使信息更清晰、更易于检索和匹配。

## 🎯 什么是 K-V 格式？

**K-V 格式**（Key-Value 格式）是一种简洁、结构化的数据表示方式，由"键"和"值"组成：

```
键（Key）: 值（Value）
```

### 常见应用场景

1. **产品参数说明**
   ```
   最大输入电压: 24V
   工作温度: -20℃ ~ 60℃
   功率: 100W
   ```

2. **配置文件**
   ```
   timeout = 30秒
   max_connections = 100
   enable_cache = true
   ```

3. **员工信息**
   ```
   - **姓名**: 张三
   - **部门**: 技术研发部
   - **入职日期**: 2023-05-15
   ```

4. **FAQ 问答**
   ```
   Q: 忘记密码怎么办？
   A: 点击登录页"找回密码"，通过邮箱重置。
   ```

## 🚀 快速开始

### 1. 基础用法

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import java.util.List;

// 从文档中提取键值对
List<KeyValuePair> kvPairs = UnstructuredParser.extractKeyValuePairs("document.docx");

// 打印所有键值对
for (KeyValuePair pair : kvPairs) {
    System.out.println(pair.getKey() + ": " + pair.getValue());
}
```

### 2. 转换为不同格式

```java
// Markdown 格式
String markdown = UnstructuredParser.toKeyValueMarkdown("document.docx");
System.out.println(markdown);

// 纯文本格式
String text = UnstructuredParser.toKeyValueText("document.docx");
System.out.println(text);

// JSON 格式
String json = UnstructuredParser.toKeyValueJson("document.docx");
System.out.println(json);
```

## 📖 支持的格式

TorchV Unstructured 能够识别以下多种 K-V 格式：

### 1. 冒号分隔（Colon Separated）

```
产品名称: TorchV Unstructured
版本: 1.0.0
作者: xiaoymin
许可证: Apache 2.0
```

**置信度**: 0.8

### 2. Markdown 格式

```
- **最大输入电压**: 24V
- **工作温度**: -20℃ ~ 60℃
- **功率**: 100W
```

**置信度**: 0.95

### 3. 等号分隔（Equals Separated）

```
timeout = 30秒
max_connections = 100
enable_cache = true
```

**置信度**: 0.85

### 4. 问答格式（Q&A Format）

```
Q: 忘记密码怎么办？ A: 点击登录页"找回密码"，通过邮箱重置。
Q: 支持哪些支付方式？ A: 微信、支付宝、银联。
```

**置信度**: 0.9

### 5. 表格格式（Table Format）

文档中的表格会自动被识别，假设第一列是键，第二列是值：

| 属性 | 值 |
|------|-----|
| 产品名称 | TorchV Unstructured |
| 版本 | 1.0.0 |
| 作者 | xiaoymin |

**置信度**: 0.9

## 🔍 KeyValuePair 数据结构

```java
public class KeyValuePair {
    private String key;                    // 键（属性名称或问题）
    private String value;                  // 值（属性值或答案）
    private double confidence;             // 置信度（0.0-1.0）
    private SourceType sourceType;         // 来源类型
    private int position;                  // 在文档中的位置
    private String metadata;               // 附加元数据
}
```

### 来源类型（SourceType）

- `PARAGRAPH` - 从段落文本中提取
- `TABLE` - 从表格中提取
- `LIST` - 从列表中提取
- `HEADING` - 从标题中提取
- `OTHER` - 其他来源

## 🎨 高级用法

### 1. 过滤键值对

```java
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;

List<KeyValuePair> allPairs = UnstructuredParser.extractKeyValuePairs("document.docx");

// 按置信度过滤（只保留高置信度的）
List<KeyValuePair> highConfidence = KeyValueExtractor.filterByConfidence(allPairs, 0.8);

// 按来源类型过滤（只保留来自表格的）
List<KeyValuePair> fromTable = KeyValueExtractor.filterBySourceType(
    allPairs, 
    KeyValuePair.SourceType.TABLE
);
```

### 2. 自定义输出格式

```java
List<KeyValuePair> pairs = UnstructuredParser.extractKeyValuePairs("document.docx");

// 转换为 Markdown（带置信度）
for (KeyValuePair pair : pairs) {
    System.out.println(pair.toMarkdownWithConfidence());
}
// 输出: - **产品名称**: TorchV Unstructured `(置信度: 0.95)`
```

## 💡 RAG 应用场景

### 为什么 K-V 格式对 RAG 有帮助？

1. **信息原子化**：每条只讲一件事，避免无关信息干扰
2. **关键词明确**：Key 本身就是语义标签（如"工作温度"），容易匹配用户问题
3. **减少噪声**：不像大段文字包含冗余描述
4. **便于向量化**：每个 K-V 可作为独立 chunk 生成 embedding，提升检索精度

### RAG 集成示例

```java
import com.torchv.infra.unstructured.UnstructuredParser;
import com.torchv.infra.unstructured.core.KeyValuePair;
import com.torchv.infra.unstructured.extractor.KeyValueExtractor;
import java.util.List;

/**
 * RAG 知识库构建器 - 使用 K-V 格式
 */
public class RAGKnowledgeBaseBuilder {

    /**
     * 处理文档并构建知识库
     */
    public void buildKnowledgeBase(String filePath) {
        // 1. 提取键值对
        List<KeyValuePair> kvPairs = UnstructuredParser.extractKeyValuePairs(filePath);

        // 2. 过滤低置信度的键值对
        List<KeyValuePair> highQualityPairs = KeyValueExtractor.filterByConfidence(kvPairs, 0.8);

        System.out.println("提取到 " + highQualityPairs.size() + " 个高质量键值对");

        // 3. 为每个键值对生成向量并存储
        for (KeyValuePair pair : highQualityPairs) {
            // 构建用于向量化的文本
            String textForEmbedding = pair.getKey() + ": " + pair.getValue();

            // 生成向量（使用你的向量化模型，如 OpenAI Embeddings）
            // float[] embedding = embeddingModel.encode(textForEmbedding);

            // 存储到向量数据库（如 Pinecone, Weaviate, Milvus 等）
            // vectorDB.upsert(
            //     id: generateId(pair),
            //     vector: embedding,
            //     metadata: {
            //         key: pair.getKey(),
            //         value: pair.getValue(),
            //         source: filePath,
            //         confidence: pair.getConfidence(),
            //         sourceType: pair.getSourceType()
            //     }
            // );

            System.out.println("已索引: " + textForEmbedding);
        }
    }

    /**
     * 检索相关信息
     */
    public String retrieveAnswer(String question) {
        // 1. 将问题向量化
        // float[] questionEmbedding = embeddingModel.encode(question);

        // 2. 在向量数据库中搜索最相似的键值对
        // List<SearchResult> results = vectorDB.search(questionEmbedding, topK: 5);

        // 3. 提取最相关的答案
        // for (SearchResult result : results) {
        //     if (result.getScore() > 0.8) {
        //         return result.getMetadata().get("value");
        //     }
        // }

        return "未找到相关信息";
    }
}
```

### 实际应用示例

**场景 1：产品手册问答系统**

文档内容：
```
- **产品名称**: 智能温控器 X100
- **最大输入电压**: 24V
- **工作温度**: -20℃ ~ 60℃
- **功率**: 100W
```

用户问题：
```
"这个温控器的工作温度范围是多少？"
```

系统响应：
```
工作温度: -20℃ ~ 60℃
```

**场景 2：员工信息查询**

文档内容：
```
- **姓名**: 张三
- **部门**: 技术研发部
- **职位**: 高级工程师
- **入职日期**: 2023-05-15
```

用户问题：
```
"张三什么时候入职的？"
```

系统响应：
```
入职日期: 2023-05-15
```

**场景 3：FAQ 自动问答**

文档内容：
```
Q: 忘记密码怎么办？ A: 点击登录页"找回密码"，通过邮箱重置。
Q: 支持哪些支付方式？ A: 微信、支付宝、银联。
Q: 如何申请退款？ A: 在订单详情页点击"申请退款"按钮。
```

用户问题：
```
"我忘记密码了怎么办？"
```

系统响应：
```
点击登录页"找回密码"，通过邮箱重置。
```

## 🔧 API 参考

### UnstructuredParser

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `extractKeyValuePairs(String filePath)` | 提取键值对 | `List<KeyValuePair>` |
| `toKeyValueMarkdown(String filePath)` | 转为 Markdown 格式 | `String` |
| `toKeyValueText(String filePath)` | 转为纯文本格式 | `String` |
| `toKeyValueJson(String filePath)` | 转为 JSON 格式 | `String` |

### KeyValueExtractor

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `extractFromText(String text)` | 从文本提取键值对 | `List<KeyValuePair>` |
| `extractFromTable(String tableHtml)` | 从表格提取键值对 | `List<KeyValuePair>` |
| `toMarkdown(List<KeyValuePair> pairs)` | 转为 Markdown | `String` |
| `toPlainText(List<KeyValuePair> pairs)` | 转为纯文本 | `String` |
| `toJsonArray(List<KeyValuePair> pairs)` | 转为 JSON 数组 | `String` |
| `filterByConfidence(List<KeyValuePair> pairs, double minConfidence)` | 按置信度过滤 | `List<KeyValuePair>` |
| `filterBySourceType(List<KeyValuePair> pairs, SourceType type)` | 按来源类型过滤 | `List<KeyValuePair>` |

### KeyValuePair

| 方法 | 说明 | 返回类型 |
|------|------|----------|
| `toMarkdown()` | 转为 Markdown 格式 | `String` |
| `toPlainText()` | 转为纯文本格式 | `String` |
| `toJson()` | 转为 JSON 格式 | `String` |
| `toMarkdownWithConfidence()` | 转为带置信度的 Markdown | `String` |
| `isValid()` | 检查是否有效 | `boolean` |

## 📊 性能建议

1. **批量处理**：如果需要处理多个文档，建议使用批量处理以提高效率
2. **置信度阈值**：根据应用场景调整置信度阈值（建议 0.8 以上）
3. **缓存结果**：对于相同文档，可以缓存提取结果避免重复处理
4. **并行处理**：大量文档可以使用并行流处理

## ❓ 常见问题

**Q: 如何提高提取的准确性？**

A:
1. 确保文档格式规范
2. 使用明确的键值对格式（如 Markdown 格式）
3. 设置合适的置信度阈值过滤低质量结果

**Q: 支持自定义 K-V 格式吗？**

A: 当前版本支持常见的 5 种格式。如需自定义格式，可以使用 `KeyValueExtractor.extractFromText()` 方法并自行解析。

**Q: 如何处理复杂的表格？**

A: 对于复杂表格（多列、嵌套等），建议先使用 `extractTables()` 提取表格，然后根据业务逻辑自定义解析。

**Q: K-V 提取支持哪些文件格式？**

A: 当前支持 DOC 和 DOCX 格式。未来版本将支持 PDF、PPT 等更多格式。

## 🎉 总结

K-V 格式提取功能是 TorchV Unstructured 为 RAG 应用量身打造的核心特性，能够：

✅ 智能识别多种 K-V 格式
✅ 提供置信度评分
✅ 支持多种输出格式
✅ 灵活的过滤和筛选
✅ 完美集成 RAG 工作流

立即开始使用，让您的 RAG 应用更加智能和高效！

---

**相关资源：**

- [完整使用说明文档](./使用说明.md)
- [示例代码](./src/test/java/com/torchv/infra/unstructured/examples/KeyValueExtractionExample.java)
- [单元测试](./src/test/java/com/torchv/infra/unstructured/KeyValueExtractionTest.java)


