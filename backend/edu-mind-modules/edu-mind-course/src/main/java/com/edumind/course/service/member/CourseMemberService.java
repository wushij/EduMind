package com.edumind.course.service.member;

import com.edumind.course.dto.member.CourseMemberAddDTO;
import com.edumind.course.vo.member.CourseMemberCandidateVO;
import com.edumind.course.vo.member.CourseMemberVO;

import java.util.List;

public interface CourseMemberService {

    List<CourseMemberVO> listMembers(Long courseId);

    List<CourseMemberCandidateVO> searchCandidates(Long courseId, String keyword);

    Long addMember(Long courseId, CourseMemberAddDTO dto);

    void removeMember(Long courseId, Long userId);

    Long joinCourse(Long courseId, Long userId);
}
