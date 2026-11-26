package com.project.job_board.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * At least 8 characters and at most 72 bytes in UTF-8: BCrypt only uses the first 72 bytes,
 * so longer passwords would silently be truncated (and Spring Security rejects them).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ValidPassword {

    String message() default "must be at least 8 characters and at most 72 bytes long";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
