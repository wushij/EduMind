package com.edumind.system.service.log;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.system.dto.log.SysOperLogPageQueryDTO;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.vo.log.SysOperLogStatsVO;
import com.edumind.system.vo.log.SysOperLogVO;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface SysOperLogService {

    /**
     * 分页查询操作日志列表
     */
    Page<SysOperLogVO> page(SysOperLogPageQueryDTO query);

    /**
     * 根据ID获取操作日志详情
     */
    SysOperLogVO getById(Long id);

    /**
     * 异步记录操作日志落库
     */
    void recordLog(SysOperLogEntity entity);

    /**
     * 删除单条日志
     */
    void delete(Long id);

    /**
     * 批量删除日志
     */
    void batchDelete(List<Long> ids);

    /**
     * 清空当前租户下所有操作日志
     */
    void clean();

    /**
     * 获取指定用户的最近操作日志（供个人画像/用户详情页联动展示）
     */
    List<SysOperLogVO> listRecentByUser(Long userId, int limit);

    /**
     * 获取操作日志统计概览看板数据
     */
    SysOperLogStatsVO getStats();
}
