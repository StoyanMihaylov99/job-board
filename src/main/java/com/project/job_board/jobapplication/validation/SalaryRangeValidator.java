package com.project.job_board.jobapplication.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SalaryRangeValidator implements ConstraintValidator<ValidSalaryRange, SalaryRange> {

    @Override
    public boolean isValid(SalaryRange value, ConstraintValidatorContext context) {
        if (value == null || value.salaryMin() == null || value.salaryMax() == null) {
            return true;
        }
        if (value.salaryMin().compareTo(value.salaryMax()) <= 0) {
            return true;
        }
        // Report the error on the salaryMin field so clients can show it next to the input.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("salaryMin")
                .addConstraintViolation();
        return false;
    }
}
