package com.edumind.ai.vo;

import lombok.Data;

import java.util.List;

@Data
public class MessageDeleteResultVO {
    private List<String> deletedIds;
}
