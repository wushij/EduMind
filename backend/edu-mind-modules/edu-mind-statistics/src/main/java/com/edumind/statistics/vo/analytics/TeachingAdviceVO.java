package com.edumind.statistics.vo.analytics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TeachingAdviceVO {
    private String summary;
    private List<String> actions = new ArrayList<>();
}
