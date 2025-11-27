package com.example.tools;

import com.example.tools.spec.TestSpec;
import org.w3c.dom.*;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TestResultSummaryGenerator {

    private static class TestCaseOutcome {
        int id;
        String description;
        boolean accepted;
        String reason; // if rejected
    }

    public static void main(String[] args) throws Exception {
        String reportsDir = "target/surefire-reports";
        String specPath = "/qa/ATT-123-tests.yaml";

        // 1. Load spec to get test descriptions
        TestSpec spec;
        try (InputStream is = TestResultSummaryGenerator.class.getResourceAsStream(specPath)) {
            if (is == null) {
                System.err.println("Spec file not found on classpath: " + specPath);
                return;
            }
            spec = new Yaml().loadAs(is, TestSpec.class);
        }

        // Map of outcomes in order of spec
        List<TestCaseOutcome> outcomes = new ArrayList<>();
        for (TestSpec.TestCase tc : spec.getTests()) {
            TestCaseOutcome o = new TestCaseOutcome();
            o.id = tc.getId();
            o.description = tc.getDescription();
            o.accepted = true; // assume pass, flip if failure found
            o.reason = "";
            outcomes.add(o);
        }

        // 2. Read surefire XML for SpecDrivenAttendanceApiTest
        Path dir = Paths.get(reportsDir);
        if (!Files.exists(dir)) {
            System.err.println("No surefire reports directory found: " + reportsDir);
            return;
        }

        // Find the XML for our spec-driven test class
        Path specXml = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "TEST-*.xml")) {
            for (Path xml : stream) {
                String content = Files.readString(xml);
                if (content.contains("SpecDrivenAttendanceApiTest")) {
                    specXml = xml;
                    break;
                }
            }
        }

        if (specXml == null) {
            System.err.println("No surefire XML found for SpecDrivenAttendanceApiTest");
            return;
        }

        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Files.newInputStream(specXml));
        doc.getDocumentElement().normalize();

        NodeList testCaseNodes = doc.getElementsByTagName("testcase");

        // We assume order of JUnit parameterized execution == order in spec
        for (int i = 0; i < testCaseNodes.getLength() && i < outcomes.size(); i++) {
            Node node = testCaseNodes.item(i);
            TestCaseOutcome o = outcomes.get(i);

            Node failureNode = getChild(node, "failure");
            Node errorNode = getChild(node, "error");

            if (failureNode != null || errorNode != null) {
                o.accepted = false;
                Node fail = (failureNode != null) ? failureNode : errorNode;
                String msg = "";
                if (fail.getAttributes() != null && fail.getAttributes().getNamedItem("message") != null) {
                    msg = fail.getAttributes().getNamedItem("message").getNodeValue();
                } else {
                    msg = fail.getTextContent();
                }
                o.reason = msg == null ? "" : msg.trim();
            }
        }

        // 3. Print final summary
        int acceptedCount = 0;
        for (int i = 0; i < outcomes.size(); i++) {
            TestCaseOutcome o = outcomes.get(i);
            String status = o.accepted ? "ACCEPTED" : "REJECTED";
            if (o.accepted) acceptedCount++;
            System.out.printf("TC %d: %s : %s", o.id, o.description, status);
            if (!o.accepted && !o.reason.isBlank()) {
                System.out.printf(" (reason: %s)", o.reason);
            }
            System.out.println();
        }

        System.out.println("TOTAL TEST CASES: " + outcomes.size());
        System.out.println("TOTAL ACCEPTED: " + acceptedCount);
        System.out.println("TOTAL REJECTED: " + (outcomes.size() - acceptedCount));
    }

    private static Node getChild(Node node, String name) {
        NodeList children = node.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node c = children.item(j);
            if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(name)) {
                return c;
            }
        }
        return null;
    }
}
