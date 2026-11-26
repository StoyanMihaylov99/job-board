package com.project.job_board.jobapplication;

import com.project.job_board.jobapplication.dto.JobApplicationFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class JobApplicationSpecifications {

    private JobApplicationSpecifications() {
    }

    static Specification<JobApplication> from(JobApplicationFilter filter) {
        List<Specification<JobApplication>> specs = new ArrayList<>();
        if (filter.status() != null && !filter.status().isEmpty()) {
            specs.add(hasStatusIn(filter.status()));
        }
        if (StringUtils.hasText(filter.company())) {
            specs.add(companyContains(filter.company()));
        }
        if (StringUtils.hasText(filter.q())) {
            specs.add(matchesSearchTerm(filter.q()));
        }
        if (filter.appliedFrom() != null) {
            specs.add(appliedOnOrAfter(filter.appliedFrom()));
        }
        if (filter.appliedTo() != null) {
            specs.add(appliedOnOrBefore(filter.appliedTo()));
        }
        return Specification.allOf(specs);
    }

    private static Specification<JobApplication> hasStatusIn(Collection<ApplicationStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    private static Specification<JobApplication> companyContains(String company) {
        String pattern = likePattern(company);
        return (root, query, cb) -> cb.like(cb.lower(root.get("company")), pattern, '\\');
    }

    private static Specification<JobApplication> matchesSearchTerm(String term) {
        String pattern = likePattern(term);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("company")), pattern, '\\'),
                cb.like(cb.lower(root.get("position")), pattern, '\\'));
    }

    private static Specification<JobApplication> appliedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("appliedDate"), from);
    }

    private static Specification<JobApplication> appliedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("appliedDate"), to);
    }

    /** Builds a case-insensitive "contains" LIKE pattern, escaping LIKE wildcards in user input. */
    private static String likePattern(String value) {
        String escaped = value.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
