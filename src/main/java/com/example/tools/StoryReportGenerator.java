// package com.example.tools;

// import com.example.tools.llm.LlmClient;
// import com.example.tools.llm.dto.LlmRequest;
// import com.example.tools.llm.dto.LlmResponse;
// import com.example.tools.model.TestGenerationResult;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.yaml.snakeyaml.Yaml;

// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.util.List;
// import java.util.Map;

// public class StoryReportGenerator {

//     private static final String GENERATED_TEST_DIR =
//             "src/test/java/com/example/generated";

//     public static void main(String[] args) throws Exception {
//         String storyFile = null;
//         String outputFile = "reports/story-report.md";

//         for (String arg : args) {
//             if (arg.startsWith("--storyFile=")) {
//                 storyFile = arg.substring("--storyFile=".length());
//             } else if (arg.startsWith("--outputFile=")) {
//                 outputFile = arg.substring("--outputFile=".length());
//             }
//         }

//         if (storyFile == null) {
//             throw new IllegalArgumentException("Missing --storyFile=<path>");
//         }

//         // 1. Read YAML story
//         String storyYaml = Files.readString(Path.of(storyFile));

//         Yaml yaml = new Yaml();
//         Map<String, Object> storyMap = yaml.load(storyYaml);
//         String storyId = String.valueOf(storyMap.getOrDefault("id", "UNKNOWN"));

//         // 2. Build prompts
//         String systemPrompt = """
//             You are a senior QA engineer and test architect for Java Spring Boot applications.

//             You must generate UNLIMITED test cases.
//             There is NO upper limit. Produce as many tests as needed to cover:

//             - All positive scenarios
//             - All negative scenarios
//             - All edge cases
//             - All boundary cases
//             - All error cases
//             - All validation failures
//             - All security-related test cases
//             - All stress/performance test scenarios (mocked)
//             - Any logical scenario implied by the story or API
            
//             CRITICAL: Your ENTIRE response must be ONLY a JSON object. 
//             Do NOT include any explanatory text before or after the JSON.
//             Do NOT wrap the JSON in markdown code fences.
//             Do NOT include comments or preamble.
//             Output ONLY raw JSON.

//             The JSON object must contain:
//             - "reportMarkdown": a full QA report as a markdown string
//             - "tests": an array of objects { "className": string, "javaCode": string }

//             You may generate MULTIPLE test classes.
//             The Java code must be valid JUnit 5.
//         """;

//        String userPrompt = """
//     Here is the YAML describing the story and API:

//     ```yaml
//     %s
//     ```

//     Generate ALL possible test cases. 
//     There is NO LIMIT — produce as many test cases as necessary.

//     Requirements:
//     1. Generate test classes in logical groups (ValidTests, InvalidTests, EdgeTests, etc).
//     2. Include every meaningful scenario the story implies.
//     3. Include all combinations of inputs, boundary values, and errors.
//     4. Return ONLY a JSON object with this exact structure:

//     {
//       "reportMarkdown": "string",
//       "tests": [
//         {
//           "className": "AnyNameTests",
//           "javaCode": "public class AnyNameTests { ... }"
//         }
//       ]
//     }

//     Generate the maximum number of tests possible.
//     RESPOND WITH ONLY JSON. NO OTHER TEXT.
//     """.formatted(storyYaml);


//         // 3. Prepare LLM client (from env vars)
//         String baseUrl = getEnvOrThrow("LLM_BASE_URL");   // e.g. https://openrouter.ai/api
//         String apiKey = getEnvOrThrow("LLM_API_KEY");
//         String model = System.getenv().getOrDefault("LLM_MODEL", "gpt-4.1"); // adjust

//         // Validate and sanitize LLM_BASE_URL
//         baseUrl = baseUrl.trim();
//         if (baseUrl.contains("\n") || baseUrl.contains("\r")) {
//             throw new IllegalArgumentException(
//                 "LLM_BASE_URL contains newlines or carriage returns. " +
//                 "This usually means the secret is malformed in GitHub Actions. " +
//                 "Make sure the secret value is a single line with no trailing whitespace."
//             );
//         }
//         if (baseUrl.endsWith("/v1/chat/completions")) {
//             throw new IllegalArgumentException(
//                 "LLM_BASE_URL should be the BASE URL (e.g., https://api.openrouter.io), " +
//                 "NOT the full endpoint path. Remove /v1/chat/completions from the end."
//             );
//         }

