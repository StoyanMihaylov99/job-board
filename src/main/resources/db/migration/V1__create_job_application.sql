CREATE TABLE job_application
(
    id              UUID PRIMARY KEY,
    title           VARCHAR(200)             NOT NULL,
    company         VARCHAR(200)             NOT NULL,
    position        VARCHAR(100),
    status          VARCHAR(20)              NOT NULL DEFAULT 'APPLIED',
    applied_date    DATE,
    location        VARCHAR(200),
    work_mode       VARCHAR(10),
    salary_min      NUMERIC(12, 2),
    salary_max      NUMERIC(12, 2),
    salary_currency VARCHAR(3),
    job_url         VARCHAR(2048),
    notes           TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    version         BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT chk_job_application_status CHECK (status IN
        ('WISHLIST', 'APPLIED', 'INTERVIEWING', 'OFFER', 'ACCEPTED', 'REJECTED', 'WITHDRAWN', 'GHOSTED')),
    CONSTRAINT chk_job_application_work_mode CHECK (work_mode IN ('ONSITE', 'HYBRID', 'REMOTE')),
    CONSTRAINT chk_job_application_salary_non_negative CHECK (
        (salary_min IS NULL OR salary_min >= 0) AND (salary_max IS NULL OR salary_max >= 0)),
    CONSTRAINT chk_job_application_salary_range CHECK (
        salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max),
    CONSTRAINT chk_job_application_currency CHECK (salary_currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_job_application_status ON job_application (status);
CREATE INDEX idx_job_application_applied_date ON job_application (applied_date);
