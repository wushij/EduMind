package com.edumind.statistics.vo.dashboard;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecentCourseVO {
    private Long id;
    private String name;
    private LocalDateTime lastVisitAt;
}
