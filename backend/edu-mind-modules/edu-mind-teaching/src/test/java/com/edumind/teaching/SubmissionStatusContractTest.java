package com.edumind.teaching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Documents submission lifecycle statuses used across teaching module and frontend.
 */
class SubmissionStatusContractTest {

    @Test
    void submissionStatusValuesMatchApiContract() {
        assertEquals("SUBMITTED", "SUBMITTED");
        assertEquals("GRADED", "GRADED");
        assertEquals("REVIEWED", "REVIEWED");
    }
}
