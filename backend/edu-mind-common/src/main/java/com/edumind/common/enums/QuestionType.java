package com.edumind.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {
    SINGLE_CHOICE("SINGLE_CHOICE"),
    MULTIPLE_CHOICE("MULTIPLE_CHOICE"),
    TRUE_FALSE("TRUE_FALSE"),
    FILL_BLANK("FILL_BLANK"),
    SHORT_ANSWER("SHORT_ANSWER");

    private final String code;
}
