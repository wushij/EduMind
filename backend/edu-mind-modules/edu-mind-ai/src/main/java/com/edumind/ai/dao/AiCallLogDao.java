package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.context.TenantContext;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AiCallLogDao {

    private final AiCallLogMapper aiCallLogMapper;

    public AiCallLogEntity findById(Long id) {
        return aiCallLogMapper.selectById(id);
    }

    public int insert(AiCallLogEntity entity) {
        if (entity.getTenantId() == null && TenantContext.getTenantId() != null && TenantContext.getTenantId() > 0) {
            entity.setTenantId(TenantContext.getTenantId());
        }
        return aiCallLogMapper.insert(entity);
    }

    public Page<AiCallLogEntity> page(Page<AiCallLogEntity> page, LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectPage(page, wrapper);
    }

    public List<AiCallLogEntity> list(LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectList(wrapper);
    }

    public long count(LambdaQueryWrapper<AiCallLogEntity> wrapper) {
        return aiCallLogMapper.selectCount(wrapper);
    }

    /** 按课程分组统计调用次数（单次 GROUP BY 聚合），用于替代逐课程 count 查询 */
    public Map<Long, Long> countGroupByCourseIds(Collection<Long> courseIds, LocalDateTime since) {
        Set<Long> distinctCourseIds = courseIds == null
                ? Collections.emptySet()
                : courseIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinctCourseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<AiCallLogEntity> wrapper = new QueryWrapper<AiCallLogEntity>()
                .select("course_id AS courseId", "COUNT(*) AS cnt")
                .in("course_id", distinctCourseIds)
                .groupBy("course_id");
        if (since != null) {
            wrapper.ge("create_time", since);
        }
        List<Map<String, Object>> rows = aiCallLogMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object courseId = row.get("courseId");
            Object count = row.get("cnt");
            if (courseId instanceof Number idValue && count instanceof Number countValue) {
                result.put(idValue.longValue(), countValue.longValue());
            }
        }
        return result;
    }

    /**
     * 按用户分组统计某课程时间窗内的调用次数（单次 GROUP BY 聚合）。
     *
     * <p>用于学生学情榜单等"课程 × 全部学生"场景，替代逐学生调用
     * {@code countCallsByCourseAndUser} 造成的 N+1 查询。</p>
     *
     * <p>过滤条件与单用户版本严格一致：course_id + user_id + create_time &gt;= since，
     * 因此同一批用户的聚合结果与逐个 count 完全等价。</p>
     *
     * @param courseId 课程 ID，为 null 表示不限定课程
     * @param userIds  用户 ID 集合；为空集合表示无统计目标
     * @param since    起始时间，可为 null
     * @return userId -> 调用次数（未出现的用户不在结果中）
     */
    public Map<Long, Long> countGroupByUserIds(Long courseId, Collection<Long> userIds, LocalDateTime since) {
        Set<Long> distinctUserIds = userIds == null
                ? Collections.emptySet()
                : userIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (distinctUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper<AiCallLogEntity> wrapper = new QueryWrapper<AiCallLogEntity>()
                .select("user_id AS userId", "COUNT(*) AS cnt")
                .in("user_id", distinctUserIds)
                .groupBy("user_id");
        if (courseId != null) {
            wrapper.eq("course_id", courseId);
        }
        if (since != null) {
            wrapper.ge("create_time", since);
        }
        List<Map<String, Object>> rows = aiCallLogMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Long> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Long userId = toLongKey(row.get("userId"));
            if (userId == null) {
                continue;
            }
            Object count = row.get("cnt");
            result.put(userId, count instanceof Number countValue ? countValue.longValue() : 0L);
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

    /**
     * 按调用场景分组统计次数（单次 GROUP BY 聚合）。
     *
     * <p>scene 在历史写入方存在大小写混用（CHAT / chat / QUESTION_GEN / question_generate），
     * 因此统一用 {@code UPPER(TRIM(scene))} 归一后再分组；scene 为空的历史日志归入 UNKNOWN，
     * 保证 SUM(结果) == 该课程时间窗内的真实调用总数。</p>
     *
     * @param courseId 课程 ID，为 null 表示不限定课程（全局统计）
     * @param since    起始时间，可为 null
     * @return scene（已大写归一） -> 调用次数
     */
    public Map<String, Long> countGroupByScene(Long courseId, LocalDateTime since) {
        QueryWrapper<AiCallLogEntity> wrapper = new QueryWrapper<AiCallLogEntity>()
                .select("COALESCE(UPPER(TRIM(scene)), 'UNKNOWN') AS scene", "COUNT(*) AS cnt")
                .groupBy("COALESCE(UPPER(TRIM(scene)), 'UNKNOWN')");
        if (courseId != null) {
            wrapper.eq("course_id", courseId);
        }
        if (since != null) {
            wrapper.ge("create_time", since);
        }
        List<Map<String, Object>> rows = aiCallLogMapper.selectMaps(wrapper);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            Object scene = row.get("scene");
            Object count = row.get("cnt");
            if (scene != null && count instanceof Number countValue) {
                result.merge(scene.toString().toUpperCase(), countValue.longValue(), Long::sum);
            }
        }
        return result;
    }
}
