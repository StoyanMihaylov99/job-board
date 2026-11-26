package com.project.job_board.jobapplication;

import com.jayway.jsonpath.JsonPath;
import com.project.job_board.TestUsers;
import com.project.job_board.TestUsers.TestUser;
import com.project.job_board.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class JobApplicationIntegrationTest {

    private static final String BASE = "/api/v1/applications";

    @Autowired
    private MockMvcTester mvc;

    private TestUser user;

    @BeforeEach
    void signIn() {
        user = TestUsers.register(mvc);
    }

    @Test
    void fullLifecycle() throws Exception {
        // create
        MvcTestResult created = mvc.post().uri(BASE).with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Backend Developer",
                          "company": "Acme",
                          "position": "Mid",
                          "workMode": "REMOTE",
                          "salaryMin": 3000,
                          "salaryMax": 4500,
                          "salaryCurrency": "EUR",
                          "jobUrl": "https://acme.example/jobs/1"
                        }
                        """)
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String body = created.getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("APPLIED");
        assertThat((String) JsonPath.read(body, "$.appliedDate")).isEqualTo(LocalDate.now().toString());

        // read back via Location
        String location = created.getResponse().getHeader("Location");
        assertThat(mvc.get().uri(location).with(user.auth())).hasStatusOk()
                .bodyJson().extractingPath("$.company").isEqualTo("Acme");

        // list with filters
        assertThat(mvc.get().uri(BASE + "?status=APPLIED&q=backend").with(user.auth()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);

        // change status
        assertThat(mvc.patch().uri(BASE + "/{id}/status", id).with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "INTERVIEWING"}
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("INTERVIEWING");

        // full update keeps status when omitted
        assertThat(mvc.put().uri(BASE + "/{id}", id).with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Senior Backend Developer", "company": "Acme", "appliedDate": "2026-11-01"}
                        """))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.title", v -> assertThat(v).isEqualTo("Senior Backend Developer"))
                .hasPathSatisfying("$.status", v -> assertThat(v).isEqualTo("INTERVIEWING"))
                .hasPathSatisfying("$.appliedDate", v -> assertThat(v).isEqualTo("2026-11-01"))
                .hasPathSatisfying("$.salaryMin", v -> assertThat(v).isNull());

        // stats
        assertThat(mvc.get().uri(BASE + "/stats").with(user.auth()))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.total", v -> assertThat(v).isEqualTo(1))
                .hasPathSatisfying("$.byStatus.INTERVIEWING", v -> assertThat(v).isEqualTo(1))
                .hasPathSatisfying("$.byStatus.OFFER", v -> assertThat(v).isEqualTo(0));

        // delete
        assertThat(mvc.delete().uri(BASE + "/{id}", id).with(user.auth())).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri(BASE + "/{id}", id).with(user.auth())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(BASE + "/{id}", id).with(user.auth())).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void wishlistItemsKeepEmptyAppliedDate() throws Exception {
        MvcTestResult created = mvc.post().uri(BASE).with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Java Engineer", "company": "Globex", "status": "WISHLIST"}
                        """)
                .exchange();

        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat((Object) JsonPath.read(created.getResponse().getContentAsString(), "$.appliedDate")).isNull();
    }

    @Test
    void usersCannotSeeOrChangeEachOthersApplications() throws Exception {
        MvcTestResult created = mvc.post().uri(BASE).with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title": "Private role", "company": "Secret Co"}
                        """)
                .exchange();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        TestUser intruder = TestUsers.register(mvc);
        assertThat(mvc.get().uri(BASE + "/{id}", id).with(intruder.auth())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.patch().uri(BASE + "/{id}/status", id).with(intruder.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status": "REJECTED"}
                        """))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri(BASE + "/{id}", id).with(intruder.auth())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri(BASE).with(intruder.auth()))
                .hasStatusOk().bodyJson().extractingPath("$.page.totalElements").isEqualTo(0);
        assertThat(mvc.get().uri(BASE + "/stats").with(intruder.auth()))
                .hasStatusOk().bodyJson().extractingPath("$.total").isEqualTo(0);

        // the owner still has it, unchanged
        assertThat(mvc.get().uri(BASE + "/{id}", id).with(user.auth()))
                .hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("APPLIED");
    }

    @Test
    void requiresAValidToken() {
        assertThat(mvc.get().uri(BASE)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri(BASE).header("Authorization", "Bearer not-a-jwt")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsUnknownSortProperty() {
        assertThat(mvc.get().uri(BASE + "?sort=doesNotExist").with(user.auth()))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.title").isEqualTo("Invalid sort parameter");
    }
}
