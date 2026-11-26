package com.project.job_board.jobapplication;

import com.project.job_board.TestcontainersConfiguration;
import com.project.job_board.jobapplication.dto.JobApplicationFilter;
import com.project.job_board.jobapplication.dto.StatusCount;
import com.project.job_board.user.AppUser;
import com.project.job_board.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class JobApplicationRepositoryTest {

    @Autowired
    private JobApplicationRepository repository;

    @Autowired
    private AppUserRepository users;

    private UUID owner;
    private UUID otherUser;

    @BeforeEach
    void seed() {
        owner = users.save(new AppUser("owner@example.com", "{noop}x", "Owner")).getId();
        otherUser = users.save(new AppUser("other@example.com", "{noop}x", "Other")).getId();
        JobApplication foreign = application("Backend Developer", "Acme Corp", "Senior", ApplicationStatus.OFFER, null);
        foreign.setOwnerId(otherUser);
        repository.save(foreign);

        repository.saveAll(List.of(
                application("Backend Developer", "Acme Corp", "Senior", ApplicationStatus.APPLIED, LocalDate.of(2026, 10, 1)),
                application("Frontend Developer", "Globex", "Mid", ApplicationStatus.INTERVIEWING, LocalDate.of(2026, 11, 1)),
                application("Java Engineer", "Initech", null, ApplicationStatus.APPLIED, LocalDate.of(2026, 11, 20)),
                application("Platform Engineer", "100% Remote_Co", null, ApplicationStatus.WISHLIST, null)));
        repository.flush();
    }

    @Test
    void filtersByStatus() {
        assertThat(find(new JobApplicationFilter(List.of(ApplicationStatus.APPLIED), null, null, null, null)))
                .extracting(JobApplication::getCompany)
                .containsExactlyInAnyOrder("Acme Corp", "Initech");
    }

    @Test
    void filtersByCompanyCaseInsensitively() {
        assertThat(find(new JobApplicationFilter(null, "acme", null, null, null)))
                .extracting(JobApplication::getCompany)
                .containsExactly("Acme Corp");
    }

    @Test
    void escapesLikeWildcardsInUserInput() {
        assertThat(find(new JobApplicationFilter(null, "%", null, null, null)))
                .extracting(JobApplication::getCompany)
                .containsExactly("100% Remote_Co");
    }

    @Test
    void freeTextSearchCoversTitleCompanyAndPosition() {
        assertThat(find(new JobApplicationFilter(null, null, "senior", null, null)))
                .extracting(JobApplication::getTitle).containsExactly("Backend Developer");
        assertThat(find(new JobApplicationFilter(null, null, "engineer", null, null)))
                .extracting(JobApplication::getTitle).containsExactlyInAnyOrder("Java Engineer", "Platform Engineer");
    }

    @Test
    void filtersByAppliedDateRange() {
        assertThat(find(new JobApplicationFilter(null, null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))))
                .extracting(JobApplication::getCompany)
                .containsExactlyInAnyOrder("Globex", "Initech");
    }

    @Test
    void emptyFilterReturnsEverything() {
        assertThat(find(new JobApplicationFilter(null, null, null, null, null))).hasSize(4);
    }

    @Test
    void neverReturnsOtherUsersApplications() {
        assertThat(repository.findAll(JobApplicationSpecifications.from(otherUser,
                new JobApplicationFilter(null, null, null, null, null))))
                .singleElement().extracting(JobApplication::getStatus).isEqualTo(ApplicationStatus.OFFER);
        assertThat(find(new JobApplicationFilter(List.of(ApplicationStatus.OFFER), null, null, null, null))).isEmpty();
    }

    @Test
    void findsByIdOnlyForTheOwner() {
        JobApplication mine = find(new JobApplicationFilter(null, "initech", null, null, null)).getFirst();
        assertThat(repository.findByIdAndOwnerId(mine.getId(), owner)).isPresent();
        assertThat(repository.findByIdAndOwnerId(mine.getId(), otherUser)).isEmpty();
    }

    @Test
    void countsByStatus() {
        assertThat(repository.countByStatus(owner)).containsExactlyInAnyOrder(
                new StatusCount(ApplicationStatus.APPLIED, 2),
                new StatusCount(ApplicationStatus.INTERVIEWING, 1),
                new StatusCount(ApplicationStatus.WISHLIST, 1));
    }

    @Test
    void databaseRejectsInvertedSalaryRange() {
        JobApplication invalid = application("Dev", "Acme", null, ApplicationStatus.APPLIED, null);
        invalid.setSalaryMin(new BigDecimal("5000"));
        invalid.setSalaryMax(new BigDecimal("1000"));

        assertThatThrownBy(() -> repository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void populatesAuditFields() {
        JobApplication saved = repository.saveAndFlush(
                application("Dev", "Acme", null, ApplicationStatus.APPLIED, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    private List<JobApplication> find(JobApplicationFilter filter) {
        return repository.findAll(JobApplicationSpecifications.from(owner, filter));
    }

    private JobApplication application(String title, String company, String position,
                                       ApplicationStatus status, LocalDate appliedDate) {
        JobApplication entity = new JobApplication();
        entity.setOwnerId(owner);
        entity.setTitle(title);
        entity.setCompany(company);
        entity.setPosition(position);
        entity.setStatus(status);
        entity.setAppliedDate(appliedDate);
        return entity;
    }
}
