package com.edumind.knowledge.service.chunk;

import com.edumind.knowledge.config.ChunkProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChunkSplitterTest {

    @Test
    void split_avoidsOverlapMidWord_fragmentLikeEngineSemicolon() {
        ChunkProperties props = new ChunkProperties();
        props.setSize(220);
        props.setOverlap(80);
        ChunkSplitter splitter = new ChunkSplitter(props);

        StringBuilder sb = new StringBuilder();
        sb.append("铺垫。".repeat(40));
        sb.append("JVM属于 JRE的核心执行引擎；\n- JRE通常包含在 JDK中。\n\n");
        sb.append("需要特别强调的是，Java程序并不是直接把 .java文件交给操作系统执行。");
        sb.append("它先被编译成字节码，再由 JVM负责加载和执行。\n\n");
        sb.append("### 2.2 四个关键词的一句话定义\n\n- JDK：开发工具包。");

        List<ChunkSplitter.SplitChunk> chunks = splitter.split(sb.toString());
        for (ChunkSplitter.SplitChunk chunk : chunks) {
            String c = chunk.getContent();
            assertFalse(c.startsWith("擎"), () -> c);
            assertFalse(c.startsWith("擎；"), () -> c);
            assertFalse(c.matches("^[,，;；].*"), () -> c);
        }
        boolean hasJreBullet = chunks.stream().anyMatch(ch -> ch.getContent().contains("- JRE通常包含在 JDK中"));
        assertTrue(hasJreBullet);
    }

    @Test
    void split_lessonMarkdownWithHeadingsAndLists_completesPromptly() {
        ChunkProperties props = new ChunkProperties();
        props.setSize(800);
        props.setOverlap(128);
        ChunkSplitter splitter = new ChunkSplitter(props);

        String sample = """
- 能够描述 javac将 `.java`源文件编译为 `.class`字节码文件的主要流程，并指出编译期与运行期的分界。
- 能够辨析 `.java`、`.class`、字节码与 JVM之间的关系，说明类文件在跨平台执行中的载体角色。
- 能够说出类加载的核心阶段（加载、链接、初始化）与双亲委派机制的基本意图。
- 能够分析 main方法执行链路及常见启动异常（如 ClassNotFoundException、NoClassDefFoundError）的发生期。

# 课节正文

## 一、从源代码到控制台输出：一条必须走通的主线

在日常写 Java代码时，我们最习惯的动作是敲下 `public static void main`，然后在终端或控制台看到打印出 `Hello, World!`。但在这个过程中，实际经历了三次不同边界的接力：

- **编译期**：由 JDK中的 javac负责，把 `Hello.java`变成 `Hello.class`字节码，此时与 JVM无关；
- **加载与链接期**：JVM在操作系统拉起进程后，把 `Hello.class`读入内存，完成校验、准备与符号解析；
- **执行期**：执行引擎找到 main方法入口，边解释边即时编译（JIT）字节码并执行。

分水岭非常清晰：编译期发生在开发者的机器或 CI流水线上，执行期发生在目标 JVM内部。只有把这三段完全拆开，当遇到诸如 `NoClassDefFoundError`、跨平台乱码、静态块初始化死锁等疑难问题时，才能一眼看穿问题处于哪一阶段。

## 二、编译期深潜：javac到底把 .java变成了什么

### 2.1 编译的本质与产物
我们先写一个最小示例。准备一个简单的源文件 `Hello.java`：

```java
public class Hello {
    public static void main(String[] args) {
        int a = 3;
        int b = 5;
        int sum = a + b;
        System.out.println("sum = " + sum);
    }
}
```

在终端执行：

```bash
javac Hello.java
```

此时同目录下会出现 `Hello.class`。这个文件既不是操作系统可以直接执行的二进制机器码，也不是纯文本，而是一种紧凑的**跨平台结构化字节码**。
""";

        List<ChunkSplitter.SplitChunk> chunks = splitter.split(sample);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
    }

    @Test
    void split_mermaidFlowchart_notCutInHalf() {
        ChunkProperties props = new ChunkProperties();
        props.setSize(800);
        props.setOverlap(128);
        ChunkSplitter splitter = new ChunkSplitter(props);

        String sample = """
## 🗺️ 2. Java 教程与全景架构概览 架构演进与核心分层

```mermaid
flowchart TD
  classDef tier1 fill:#1e293b,stroke:#38bdf8,stroke-width:2px,color:#f8fafc;
  classDef tier2 fill:#0f172a,stroke:#d4a853,stroke-width:3px,color:#d4a853,font-weight:bold;
  classDef tier3 fill:#1e293b,stroke:#34d399,stroke-width:2px,color:#f8fafc;

  subgraph Tier1["第一阶段：底层原理与核心认知 📋"]
    T1_1["Java 语言与 JVM 规范全景 🏗️"]:::tier1
    T1_2["强类型系统与对象模型 🔍"]:::tier1
    T1_3["JDK / JRE / JVM 三层基石 📦"]:::tier1
  end

  subgraph Tier2["第二阶段：生产机制与架构防御 ⚡"]
    T2_1["面向对象四大支柱封装 🛡️"]:::tier2
    T2_2["CodeLedger 账务领域建模 🧱"]:::tier2
    T2_3["标准库优先工程选型 ⚙️"]:::tier2
  end

  subgraph Tier3["第三阶段：工程落地与交付验证 🚀"]
    T3_1["跨平台字节码解释执行 🔄"]:::tier3
    T3_2["JIT 即时编译性能优化 ⚡"]:::tier3
    T3_3["工业级高可用企业系统交付 🚀"]:::tier3
  end

  T1_1 --> T1_2 --> T1_3
  T1_3 ==>|核心逻辑驱动| T2_1
  T2_1 --> T2_2 --> T2_3
  T2_3 ==>|生产验证落地| T3_1
  T3_1 --> T3_2 --> T3_3
```

---

## 📊 3. 技术选型与关键维度权衡

### 3.1 核心选型决策矩阵
""";

        List<ChunkSplitter.SplitChunk> chunks = splitter.split(sample);
        // 查找包含 mermaid 的 chunk
        ChunkSplitter.SplitChunk mermaidChunk = chunks.stream()
                .filter(ch -> ch.getContent().contains("```mermaid"))
                .findFirst()
                .orElse(null);

        org.junit.jupiter.api.Assertions.assertNotNull(mermaidChunk);
        // 验证该 chunk 包含了完整的 mermaid 闭合，并且没有被切成两段
        assertTrue(mermaidChunk.getContent().contains("T3_1 --> T3_2 --> T3_3"));
        assertTrue(mermaidChunk.getContent().contains("```\n") || mermaidChunk.getContent().endsWith("```"));
    }

    @Test
    void split_fullFile_001_238_2248() throws Exception {
        java.io.File file = new java.io.File("E:/Code Compass/sql/topic-import/java-214/001_238_2248.md");
        if (!file.exists()) {
            return;
        }
        String content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        ChunkProperties props = new ChunkProperties();
        props.setSize(800);
        props.setOverlap(128);
        ChunkSplitter splitter = new ChunkSplitter(props);
        List<ChunkSplitter.SplitChunk> chunks = splitter.split(content);

        assertFalse(chunks.isEmpty());
        for (ChunkSplitter.SplitChunk chunk : chunks) {
            // 验证绝不能出现只有分割线（如 ---）的空白碎片
            assertFalse(chunk.getContent().trim().equals("---"), () -> "Found dummy chunk with only --- at index " + chunk.getChunkIndex());
            // 每一个 chunk 内部的 ``` 数量必须为偶数（代码块必须合法闭合）
            int fenceCount = 0;
            int pos = 0;
            while ((pos = chunk.getContent().indexOf("```", pos)) != -1) {
                fenceCount++;
                pos += 3;
            }
            assertTrue(fenceCount % 2 == 0, () -> "Chunk " + chunk.getChunkIndex() + " has unclosed code fence! Content:\n" + chunk.getContent());
        }
    }

    @Test
    void split_doc_653() throws Exception {
        java.io.File file = new java.io.File("E:/EduMind/doc_653_utf8.md");
        if (!file.exists()) return;
        String content = java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        if (content.startsWith("content\n")) {
            content = content.substring("content\n".length());
        }
        ChunkProperties props = new ChunkProperties();
        props.setSize(800);
        props.setOverlap(128);
        ChunkSplitter splitter = new ChunkSplitter(props);
        List<ChunkSplitter.SplitChunk> chunks = splitter.split(content);
        assertFalse(chunks.isEmpty());
        // 验证三、.class文件 独立成块，且包含完整的 3.1 和 3.2，不与 2.4 混杂
        ChunkSplitter.SplitChunk classChunk = chunks.stream()
                .filter(c -> c.getContent().contains(".class文件：跨平台契约的载体"))
                .findFirst().orElse(null);
        org.junit.jupiter.api.Assertions.assertNotNull(classChunk);
        assertTrue(classChunk.getContent().contains("### 3.1"));
        assertTrue(classChunk.getContent().contains("### 3.2"));
        assertFalse(classChunk.getContent().contains("2.4"));
    }
}
