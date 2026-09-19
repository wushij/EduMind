package com.edumind.question.support;

import com.edumind.common.exception.BusinessException;
import com.edumind.question.util.QuestionStemValidator;

public final class QuestionStemGuard {

    private QuestionStemGuard() {
    }

    public static void assertValidStem(String stem) {
        if (stem == null || stem.isBlank()) {
            throw new BusinessException("题干不能为空");
        }
        if (QuestionStemValidator.isGarbageStem(stem)) {
            throw new BusinessException("题干无效：检测到 AI 提示词或测试占位内容，请重新生成或编辑后再保存");
        }
    }
}
