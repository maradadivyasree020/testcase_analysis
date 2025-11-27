package com.example.college;

import com.example.tools.spec.TestSpec;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spec-driven test: reads qa/ATT-123-tests.yaml from test classpath
 * and runs one parameterized test per test case.
 */
@SpringBootTest(classes = CollegeApplication.class) // <-- change if your main class is different
@AutoConfigureMockMvc
public class SpecDrivenAttendanceApiTest {

    @Autowired
    MockMvc mockMvc;

    // src/test/resources/qa/ATT-123-tests.yaml
    private static final String SPEC_PATH = "/qa/ATT-123-tests.yaml";

    static Stream<TestCaseWithMeta> testCases() throws Exception {
        try (InputStream is = SpecDrivenAttendanceApiTest.class.getResourceAsStream(SPEC_PATH)) {
            if (is == null) {
                throw new IllegalStateException("Spec file not found on classpath: " + SPEC_PATH);
            }
            Yaml yaml = new Yaml();
            TestSpec spec = yaml.loadAs(is, TestSpec.class);

            if (spec.getTests() == null || spec.getTests().isEmpty()) {
                throw new IllegalStateException("No tests found in spec: " + SPEC_PATH);
            }

            return spec.getTests().stream()
                    .map(tc -> new TestCaseWithMeta(spec.getApi().getMethod(), spec.getApi().getPath(), tc));
        }
    }

    @ParameterizedTest(name = "{0}") // <-- uses TestCaseWithMeta.toString()
    @MethodSource("testCases")
    @DisplayName("Spec-driven API tests for ATT-123")
    void runSpecDrivenTests(TestCaseWithMeta data) throws Exception {
        TestSpec.TestCase tc = data.testCase;

        var requestBuilder = post(data.path)
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
        final String method;
        final String path;
        final TestSpec.TestCase testCase;

        TestCaseWithMeta(String method, String path, TestSpec.TestCase testCase) {
            this.method = method;
            this.path = path;
            this.testCase = testCase;
        }

        @Override
        public String toString() {
            // This string is used in @ParameterizedTest(name = "{0}")
            return "TC " + testCase.getId() + ": " + testCase.getDescription();
        }
    }
}
