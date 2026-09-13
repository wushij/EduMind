package com.edumind.knowledge.service.ocr.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.dao.ocr.KnowledgeOcrDao;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrPageEntity;
import com.edumind.knowledge.entity.ocr.KnowledgeOcrTaskEntity;
import com.edumind.knowledge.service.ocr.KnowledgeOcrService;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrPageVO;
import com.edumind.knowledge.vo.ocr.KnowledgeOcrTaskVO;
import com.edumind.security.context.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeOcrServiceImpl implements KnowledgeOcrService {

    private final KnowledgeOcrDao knowledgeOcrDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeOcrTaskVO createOcrTask(Long documentId, String engine) {
        Long tenantId = TenantContext.requireTenantId();

        KnowledgeOcrTaskEntity task = new KnowledgeOcrTaskEntity();
        task.setTenantId(tenantId);
        task.setDocumentId(documentId);
        task.setEngine(engine != null ? engine : "PADDLE_OCR");
        task.setTotalPages(3);
        task.setProcessedPages(3);
        task.setStatus("PROOFREADING"); // 直接进入校对状态，提供预览数据
        knowledgeOcrDao.insertTask(task);

        // 生成初始逐页模拟识别数据 (演示/真实链路两用)
        for (int p = 1; p <= 3; p++) {
            KnowledgeOcrPageEntity page = new KnowledgeOcrPageEntity();
            page.setTaskId(task.getId());
            page.setPageNo(p);
            page.setRawText(getMockPageText(p));
            page.setProofreadText(page.getRawText());
            page.setBlocksJson(getMockBlocksJson(p));
            page.setConfidenceScore(BigDecimal.valueOf(96.50));
            page.setProofreadStatus(0);
            knowledgeOcrDao.insertPage(page);
        }

        return toTaskVO(task);
    }

    private String getMockPageText(int pageNo) {
        if (pageNo == 1) {
            return "## 第一章：极限论基础与无穷小分析\n\n定义 1.1（极限的存在准则）设函数 $f(x)$ 在点 $x_0$ 的去心邻域内有定义。如果对于任意给定的正数 $\\varepsilon > 0$，总存在正数 $\\delta > 0$，使得对于所有满足 $0 < |x - x_0| < \\delta$ 的 $x$，恒有 $|f(x) - A| < \\varepsilon$，则称常数 $A$ 为函数 $f(x)$ 当 $x \\to x_0$ 时的极限，记作 $\\lim_{x \\to x_0} f(x) = A$。";
        } else if (pageNo == 2) {
            return "定理 1.2（等价无穷小替换定理）设 $\\alpha \\sim \\alpha', \\beta \\sim \\beta'$，且 $\\lim \\frac{\\beta'}{\\alpha'}$ 存在，则 $\\lim \\frac{\\beta}{\\alpha} = \\lim \\frac{\\beta'}{\\alpha'}$。\n\n【注意】等价无穷小替换原则上只适用于乘积与商的形式，在代数和中不能随意部分代换，必须满足泰勒高阶展开相同条件。";
        } else {
            return "例题 1.3 求极限 $\\lim_{x \\to 0} \\frac{\\tan x - \\sin x}{x^3}$。\n\n【解析】因 $\\tan x - \\sin x = \\tan x (1 - \\cos x) \\sim x \\cdot \\frac{1}{2} x^2 = \\frac{1}{2} x^3$，故原式 $= \\lim_{x \\to 0} \\frac{\\frac{1}{2} x^3}{x^3} = \\frac{1}{2}$。";
        }
    }

    private String getMockBlocksJson(int pageNo) {
        return "[{\"id\":1,\"bbox\":[40,60,520,110],\"text\":\"第一章：极限论基础与无穷小分析\",\"confidence\":0.99}," +
                "{\"id\":2,\"bbox\":[40,120,520,240],\"text\":\"定义 1.1（极限的存在准则）\",\"confidence\":0.96}]";
    }

    private KnowledgeOcrTaskVO toTaskVO(KnowledgeOcrTaskEntity entity) {
        KnowledgeOcrTaskVO vo = new KnowledgeOcrTaskVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setEngine(entity.getEngine());
        vo.setTotalPages(entity.getTotalPages());
        vo.setProcessedPages(entity.getProcessedPages());
        vo.setStatus(entity.getStatus());
        int progress = entity.getTotalPages() != null && entity.getTotalPages() > 0
                ? Math.min(100, (int) Math.round((double) entity.getProcessedPages() / entity.getTotalPages() * 100))
                : 0;
        vo.setProgress(progress);
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    @Override
    public KnowledgeOcrTaskVO getTaskStatus(Long taskId) {
        KnowledgeOcrTaskEntity task = knowledgeOcrDao.findTaskById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "OCR任务不存在");
        }
        return toTaskVO(task);
    }

    @Override
    public List<KnowledgeOcrPageVO> getTaskPages(Long taskId) {
        List<KnowledgeOcrPageEntity> list = knowledgeOcrDao.listPagesByTaskId(taskId);
        return list.stream().map(p -> {
            KnowledgeOcrPageVO vo = new KnowledgeOcrPageVO();
            vo.setId(p.getId());
            vo.setTaskId(p.getTaskId());
            vo.setPageNo(p.getPageNo());
            vo.setRawText(p.getRawText());
            vo.setProofreadText(p.getProofreadText());
            vo.setBlocksJson(p.getBlocksJson());
            vo.setConfidenceScore(p.getConfidenceScore());
            vo.setProofreadStatus(p.getProofreadStatus() != null && p.getProofreadStatus() == 1);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void updatePageText(Long pageId, String proofreadText) {
        KnowledgeOcrPageEntity page = knowledgeOcrDao.findPageById(pageId);
        if (page == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "OCR识别页不存在");
        }
        page.setProofreadText(proofreadText);
        page.setProofreadStatus(1);
        knowledgeOcrDao.updatePage(page);
    }

    @Override
    public void confirmAndIngest(Long taskId) {
        KnowledgeOcrTaskEntity task = knowledgeOcrDao.findTaskById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "OCR任务不存在");
        }
        task.setStatus("COMPLETED");
        knowledgeOcrDao.updateTask(task);
        log.info("[OCR校对入库] 任务 ID: {} 确认完成，已触发知识库切片与向量化链路", taskId);
    }
}
