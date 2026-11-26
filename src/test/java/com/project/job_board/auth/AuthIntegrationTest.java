package com.project.job_board.auth;

import com.project.job_board.TestUsers;
import com.project.job_board.TestUsers.TestUser;
import com.project.job_board.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

    private static final String AUTH = "/api/v1/auth";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void registerReturnsTokenAndSetsHardenedRefreshCookie() throws Exception {
        MvcTestResult result = mvc.post().uri(AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "  New.User@Example.com ", "password": "%s", "displayName": " Ana "}
                        """.formatted(TestUsers.PASSWORD))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .hasPathSatisfying("$.tokenType", v -> assertThat(v).isEqualTo("Bearer"))
                .hasPathSatisfying("$.expiresIn", v -> assertThat(v).isEqualTo(900))
                .hasPathSatisfying("$.user.email", v -> assertThat(v).isEqualTo("new.user@example.com"))
                .hasPathSatisfying("$.user.displayName", v -> assertThat(v).isEqualTo("Ana"));
        assertThat(result.getResponse().getContentAsString()).doesNotContain("refresh");

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("refresh_token=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/v1/auth", "Max-Age=1209600");
    }

    @Test
    void registerRejectsDuplicateEmailCaseInsensitively() {
        TestUser existing = TestUsers.register(mvc);
        assertThat(mvc.post().uri(AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "Copy"}
                        """.formatted(existing.email().toUpperCase(), TestUsers.PASSWORD)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.errors.email").isEqualTo("An account with this email already exists.");
    }

    @Test
    void registerValidatesInput() {
        assertThat(mvc.post().uri(AUTH + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "not-an-email", "password": "short", "displayName": ""}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").asMap()
                .containsOnlyKeys("email", "password", "displayName");
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailGivesTheSameError() throws Exception {
        TestUser user = TestUsers.register(mvc);
        String wrongPassword = login(user.email(), "wrong password!").getResponse().getContentAsString();
        String unknownEmail = login("nobody@example.com", TestUsers.PASSWORD).getResponse().getContentAsString();

        assertThat(login(user.email(), "wrong password!")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword).isEqualTo(unknownEmail).contains("Email or password is incorrect.");
    }

    @Test
    void loginSucceedsWithAnyEmailCasing() {
        TestUser user = TestUsers.register(mvc);
        MvcTestResult result = login(user.email().toUpperCase(), TestUsers.PASSWORD);
        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() {
        TestUser user = TestUsers.register(mvc);
        Cookie original = user.refreshCookie();

        MvcTestResult refreshed = refresh(original);
        assertThat(refreshed).hasStatusOk().bodyJson().extractingPath("$.accessToken").isNotNull();
        Cookie rotated = refreshed.getResponse().getCookie("refresh_token");
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());

        // The new access token works
        TestUser afterRefresh = TestUsers.fromAuthResponse(user.email(), refreshed);
        assertThat(mvc.get().uri("/api/v1/me").with(afterRefresh.auth())).hasStatusOk();

        // Replaying the old token right away (two tabs racing) is tolerated...
        assertThat(refresh(original)).hasStatusOk();

        // ...but replaying it later means it was stolen: every session of the user is revoked.
        jdbc.update("update refresh_token set revoked_at = revoked_at - interval '1 minute' where revoked_at is not null");
        MvcTestResult reuse = refresh(original);
        assertThat(reuse).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(reuse.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        assertThat(refresh(rotated)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refreshWithoutOrWithUnknownCookieIsRejected() {
        assertThat(mvc.post().uri(AUTH + "/refresh")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(new Cookie("refresh_token", "made-up"))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheRefreshToken() {
        TestUser user = TestUsers.register(mvc);
        MvcTestResult logout = mvc.post().uri(AUTH + "/logout").cookie(user.refreshCookie()).exchange();
        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(logout.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
        assertThat(refresh(user.refreshCookie())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void profileCanBeReadAndRenamed() {
        TestUser user = TestUsers.register(mvc);
        assertThat(mvc.get().uri("/api/v1/me").with(user.auth()))
                .hasStatusOk().bodyJson().extractingPath("$.email").isEqualTo(user.email());

        assertThat(mvc.patch().uri("/api/v1/me").with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName": "Renamed"}
                        """))
                .hasStatusOk().bodyJson().extractingPath("$.displayName").isEqualTo("Renamed");
        assertThat(mvc.get().uri("/api/v1/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changingPasswordEndsOtherSessions() {
        TestUser user = TestUsers.register(mvc);
        Cookie otherDevice = login(user.email(), TestUsers.PASSWORD).getResponse().getCookie("refresh_token");

        assertThat(changePassword(user, "wrong current", "a brand new password"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors.currentPassword").isEqualTo("Current password is incorrect.");

        MvcTestResult changed = changePassword(user, TestUsers.PASSWORD, "a brand new password");
        assertThat(changed).hasStatusOk();
        Cookie freshCookie = changed.getResponse().getCookie("refresh_token");

        assertThat(refresh(otherDevice)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(freshCookie)).hasStatusOk();
        assertThat(login(user.email(), TestUsers.PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login(user.email(), "a brand new password")).hasStatusOk();
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post().uri(AUTH + "/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .exchange();
    }

    private MvcTestResult refresh(Cookie cookie) {
        return mvc.post().uri(AUTH + "/refresh").cookie(cookie).exchange();
    }

    private MvcTestResult changePassword(TestUser user, String current, String next) {
        return mvc.put().uri("/api/v1/me/password").with(user.auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"currentPassword": "%s", "newPassword": "%s"}
                        """.formatted(current, next))
                .exchange();
    }
}
