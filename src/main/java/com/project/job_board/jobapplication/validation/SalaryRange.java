package com.project.job_board.jobapplication.validation;

import java.math.BigDecimal;

/** Implemented by payloads that carry an optional salary range. */
public interface SalaryRange {

    BigDecimal salaryMin();

    BigDecimal salaryMax();
}
