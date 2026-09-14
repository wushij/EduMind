package com.edumind.ai.rag.query;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueryRewriterTest {

    @Test
    void sanitizeRewriteOutput_shouldPrefixCourseWhenModelEchoesOriginalQuestion() {
        String original = "Java多态的具体实现原理是什么？它与方法重载有什么区别？";
        String echoed = "Java多态的具体实现原理是什么？它与方法重载有什么区别？";

        String result = QueryRewriter.sanitizeRewriteOutput(echoed, original, "Java程序设计");

        Assertions.assertTrue(result.startsWith("Java程序设计"));
        Assertions.assertTrue(result.contains("多态"));
    }

    @Test
    void sanitizeRewriteOutput_shouldKeepExplicitRewrite() {
        String rewritten = "Java程序设计课程中多态实现原理及与方法重载的区别";

        String result = QueryRewriter.sanitizeRewriteOutput(
                rewritten,
                "Java多态的具体实现原理是什么？",
                "Java程序设计"
        );

        Assertions.assertEquals(rewritten, result);
    }
}