//         LlmClient llmClient = new LlmClient(baseUrl, apiKey, model);

//         LlmRequest request = new LlmRequest(
//                 llmClient.getModel(),
//                 List.of(
//                         LlmClient.system(systemPrompt),
//                         LlmClient.user(userPrompt)
//                 )
//         );

//         // 4. Call LLM
//         LlmResponse response = llmClient.callLlm(request);
//         String jsonContent = response.getFirstContentOrThrow();

//         // 5. Clean JSON response (strip markdown code fences if present)
//         jsonContent = cleanJsonResponse(jsonContent);

//         // 6. Parse JSON into TestGenerationResult with fallback
//         ObjectMapper mapper = new ObjectMapper();
//         TestGenerationResult result;
//         try {
//             result = mapper.readValue(jsonContent, TestGenerationResult.class);
//         } catch (Exception e) {
//             System.err.println("⚠️  Warning: Failed to parse LLM response as JSON: " + e.getMessage());
//             System.err.println("Response started with: " + jsonContent.substring(0, Math.min(100, jsonContent.length())));
//             System.err.println("Treating entire response as markdown report...");
            
//             result = new TestGenerationResult();
//             result.setReportMarkdown(jsonContent);
//             result.setTests(null);
            
//             System.err.println("Falling back to plain markdown report (no generated tests).");
//         }

//         // 7. Write Markdown report
//         Path reportPath = Path.of(outputFile);
//         Files.createDirectories(reportPath.getParent());
//         Files.writeString(reportPath, result.getReportMarkdown());

//         // 8. Write generated test classes
//         if (result.getTests() != null) {
//             for (TestGenerationResult.GeneratedTest t : result.getTests()) {
//                 writeTestClass(t);
//             }
//         }

//         System.out.printf("Story %s processed. Report: %s%n", storyId, outputFile);
//     }

//     private static void writeTestClass(TestGenerationResult.GeneratedTest test) throws IOException {
//         String className = test.getClassName();
//         if (className == null || className.isBlank()) {
//             throw new IllegalArgumentException("Generated test has no className");
//         }

//         Path dir = Path.of(GENERATED_TEST_DIR);
//         Files.createDirectories(dir);

//         Path file = dir.resolve(className + ".java");
//         Files.writeString(file, test.getJavaCode());
//         System.out.println("Generated test: " + file);
//     }

//     /**
//      * Cleans JSON response by stripping markdown code fences.
//      * LLMs sometimes wrap JSON in ```json ... ``` fences even when asked for raw JSON.
//      * This method removes those fences and returns clean JSON.
//      */
//     private static String cleanJsonResponse(String response) {
//         if (response == null || response.isBlank()) {
//             return response;
//         }

//         String trimmed = response.trim();

//         // Check for markdown code fence pattern: ```json\n...\n```
//         if (trimmed.startsWith("```json")) {
//             // Remove opening ```json
//             trimmed = trimmed.substring(7).trim(); // Remove "```json"

//             // Remove closing ```
//             if (trimmed.endsWith("```")) {
//                 trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
//             }
//         } else if (trimmed.startsWith("```")) {
//             // Generic code fence without language specifier
//             trimmed = trimmed.substring(3).trim();
//             if (trimmed.endsWith("```")) {
//                 trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
//             }
//         }

//         return trimmed;
//     }
//     private static String getEnvOrThrow(String key) {
//         String value = System.getenv(key);
//         if (value == null || value.isBlank()) {
//             throw new IllegalStateException("Missing environment variable: " + key);
//         }
//         return value;
//     }
// }

package com.example.tools;

