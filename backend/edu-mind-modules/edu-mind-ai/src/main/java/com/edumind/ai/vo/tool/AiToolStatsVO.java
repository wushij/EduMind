package com.edumind.ai.vo.tool;

import lombok.Data;

@Data
public class AiToolStatsVO {
    private int total;
    private int online;
    private int offline;
    private int teacherCount;
    private int studentCount;
    private int generalCount;
    private long totalUseCount;
}
