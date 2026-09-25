package com.edumind.statistics.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.mapper.WrongQuestionRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class WrongQuestionRecordDao {

    private final WrongQuestionRecordMapper wrongQuestionRecordMapper;

    public Page<WrongQuestionRecordEntity> pageByCourse(Page<WrongQuestionRecordEntity> page,
                                                        Long courseId, Long knowledgePointId) {
        LambdaQueryWrapper<WrongQuestionRecordEntity> wrapper = new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                .eq(knowledgePointId != null, WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                .orderByDesc(WrongQuestionRecordEntity::getWrongCount);
        return wrongQuestionRecordMapper.selectPage(page, wrapper);
    }

    public Page<WrongQuestionRecordEntity> pageByStudent(Page<WrongQuestionRecordEntity> page,
                                                         Long studentId, Long courseId) {
        return pageByStudent(page, studentId, courseId, null, null, 0);
    }

    public Page<WrongQuestionRecordEntity> pageByStudent(Page<WrongQuestionRecordEntity> page,
                                                         Long studentId, Long courseId,
                                                         Long knowledgePointId, String errorTypeCode,
                                                         Integer status) {
        LambdaQueryWrapper<WrongQuestionRecordEntity> wrapper = new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                .eq(knowledgePointId != null, WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                .eq(status != null, WrongQuestionRecordEntity::getStatus, status)
                .like(StringUtils.hasText(errorTypeCode),
                        WrongQuestionRecordEntity::getErrorTypes, errorTypeCode)
                .orderByDesc(WrongQuestionRecordEntity::getWrongCount);
        return wrongQuestionRecordMapper.selectPage(page, wrapper);
    }

    public long countByStudentCourseAndStatus(Long studentId, Long courseId, Integer status) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .eq(status != null, WrongQuestionRecordEntity::getStatus, status)
        );
    }

    public java.util.List<WrongQuestionRecordEntity> listActiveByStudentAndCourse(Long studentId, Long courseId, int limit) {
        return wrongQuestionRecordMapper.selectList(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .eq(WrongQuestionRecordEntity::getStatus, 0)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
                        .last(limit > 0 ? "LIMIT " + limit : "")
        );
    }

    public int insert(WrongQuestionRecordEntity entity) {
        return wrongQuestionRecordMapper.insert(entity);
    }

    public int updateById(WrongQuestionRecordEntity entity) {
        return wrongQuestionRecordMapper.updateById(entity);
    }

    /**
     * 显式更新归因结论与失分类型标签。
     * MyBatis-Plus 默认的更新策略会忽略 null 字段，若沿用 {@link #updateById} 传 errorTypes=null，
     * 旧的失分类型标签会残留在库里（例如「未作答」记录仍显示上一次的「审题不清」）。
     * 因此这里用 UpdateWrapper 显式 set，保证标签能被真正清空。
     */
    public int updateDiagnosisResult(Long id, String diagnosis, String errorTypes) {
        return wrongQuestionRecordMapper.update(null, new LambdaUpdateWrapper<WrongQuestionRecordEntity>()
                .eq(WrongQuestionRecordEntity::getId, id)
                .set(WrongQuestionRecordEntity::getDiagnosis, diagnosis)
                .set(WrongQuestionRecordEntity::getErrorTypes, errorTypes));
    }

    public WrongQuestionRecordEntity findById(Long id) {
        return wrongQuestionRecordMapper.selectById(id);
    }

    public WrongQuestionRecordEntity findByStudentAndQuestion(Long studentId, Long questionId) {
        return wrongQuestionRecordMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getQuestionId, questionId)
                        .last("LIMIT 1")
        );
    }

    public WrongQuestionRecordEntity findByStudentCourseAndKp(Long studentId, Long courseId, Long knowledgePointId) {
        return wrongQuestionRecordMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .eq(WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
                        .last("LIMIT 1")
        );
    }

    public long countByCourse(Long courseId) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
        );
    }

    public long countByCourseAndDateRange(Long courseId, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .ge(start != null, WrongQuestionRecordEntity::getCreateTime, start)
                        .le(end != null, WrongQuestionRecordEntity::getCreateTime, end)
        );
    }

    public long countByStudentAndCourse(Long studentId, Long courseId) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
        );
    }

    /**
     * 批量按学生统计某课程的错题记录条数（单次 GROUP BY 聚合）。
     *
     * <p>用于学生学情榜单等"课程 × 全部学生"场景，替代逐学生调用
     * {@link #countByStudentAndCourse(Long, Long)} 造成的 N+1 查询。</p>
     *
     * <p>口径与单学生版本完全一致（course_id + student_id 的记录条数）；
     * 特意不加载明细实体，避免把 lastStudentAnswer / diagnosis 等大字段拖进内存。</p>
     *
     * @param courseId   课程 ID
     * @param studentIds 学生 ID 集合
     * @return studentId -> 错题记录条数（无记录的学生不在结果中，由调用方取 0）
     */
    public java.util.Map<Long, Long> countGroupByStudentIds(Long courseId, java.util.Collection<Long> studentIds) {
        java.util.Set<Long> distinctStudentIds = studentIds == null
                ? java.util.Collections.emptySet()
                : studentIds.stream().filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (courseId == null || distinctStudentIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.List<java.util.Map<String, Object>> rows = wrongQuestionRecordMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<WrongQuestionRecordEntity>()
                        .select("student_id AS studentId", "COUNT(*) AS cnt")
                        .eq("course_id", courseId)
                        .in("student_id", distinctStudentIds)
                        .groupBy("student_id"));
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<Long, Long> result = new java.util.HashMap<>(rows.size());
        for (java.util.Map<String, Object> row : rows) {
            Long studentId = toLongKey(row.get("studentId"));
            if (studentId == null) {
                continue;
            }
            Object count = row.get("cnt");
            result.put(studentId, count instanceof Number countValue ? countValue.longValue() : 0L);
        }
        return result;
    }

    /**
     * 聚合结果中的主键转换：不同 JDBC 驱动可能返回 Long / BigInteger / String，
     * 统一做宽松解析，避免因类型断言过严而静默丢数据。
     */
    private static Long toLongKey(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public long countByKpAndCourse(Long knowledgePointId, Long courseId) {
        return wrongQuestionRecordMapper.selectCount(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
        );
    }

    public java.util.List<WrongQuestionRecordEntity> listByStudentAndCourse(Long studentId, Long courseId, int limit) {
        return wrongQuestionRecordMapper.selectList(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getStudentId, studentId)
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
                        .last(limit > 0 ? "LIMIT " + limit : "")
        );
    }

    public java.util.List<WrongQuestionRecordEntity> listByCourse(Long courseId) {
        return listByCourse(courseId, null);
    }

    public java.util.List<WrongQuestionRecordEntity> listByCourse(Long courseId, Long knowledgePointId) {
        return wrongQuestionRecordMapper.selectList(
                new LambdaQueryWrapper<WrongQuestionRecordEntity>()
                        .eq(WrongQuestionRecordEntity::getCourseId, courseId)
                        .eq(knowledgePointId != null, WrongQuestionRecordEntity::getKnowledgePointId, knowledgePointId)
                        .orderByDesc(WrongQuestionRecordEntity::getWrongCount)
        );
    }
}