import com.example.tools.llm.LlmClient;
import com.example.tools.llm.dto.LlmRequest;
import com.example.tools.llm.dto.LlmResponse;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class StoryReportGenerator {

    // Safe default upper bound; can be overridden by env LLM_MAX_TOKENS
    private static final int DEFAULT_MAX_TOKENS = 2500;

    public static void main(String[] args) throws Exception {
        String storyFile = null;
        String outputFile = "reports/story-report.md";

        for (String arg : args) {
            if (arg.startsWith("--storyFile=")) {
                storyFile = arg.substring("--storyFile=".length());
            } else if (arg.startsWith("--outputFile=")) {
                outputFile = arg.substring("--outputFile=".length());
            }
        }

        if (storyFile == null) {
            throw new IllegalArgumentException("Missing --storyFile=<path>");
        }

        // 1. Read YAML story
        String storyYaml = Files.readString(Path.of(storyFile));

        Yaml yaml = new Yaml();
        Map<String, Object> storyMap = yaml.load(storyYaml);
        String storyId = String.valueOf(storyMap.getOrDefault("id", "UNKNOWN"));

        // 2. Build prompts (focus on markdown report, not code generation)
        String systemPrompt = """
                You are a senior QA engineer and test architect. Your task is to create a comprehensive
                QA analysis and test plan report in Markdown format.

                You must output ONLY a SINGLE, VALID MARKDOWN document. Do NOT output JSON, code blocks, or any non-Markdown.

                The report should include:
                - Executive summary of test scenarios
                - Positive test cases and expected outcomes
                - Negative test cases and error handling
                - Edge cases and boundary value analysis
                - Performance and stress scenarios
                - Security considerations

                Write clear, actionable test case descriptions that can be implemented by developers.
                Keep the markdown well-structured with headings, lists, and clear formatting.
                """;

        String userPrompt = """
                Here is the API specification you need to analyze:

                ```yaml
                %s
                ```

                Generate a COMPREHENSIVE QA TEST PLAN in Markdown format.

                Include:
                1. Overview of the API functionality
                2. Test Case Categories (Positive, Negative, Edge Cases, Security)
                3. For each category, list specific test scenarios with:
                   - Test case ID
                   - Description
                   - Input parameters
                   - Expected outcome
                   - Edge cases or special considerations

                DO NOT include JSON, code, or any non-Markdown content.
                Output ONLY a valid Markdown document.
                """.formatted(storyYaml);


        // 3. Prepare LLM client (from env vars)
        String baseUrl = getEnvOrThrow("LLM_BASE_URL");   // e.g. https://openrouter.ai/api
        String apiKey = getEnvOrThrow("LLM_API_KEY");
        String model = System.getenv().getOrDefault("LLM_MODEL", "openai/gpt-4.1");

        LlmClient llmClient = new LlmClient(baseUrl, apiKey, model);

        LlmRequest request = new LlmRequest(
                llmClient.getModel(),
                List.of(
                        LlmClient.system(systemPrompt),
                        LlmClient.user(userPrompt)
                )
        );

        // Force JSON-object response mode (OpenAI/OpenRouter style)
        // NOTE: Disabled since we're now requesting Markdown only
        // request.setResponse_format(Map.of("type", "json_object"));

        // Cap tokens so we don't blow the credit/size limit
        int maxTokens = getMaxTokensFromEnvOrDefault();
        request.setMax_tokens(maxTokens);

        // 4. Call LLM
        LlmResponse response = llmClient.callLlm(request);
        String reportContent = response.getFirstContentOrThrow();

        // 5. Write Markdown report directly (no JSON parsing needed)
        Path reportPath = Path.of(outputFile);
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, reportContent);

        System.out.printf("✅ Story %s processed. Report: %s (max_tokens=%d)%n",
                storyId, outputFile, maxTokens);
    }




    private static String getEnvOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }

    // Optionally override max tokens via env var LLM_MAX_TOKENS
    private static int getMaxTokensFromEnvOrDefault() {
        String fromEnv = System.getenv("LLM_MAX_TOKENS");
        if (fromEnv != null && !fromEnv.isBlank()) {
            try {
                return Integer.parseInt(fromEnv.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_MAX_TOKENS;
    }
}
