package com.project.job_board.jobapplication.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ensures {@code salaryMin <= salaryMax} when both are present. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SalaryRangeValidator.class)
public @interface ValidSalaryRange {

    String message() default "must not be greater than salaryMax";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
