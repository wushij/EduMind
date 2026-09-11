package com.edumind.course.controller.member;

import com.edumind.common.api.ApiResult;
import com.edumind.course.dto.member.CourseMemberAddDTO;
import com.edumind.course.service.member.CourseMemberService;
import com.edumind.course.vo.member.CourseMemberVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/members")
@RequiredArgsConstructor
public class CourseMemberController {

    private final CourseMemberService courseMemberService;

    @SaCheckPermission("course:view")
    @GetMapping
    public ApiResult<List<CourseMemberVO>> listMembers(@PathVariable("courseId") Long courseId) {
        return ApiResult.success(courseMemberService.listMembers(courseId));
    }

    @SaCheckPermission("course:edit")
    @PostMapping
    public ApiResult<Long> addMember(@PathVariable("courseId") Long courseId,
                                     @Valid @RequestBody CourseMemberAddDTO dto) {
        return ApiResult.success(courseMemberService.addMember(courseId, dto));
    }
}
