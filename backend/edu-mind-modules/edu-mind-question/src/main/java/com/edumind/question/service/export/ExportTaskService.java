package com.edumind.question.service.export;

import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.vo.export.ExportTaskVO;

import java.util.List;

/**
 * 试卷与文档导出业务服务接口
 */
public interface ExportTaskService {

    /**
     * 创建高保真试卷导出任务
     */
    ExportTaskVO createPaperExportTask(PaperExportRequestDTO dto);

    /**
     * 获取指定导出任务状态与下载地址
     */
    ExportTaskVO getTaskStatus(Long taskId);

    /**
     * 查询当前用户的近期导出任务列表
     */
    List<ExportTaskVO> listMyExportTasks();
}
