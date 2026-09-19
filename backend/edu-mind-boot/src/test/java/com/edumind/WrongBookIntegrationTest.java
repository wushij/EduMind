package com.edumind;

import com.edumind.statistics.service.learning.WrongBookService;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@ActiveProfiles("test")
class WrongBookIntegrationTest {

    @Autowired
    private WrongBookService wrongBookService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void wrongBookListShouldEnrichQuestionStem() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wrong_question_record WHERE student_id = 3 AND status = 0", Long.class);
        assumeTrue(count != null && count > 0, "需 seed 错题记录");

        WrongBookListVO list = wrongBookService.list(3L, 102L, 1, 10, null, null, 0);
        assertNotNull(list.getList());
        assertTrue(list.getTotal() > 0);
        assertNotNull(list.getList().get(0).getStem());
        assertNotNull(list.getList().get(0).getStudentAnswer());
    }

    @Test
    void markMasteredShouldExcludeFromPendingList() {
        Long recordId = jdbcTemplate.query(
                "SELECT id FROM wrong_question_record WHERE student_id = 3 AND course_id = 102 AND status = 0 LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        assumeTrue(recordId != null, "需待攻坚错题");

        wrongBookService.markMastered(3L, recordId);
        WrongBookListVO after = wrongBookService.list(3L, 102L, 1, 50, null, null, 0);
        assertTrue(after.getList().stream().noneMatch(i -> recordId.equals(i.getId())));

        WrongBookOverviewVO overview = wrongBookService.overview(3L, 102L);
        assertTrue(overview.getMasteredCount() >= 1);

        jdbcTemplate.update("UPDATE wrong_question_record SET status = 0, mastered_time = NULL WHERE id = ?", recordId);
    }
}
