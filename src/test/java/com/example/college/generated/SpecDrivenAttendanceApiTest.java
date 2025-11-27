package com.example.college.generated;

import com.example.tools.spec.TestSpec;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Run these spec-driven integration tests only when explicitly enabled via
// the environment variable RUN_SPEC_TESTS=true. This avoids flaky CI failures
// when the spec or environment isn't available.
@EnabledIfEnvironmentVariable(named = "RUN_SPEC_TESTS", matches = "true")
@SpringBootTest(classes = com.example.college.CollegeApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SpecDrivenAttendanceApiTest {

    @Autowired
    MockMvc mockMvc;

    private static final String SPEC_PATH = "/qa/ATT-123-tests.yaml";

    static Stream<TestCaseWithMeta> testCases() throws Exception {
        try (InputStream is = SpecDrivenAttendanceApiTest.class.getResourceAsStream(SPEC_PATH)) {
            if (is == null) {
                // Spec file not found - skip this test by returning empty stream
                System.out.println("⚠️  Spec file not found on classpath: " + SPEC_PATH);
                System.out.println("    This is expected if spec-driven tests haven't been generated yet.");
                System.out.println("    Skipping spec-driven tests.");
                return Stream.empty();
            }
            Yaml yaml = new Yaml();
            TestSpec spec = yaml.loadAs(is, TestSpec.class);
            return spec.getTests().stream()
                    .map(tc -> new TestCaseWithMeta(spec.getApi().getPath(), tc));
        }
    }

    @ParameterizedTest(name = "TC {index}: {0}")
    @MethodSource("testCases")
    @DisplayName("Spec-driven API tests for ATT-123")
    void runSpecDrivenTests(TestCaseWithMeta data) throws Exception {
        TestSpec.TestCase tc = data.testCase;

        var requestBuilder = post(data.path)  // API is POST in this story
                .contentType(MediaType.APPLICATION_JSON)
                .content(tc.getRequestBody());

        var result = mockMvc.perform(requestBuilder)
                .andExpect(status().is(tc.getExpectedStatus()))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        if (tc.getExpectedJson() != null) {
            for (TestSpec.ExpectedJson ej : tc.getExpectedJson()) {
                Object actual = JsonPath.read(responseBody, ej.getPath());
                assertThat(actual)
                        .withFailMessage("JSON path %s expected <%s> but was <%s>",
                                ej.getPath(), ej.getValue(), actual)
                        .isEqualTo(ej.getValue());
            }
        }
    }

    // Helper wrapper for method source
    static class TestCaseWithMeta {
        final String path;
        final TestSpec.TestCase testCase;

        TestCaseWithMeta(String path, TestSpec.TestCase testCase) {
            this.path = path;
            this.testCase = testCase;
        }

        @Override
        public String toString() {
            return "TC " + testCase.getId() + ": " + testCase.getDescription();
        }
    }
}
