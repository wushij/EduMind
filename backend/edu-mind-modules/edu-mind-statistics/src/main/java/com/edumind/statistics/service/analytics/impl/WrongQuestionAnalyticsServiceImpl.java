package com.edumind.statistics.service.analytics.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.WrongQuestionAnalyticsService;
import com.edumind.statistics.vo.analytics.WrongQuestionAnalyticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongQuestionAnalyticsServiceImpl implements WrongQuestionAnalyticsService {

    private final WrongQuestionRecordDao wrongQuestionRecordDao;

    @Override
    public WrongQuestionAnalyticsVO listWrongQuestions(Long courseId, Long knowledgePointId, int page, int pageSize) {
        Page<WrongQuestionRecordEntity> result = wrongQuestionRecordDao.pageByCourse(
                new Page<>(page, pageSize), courseId, knowledgePointId);
        WrongQuestionAnalyticsVO vo = new WrongQuestionAnalyticsVO();
        vo.setTotal(result.getTotal());
        for (WrongQuestionRecordEntity entity : result.getRecords()) {
            WrongQuestionAnalyticsVO.WrongQuestionItemVO item = new WrongQuestionAnalyticsVO.WrongQuestionItemVO();
            item.setId(entity.getId());
            item.setQuestionId(entity.getQuestionId());
            item.setWrongCount(entity.getWrongCount());
            item.setErrorTypes(parseErrorTypes(entity.getErrorTypes()));
            item.setDiagnosis(entity.getDiagnosis());
            item.setVariantQuestionIds(parseVariantIds(entity.getVariantQuestionIds()));
            vo.getList().add(item);
        }
        return vo;
    }

    private List<String> parseErrorTypes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(StringUtils::hasText).collect(Collectors.toList());
    }

    private List<Long> parseVariantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }
}
