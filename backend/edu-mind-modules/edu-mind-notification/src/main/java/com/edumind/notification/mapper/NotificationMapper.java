package com.edumind.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.notification.entity.NotificationEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationEntity> {

    /**
     * 多行 VALUES 批量插入（单条 SQL，参与当前事务），用于替代循环内逐条 insert。
     * <p>
     * 说明：
     * 1. tenant_id 显式列出，租户拦截器不再补列；为空时回退列默认值 1，避免 NOT NULL 报错；
     * 2. priority / is_read 为 NOT NULL 列，插入前做空值兜底；
     * 3. create_time 不列出，交由数据库 DEFAULT CURRENT_TIMESTAMP 填充，与单条 insert 行为一致；
     * 4. 开启 useGeneratedKeys，把自增主键回填到每个实体 id，保证推送载荷含 id。
     */
    @Insert("<script>"
            + "INSERT INTO sys_notification (tenant_id, user_id, title, content, type, ref_id, priority, is_read) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(IFNULL(#{item.tenantId}, 1), #{item.userId}, #{item.title}, #{item.content}, #{item.type}, "
            + "#{item.refId}, IFNULL(#{item.priority}, 0), IFNULL(#{item.isRead}, 0))"
            + "</foreach>"
            + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(@Param("list") List<NotificationEntity> list);
}
