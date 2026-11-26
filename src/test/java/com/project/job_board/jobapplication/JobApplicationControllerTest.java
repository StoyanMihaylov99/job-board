package com.project.job_board.jobapplication;

import com.project.job_board.auth.SecurityConfig;
import com.project.job_board.jobapplication.dto.JobApplicationFilter;
import com.project.job_board.jobapplication.dto.JobApplicationRequest;
import com.project.job_board.jobapplication.dto.JobApplicationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@WebMvcTest(JobApplicationController.class)
@Import(SecurityConfig.class)
class JobApplicationControllerTest {

    private static final String BASE = "/api/v1/applications";
    private static final UUID ID = UUID.fromString("01a217b2-f2e9-7228-99c5-8e4b00d1a4dd");
    private static final UUID USER = UUID.fromString("01a217b2-0000-7000-8000-000000000001");
    private static final RequestPostProcessor AUTH = jwt().jwt(token -> token.subject(USER.toString()));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private JobApplicationService service;

    @Test
    void requestsWithoutTokenAreRejected() {
        assertThat(mvc.get().uri(BASE)).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(service);
    }

    @Test
    void createReturns201WithLocationHeader() {
        given(service.create(eq(USER), any(JobApplicationRequest.class))).willReturn(sampleResponse(ApplicationStatus.APPLIED));

        MvcTestResult result = mvc.post().uri(BASE).with(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Backend Developer", "company": "Acme"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result.getResponse().getHeader("Location")).endsWith(BASE + "/" + ID);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.toString());
    }

    @Test
    void createWithInvalidPayloadReturns400WithFieldErrors() {
        MvcTestResult result = mvc.post().uri(BASE).with(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": " ",
                          "salaryMin": 5000,
                          "salaryMax": 1000,
                          "salaryCurrency": "eur",
                          "jobUrl": "ftp://example.com",
                          "appliedDate": "2999-01-01"
                        }
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors").asMap()
                .containsOnlyKeys("title", "company", "salaryMin", "salaryCurrency", "jobUrl", "appliedDate");
        verifyNoInteractions(service);
    }

    @Test
    void createWithUnknownStatusReturns400() {
        assertThat(mvc.post().uri(BASE).with(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Dev", "company": "Acme", "status": "NOPE"}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(service);
    }

    @Test
    void getUnknownIdReturns404ProblemDetail() {
        given(service.findById(USER, ID)).willThrow(new JobApplicationNotFoundException(ID));

        assertThat(mvc.get().uri(BASE + "/{id}", ID).with(AUTH))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.title").isEqualTo("Job application not found");
    }

    @Test
    void getWithMalformedIdReturns400() {
        assertThat(mvc.get().uri(BASE + "/not-a-uuid").with(AUTH)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listBindsFiltersAndPagination() {
        given(service.findAll(eq(USER), any(), any())).willReturn(new PageImpl<>(
                List.of(sampleResponse(ApplicationStatus.APPLIED)), PageRequest.of(0, 5), 1));

        assertThat(mvc.get().uri(BASE + "?status=APPLIED&status=OFFER&company=acme&appliedFrom=2026-01-01&size=5").with(AUTH))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[0].id", id -> assertThat(id).isEqualTo(ID.toString()))
                .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).isEqualTo(1));

        then(service).should().findAll(
                eq(USER),
                eq(new JobApplicationFilter(
                        List.of(ApplicationStatus.APPLIED, ApplicationStatus.OFFER), "acme", null,
                        LocalDate.of(2026, 1, 1), null)),
                any(Pageable.class));
    }

    @Test
    void listWithInvalidStatusReturns400WithoutLeakingTypes() {
        assertThat(mvc.get().uri(BASE + "?status=FOO").with(AUTH))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors.status").isEqualTo("invalid value 'FOO'");
    }

    @Test
    void patchStatusRequiresStatus() {
        assertThat(mvc.patch().uri(BASE + "/{id}/status", ID).with(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(service);
    }

    @Test
    void patchStatusDelegatesToService() {
        given(service.updateStatus(USER, ID, ApplicationStatus.OFFER)).willReturn(sampleResponse(ApplicationStatus.OFFER));

        assertThat(mvc.patch().uri(BASE + "/{id}/status", ID).with(AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "OFFER"}
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("OFFER");
    }

    @Test
    void deleteReturns204() {
        assertThat(mvc.delete().uri(BASE + "/{id}", ID).with(AUTH)).hasStatus(HttpStatus.NO_CONTENT);
        then(service).should().delete(USER, ID);
    }

    private static JobApplicationResponse sampleResponse(ApplicationStatus status) {
        Instant now = Instant.parse("2026-11-26T10:00:00Z");
        return new JobApplicationResponse(ID, "Backend Developer", "Acme", "Mid", status,
                LocalDate.of(2026, 11, 26), "Sofia", WorkMode.HYBRID, null, null, null, null, null, now, now);
    }
}
