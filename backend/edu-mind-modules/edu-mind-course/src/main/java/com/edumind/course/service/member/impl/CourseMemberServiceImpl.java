package com.edumind.course.service.member.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.enums.RoleCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dto.member.CourseMemberAddDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.course.service.member.CourseMemberService;
import com.edumind.course.vo.member.CourseMemberVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseMemberServiceImpl implements CourseMemberService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final UserQueryApi userQueryApi;

    @Override
    public List<CourseMemberVO> listMembers(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseManageable(course);
        return courseMemberDao.findByCourseId(courseId).stream()
                .map(this::toMemberVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long addMember(Long courseId, CourseMemberAddDTO dto) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        assertCourseManageable(course);
        if (courseMemberDao.findByCourseIdAndUserId(courseId, dto.getUserId()) != null) {
            throw new BusinessException("该用户已是课程成员");
        }
        UserVO user = (UserVO) userQueryApi.getUserById(dto.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        CourseMemberEntity entity = new CourseMemberEntity();
        entity.setCourseId(courseId);
        entity.setUserId(dto.getUserId());
        entity.setMemberRole(dto.getRole());
        courseMemberDao.insert(entity);
        return entity.getId();
    }

    private CourseMemberVO toMemberVO(CourseMemberEntity entity) {
        UserVO user = (UserVO) userQueryApi.getUserById(entity.getUserId());
        return CourseMemberVO.builder()
                .id(entity.getId())
                .courseId(entity.getCourseId())
                .userId(entity.getUserId())
                .username(user != null ? user.getUsername() : null)
                .realName(user != null ? user.getRealName() : null)
                .memberRole(entity.getMemberRole())
                .build();
    }

    private void assertCourseManageable(CourseEntity course) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        List<String> roles = userQueryApi.getRolesByUserId(currentUserId);
        if (roles.contains(RoleCode.ADMIN.getCode())) {
            return;
        }
        if (roles.contains(RoleCode.TEACHER.getCode()) && currentUserId.equals(course.getTeacherId())) {
            return;
        }
        throw new BusinessException("无权限管理课程成员");
    }
}
