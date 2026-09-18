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
}
