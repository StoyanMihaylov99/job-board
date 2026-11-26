package com.project.job_board;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Registers real users through the API for integration tests. */
public final class TestUsers {

    public static final String PASSWORD = "correct horse battery";

    private TestUsers() {
    }

    public record TestUser(String email, String accessToken, Cookie refreshCookie) {

        public RequestPostProcessor auth() {
            return request -> {
                request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
                return request;
            };
        }
    }

    public static TestUser register(MockMvcTester mvc) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        MvcTestResult result = mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "displayName": "Test User"}
                        """.formatted(email, PASSWORD))
                .exchange();
        assertThat(result).hasStatus2xxSuccessful();
        return fromAuthResponse(email, result);
    }

    public static TestUser fromAuthResponse(String email, MvcTestResult result) {
        try {
            String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
            return new TestUser(email, token, result.getResponse().getCookie("refresh_token"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
