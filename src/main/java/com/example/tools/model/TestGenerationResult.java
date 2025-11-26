package com.example.tools.model;

import java.util.List;

public class TestGenerationResult {

    private String reportMarkdown;
    private List<GeneratedTest> tests;

    public String getReportMarkdown() {
        return reportMarkdown;
    }

    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
    }

    public List<GeneratedTest> getTests() {
        return tests;
    }

    public void setTests(List<GeneratedTest> tests) {
        this.tests = tests;
    }

    public static class GeneratedTest {
        private String className;
        private String javaCode;

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public String getJavaCode() {
            return javaCode;
        }

        public void setJavaCode(String javaCode) {
            this.javaCode = javaCode;
        }
    }
}
