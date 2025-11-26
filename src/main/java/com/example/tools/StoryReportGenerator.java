package com.example.tools;

import com.example.tools.llm.LlmClient;
import com.example.tools.llm.dto.LlmRequest;
import com.example.tools.llm.dto.LlmResponse;
import com.example.tools.model.TestGenerationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class StoryReportGenerator {

    private static final String GENERATED_TEST_DIR =
            "src/test/java/com/example/generated";

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

        // 2. Build prompts
        String systemPrompt = """
            You are a senior QA engineer and test architect for Java Spring Boot applications.

            You must generate UNLIMITED test cases.
            There is NO upper limit. Produce as many tests as needed to cover:

            - All positive scenarios
            - All negative scenarios
            - All edge cases
            - All boundary cases
            - All error cases
            - All validation failures
            - All security-related test cases
            - All stress/performance test scenarios (mocked)
            - Any logical scenario implied by the story or API
            
            Your output must be a JSON object containing:
            - "reportMarkdown": a full QA report
            - "tests": an array of objects { "className": string, "javaCode": string }

            You may generate MULTIPLE test classes.
            The Java code must be valid JUnit 5.
        """;

       String userPrompt = """
    Here is the YAML describing the story and API:

    ```yaml
    %s
    ```

    Generate ALL possible test cases. 
    There is NO LIMIT — produce as many test cases as necessary.

    Requirements:
    1. Generate test classes in logical groups (ValidTests, InvalidTests, EdgeTests, etc).
    2. Include every meaningful scenario the story implies.
    3. Include all combinations of inputs, boundary values, and errors.
    4. Return JSON exactly in this structure:

    {
      "reportMarkdown": "string",
      "tests": [
        {
          "className": "AnyNameTests",
          "javaCode": "public class AnyNameTests { ... }"
        }
      ]
    }

    Generate the maximum number of tests possible.
    """.formatted(storyYaml);


        // 3. Prepare LLM client (from env vars)
        String baseUrl = getEnvOrThrow("LLM_BASE_URL");   // e.g. https://openrouter.ai/api
        String apiKey = getEnvOrThrow("LLM_API_KEY");
        String model = System.getenv().getOrDefault("LLM_MODEL", "gpt-4.1"); // adjust

        // Validate and sanitize LLM_BASE_URL
        baseUrl = baseUrl.trim();
        if (baseUrl.contains("\n") || baseUrl.contains("\r")) {
            throw new IllegalArgumentException(
                "LLM_BASE_URL contains newlines or carriage returns. " +
                "This usually means the secret is malformed in GitHub Actions. " +
                "Make sure the secret value is a single line with no trailing whitespace."
            );
        }
        if (baseUrl.endsWith("/v1/chat/completions")) {
            throw new IllegalArgumentException(
                "LLM_BASE_URL should be the BASE URL (e.g., https://api.openrouter.io), " +
                "NOT the full endpoint path. Remove /v1/chat/completions from the end."
            );
        }

        LlmClient llmClient = new LlmClient(baseUrl, apiKey, model);

        LlmRequest request = new LlmRequest(
                llmClient.getModel(),
                List.of(
                        LlmClient.system(systemPrompt),
                        LlmClient.user(userPrompt)
                )
        );

        // 4. Call LLM
        LlmResponse response = llmClient.callLlm(request);
        String jsonContent = response.getFirstContentOrThrow();

        // 5. Parse JSON into TestGenerationResult
        ObjectMapper mapper = new ObjectMapper();
        TestGenerationResult result =
                mapper.readValue(jsonContent, TestGenerationResult.class);

        // 6. Write Markdown report
        Path reportPath = Path.of(outputFile);
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, result.getReportMarkdown());

        // 7. Write generated test classes
        if (result.getTests() != null) {
            for (TestGenerationResult.GeneratedTest t : result.getTests()) {
                writeTestClass(t);
            }
        }

        System.out.printf("Story %s processed. Report: %s%n", storyId, outputFile);
    }

    private static void writeTestClass(TestGenerationResult.GeneratedTest test) throws IOException {
        String className = test.getClassName();
        if (className == null || className.isBlank()) {
            throw new IllegalArgumentException("Generated test has no className");
        }

        Path dir = Path.of(GENERATED_TEST_DIR);
        Files.createDirectories(dir);

        Path file = dir.resolve(className + ".java");
        Files.writeString(file, test.getJavaCode());
        System.out.println("Generated test: " + file);
    }

    private static String getEnvOrThrow(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return value;
    }
}

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

//     // 🔴 Safe upper bound for free-tier (can change or make env-driven)
//     private static final int DEFAULT_MAX_TOKENS = 3000;

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

//         String systemPrompt = """
//     You are a senior QA engineer and test architect for Java Spring Boot applications.

