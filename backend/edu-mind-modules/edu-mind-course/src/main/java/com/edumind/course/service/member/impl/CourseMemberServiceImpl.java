package com.edumind.course.service.member.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.CourseMemberDao;
import com.edumind.course.dto.member.CourseMemberAddDTO;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.CourseMemberEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.member.CourseMemberService;
import com.edumind.course.vo.member.CourseMemberCandidateVO;
import com.edumind.course.vo.member.CourseMemberVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseMemberServiceImpl implements CourseMemberService {

    private final CourseDao courseDao;
    private final CourseMemberDao courseMemberDao;
    private final UserQueryApi userQueryApi;
    private final CourseAccessService courseAccessService;

    @Override
    public List<CourseMemberVO> listMembers(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanView(course);
        List<CourseMemberEntity> members = courseMemberDao.findByCourseId(courseId);
        // 批量补全成员用户信息，避免逐成员跨模块查询（每人 3 次 DB）造成 N+1
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(
                members.stream().map(CourseMemberEntity::getUserId).collect(Collectors.toList()));
        return members.stream()
                .map(member -> toMemberVO(member, userMap.get(member.getUserId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseMemberCandidateVO> searchCandidates(Long courseId, String keyword) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        if (!StringUtils.hasText(keyword) || keyword.trim().length() < 1) {
            return List.of();
        }
        String kw = keyword.trim();
        List<Long> matchedIds = userQueryApi.findUserIdsByKeyword(kw);
        if (matchedIds.isEmpty()) {
            return List.of();
        }
        Long tenantId = course.getTenantId();
        if (tenantId != null && tenantId > 0) {
            Set<Long> tenantUserIds = new HashSet<>(userQueryApi.listActiveUserIdsByTenantId(tenantId));
            matchedIds = matchedIds.stream()
                    .filter(tenantUserIds::contains)
                    .limit(20)
                    .collect(Collectors.toList());
        } else {
            matchedIds = matchedIds.stream().limit(20).collect(Collectors.toList());
        }
        Set<Long> existingMemberUserIds = courseMemberDao.findByCourseId(courseId).stream()
                .map(CourseMemberEntity::getUserId)
                .collect(Collectors.toSet());
        List<Long> candidateIds = matchedIds.stream()
                .filter(id -> !existingMemberUserIds.contains(id))
                .collect(Collectors.toList());
        // 批量查询候选用户，避免逐个跨模块调用
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(candidateIds);
        return candidateIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(user -> CourseMemberCandidateVO.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .avatar(user.getAvatar())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Long addMember(Long courseId, CourseMemberAddDTO dto) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        if (courseMemberDao.findByCourseIdAndUserId(courseId, dto.getUserId()) != null) {
            throw new BusinessException("该用户已是课程成员");
        }
        UserBriefVO user = userQueryApi.getUserById(dto.getUserId());
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

    @Override
    public void removeMember(Long courseId, Long userId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        courseMemberDao.deleteByCourseIdAndUserId(courseId, userId);
    }

    @Override
    public Long joinCourse(Long courseId, Long userId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (courseMemberDao.findByCourseIdAndUserId(courseId, userId) != null) {
            throw new BusinessException("您已经是该课程成员");
        }
        CourseMemberEntity entity = new CourseMemberEntity();
        entity.setCourseId(courseId);
        entity.setUserId(userId);
        entity.setMemberRole("STUDENT");
        courseMemberDao.insert(entity);
        return entity.getId();
    }

    private CourseMemberVO toMemberVO(CourseMemberEntity entity, UserBriefVO user) {
        return CourseMemberVO.builder()
                .id(entity.getId())
                .courseId(entity.getCourseId())
                .userId(entity.getUserId())
                .username(user != null ? user.getUsername() : null)
                .realName(user != null ? user.getRealName() : null)
                .avatar(user != null ? user.getAvatar() : null)
                .memberRole(entity.getMemberRole())
                .build();
    }

}
