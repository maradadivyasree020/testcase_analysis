package com.example.tools.spec;

import java.util.List;

public class TestSpec {

    private String storyId;
    private ApiSpec api;
    private List<TestCase> tests;

    public String getStoryId() { return storyId; }
    public void setStoryId(String storyId) { this.storyId = storyId; }

    public ApiSpec getApi() { return api; }
    public void setApi(ApiSpec api) { this.api = api; }

    public List<TestCase> getTests() { return tests; }
    public void setTests(List<TestCase> tests) { this.tests = tests; }

    public static class ApiSpec {
        private String method;
        private String path;

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
    }

    public static class TestCase {
        private int id;
        private String name;
        private String description;
        private String requestBody;
        private int expectedStatus;
        private List<ExpectedJson> expectedJson;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getRequestBody() { return requestBody; }
        public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

        public int getExpectedStatus() { return expectedStatus; }
        public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }

        public List<ExpectedJson> getExpectedJson() { return expectedJson; }
        public void setExpectedJson(List<ExpectedJson> expectedJson) { this.expectedJson = expectedJson; }
    }

    public static class ExpectedJson {
        private String path;
        private Object value;

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }
}