//     You will receive a YAML describing a user story, business rules, API specs, and acceptance criteria.

//     Generate a COMPACT but THOROUGH test suite:
//     - HARD LIMIT: at most 8–10 test methods total
//     - HARD LIMIT: at most 2 test classes
//     - Keep the Markdown report short (under ~300 lines)

//     Cover:
//     - Positive scenarios
//     - Negative scenarios
//     - Edge & boundary cases
//     - Validation and error scenarios

//     Return ONLY a JSON object with:
//     - "reportMarkdown": detailed QA report in Markdown
//     - "tests": array of { "className": string, "javaCode": string }

//     The Java code must be valid JUnit 5.
//     Do NOT output anything before or after the JSON.
//     Do NOT wrap JSON in ``` fences.
//     """;

//     String userPrompt = """
//     Here is the YAML describing the story and API:

//     ```yaml
//     %s
//     ```

//     Generate a compact test suite:

//     - Total test methods across all classes: MAX 10
//     - Total test classes: MAX 2
//     - Choose the most important scenarios only.

//     Output ONLY a SINGLE JSON object in this structure:

//     {
//       "reportMarkdown": "string",
//       "tests": [
//         {
//           "className": "AnyNameTests",
//           "javaCode": "public class AnyNameTests { ... }"
//         }
//       ]
//     }
//     """.formatted(storyYaml);

    

//         // 3. Prepare LLM client (from env vars)
//         String baseUrl = getEnvOrThrow("LLM_BASE_URL");   // e.g. https://api.openrouter.io or https://api.openai.com
//         String apiKey = getEnvOrThrow("LLM_API_KEY");
//         // Default to gpt-4-turbo (works for OpenAI and most OpenRouter setups)
//         String model = System.getenv().getOrDefault("LLM_MODEL", "gpt-4-turbo");

//         System.out.println("Using model: " + model);

//         LlmClient llmClient = new LlmClient(baseUrl, apiKey, model);

//         LlmRequest request = new LlmRequest(
//                 llmClient.getModel(),
//                 List.of(
//                         LlmClient.system(systemPrompt),
//                         LlmClient.user(userPrompt)
//                 )
//         );

//         request.setResponse_format(Map.of("type", "json_object"));
//         // 🔴 NEW: cap the response tokens so OpenRouter doesn’t throw 402
//         int maxTokens = getMaxTokensFromEnvOrDefault();
//         request.setMax_tokens(maxTokens);

//         // 4. Call LLM
//         LlmResponse response = llmClient.callLlm(request);
//         String jsonContent = response.getFirstContentOrThrow();

//         // 5. Parse JSON into TestGenerationResult with fallback
//         ObjectMapper mapper = new ObjectMapper();
//         TestGenerationResult result;
//         try {
//             result = mapper.readValue(jsonContent, TestGenerationResult.class);
//         } catch (Exception e) {
//             System.err.println("⚠️  Warning: Failed to parse LLM response as JSON: " + e.getMessage());
//             System.err.println("Attempting to extract Markdown report from raw response...");
            
//             // Fallback: try to extract reportMarkdown manually
//             String fallbackMarkdown = "# QA Report - Partial\n\nThe LLM response was invalid JSON (likely unescaped quotes in generated code).\n\n" +
//                     "Raw LLM response:\n\n```\n" + jsonContent + "\n```";
            
//             result = new TestGenerationResult();
//             result.setReportMarkdown(fallbackMarkdown);
//             result.setTests(null);
            
//             System.err.println("Falling back to plain report (no generated tests).");
//         }

//         // 6. Write Markdown report
//         Path reportPath = Path.of(outputFile);
//         Files.createDirectories(reportPath.getParent());
//         Files.writeString(reportPath, result.getReportMarkdown());

//         // 7. Write generated test classes (if any)
//         if (result.getTests() != null) {
//             for (TestGenerationResult.GeneratedTest t : result.getTests()) {
//                 writeTestClass(t);
//             }
//         }

//         System.out.printf("Story %s processed. Report: %s (max_tokens=%d)%n",
//                 storyId, outputFile, maxTokens);
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

//     private static String getEnvOrThrow(String key) {
//         String value = System.getenv(key);
//         if (value == null || value.isBlank()) {
//             throw new IllegalStateException("Missing environment variable: " + key);
//         }
//         return value;
//     }

//     // 🔴 NEW: optionally override max tokens via env var LLM_MAX_TOKENS
//     private static int getMaxTokensFromEnvOrDefault() {
//         String fromEnv = System.getenv("LLM_MAX_TOKENS");
//         if (fromEnv != null && !fromEnv.isBlank()) {
//             try {
//                 return Integer.parseInt(fromEnv.trim());
//             } catch (NumberFormatException ignored) {
//             }
//         }
//         return DEFAULT_MAX_TOKENS;
//     }
// }
