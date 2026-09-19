package com.edumind.course.vo.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 课程成员录入时的用户候选项（按用户名 / 昵称搜索）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMemberCandidateVO implements Serializable {

    private Long userId;
    private String username;
    private String realName;
    private String avatar;
}
