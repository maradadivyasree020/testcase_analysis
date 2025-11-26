package com.example.tools;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TestResultSummaryGenerator {

    private static class TestCaseResult {
        String name;      // className.methodName
        boolean accepted; // true = ACCEPTED, false = REJECTED
    }

    public static void main(String[] args) throws Exception {
        String reportsDir = "target/surefire-reports";

        for (String arg : args) {
            if (arg.startsWith("--reportsDir=")) {
                reportsDir = arg.substring("--reportsDir=".length());
            }
        }

        Path dir = Paths.get(reportsDir);
        if (!Files.exists(dir)) {
            System.err.println("No surefire reports directory found: " + reportsDir);
            return;
        }

        List<TestCaseResult> results = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
            for (Path xmlFile : stream) {
                parseReportFile(xmlFile, results);
            }
        }

        int total = results.size();
        int acceptedCount = 0;

        int index = 1;
        for (TestCaseResult r : results) {
            if (r.accepted) acceptedCount++;
            String status = r.accepted ? "ACCEPTED" : "REJECTED";
            System.out.printf("%d. TEST CASE %d (%s): %s%n",
                    index, index, r.name, status);
            index++;
        }

        System.out.println("TOTAL TEST CASES: " + total);
        System.out.println("TOTAL ACCEPTED: " + acceptedCount);
        System.out.println("TOTAL REJECTED: " + (total - acceptedCount));
    }

    private static void parseReportFile(Path xmlFile, List<TestCaseResult> results) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(Files.newInputStream(xmlFile));

        doc.getDocumentElement().normalize();

        NodeList testCaseNodes = doc.getElementsByTagName("testcase");
        for (int i = 0; i < testCaseNodes.getLength(); i++) {
            Node node = testCaseNodes.item(i);
            String className = node.getAttributes().getNamedItem("classname").getNodeValue();
            String methodName = node.getAttributes().getNamedItem("name").getNodeValue();

            boolean hasFailureOrError = hasChild(node, "failure") || hasChild(node, "error");

            TestCaseResult r = new TestCaseResult();
            r.name = className + "." + methodName;
            r.accepted = !hasFailureOrError;
            results.add(r);
        }
    }

    private static boolean hasChild(Node node, String childName) {
        NodeList children = node.getChildNodes();
        for (int j = 0; j < children.getLength(); j++) {
            Node c = children.item(j);
            if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(childName)) {
                return true;
            }
        }
        return false;
    }
}
