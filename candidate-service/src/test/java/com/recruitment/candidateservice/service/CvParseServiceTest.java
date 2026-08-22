package com.recruitment.candidateservice.service;

import com.recruitment.candidateservice.dto.ParsedCvData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CvParseServiceTest {

    private final CvParseService cvParseService = new CvParseService();

    @Test
    void parseTxtExtractsEmailPhoneNameAndSkills() {
        String cv = """
                Alice Smith
                alice.smith@example.com
                +1 555-123-4567

                Skills: Java, Spring, Docker, MySQL
                Experience building REST APIs.
                """;

        ParsedCvData parsed = cvParseService.parseBytes(cv.getBytes(StandardCharsets.UTF_8), "txt");

        assertThat(parsed.firstName()).isEqualTo("Alice");
        assertThat(parsed.lastName()).isEqualTo("Smith");
        assertThat(parsed.email()).isEqualTo("alice.smith@example.com");
        assertThat(parsed.phone()).contains("15551234567");
        assertThat(parsed.tags()).contains("java", "spring", "docker", "mysql");
        assertThat(parsed.rawTextPreview()).contains("Alice Smith");
    }

    @Test
    void parseTxtWithoutEmailLeavesEmailNull() {
        String cv = """
                Bob Jones
                Skills: Python
                """;

        ParsedCvData parsed = cvParseService.parseBytes(cv.getBytes(StandardCharsets.UTF_8), "txt");

        assertThat(parsed.email()).isNull();
        assertThat(parsed.firstName()).isEqualTo("Bob");
        assertThat(parsed.tags()).contains("python");
    }
}
