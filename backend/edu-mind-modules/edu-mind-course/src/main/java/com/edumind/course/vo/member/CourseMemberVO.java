package com.edumind.course.vo.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMemberVO implements Serializable {

    private Long id;
    private Long courseId;
    private Long userId;
    private String username;
    private String realName;
    private String memberRole;
}
