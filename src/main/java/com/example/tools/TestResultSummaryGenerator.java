// package com.example.tools;

// import org.w3c.dom.Document;
// import org.w3c.dom.Node;
// import org.w3c.dom.NodeList;

// import javax.xml.parsers.DocumentBuilderFactory;
// import java.nio.file.*;
// import java.util.ArrayList;
// import java.util.List;

// public class TestResultSummaryGenerator {

//     private static class TestCaseResult {
//         String name;      // className.methodName
//         boolean accepted; // true = ACCEPTED, false = REJECTED
//     }

//     public static void main(String[] args) throws Exception {
//         String reportsDir = "target/surefire-reports";

//         for (String arg : args) {
//             if (arg.startsWith("--reportsDir=")) {
//                 reportsDir = arg.substring("--reportsDir=".length());
//             }
//         }

//         Path dir = Paths.get(reportsDir);
//         if (!Files.exists(dir)) {
//             System.err.println("No surefire reports directory found: " + reportsDir);
//             return;
//         }

//         List<TestCaseResult> results = new ArrayList<>();

//         try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
//             for (Path xmlFile : stream) {
//                 parseReportFile(xmlFile, results);
//             }
//         }

//         int total = results.size();
//         int acceptedCount = 0;

//         int index = 1;
//         for (TestCaseResult r : results) {
//             if (r.accepted) acceptedCount++;
//             String status = r.accepted ? "ACCEPTED" : "REJECTED";
//             System.out.printf("%d. TEST CASE %d (%s): %s%n",
//                     index, index, r.name, status);
//             index++;
//         }

//         System.out.println("TOTAL TEST CASES: " + total);
//         System.out.println("TOTAL ACCEPTED: " + acceptedCount);
//         System.out.println("TOTAL REJECTED: " + (total - acceptedCount));
//     }

//     private static void parseReportFile(Path xmlFile, List<TestCaseResult> results) throws Exception {
//         Document doc = DocumentBuilderFactory.newInstance()
//                 .newDocumentBuilder()
//                 .parse(Files.newInputStream(xmlFile));

//         doc.getDocumentElement().normalize();

//         NodeList testCaseNodes = doc.getElementsByTagName("testcase");
//         for (int i = 0; i < testCaseNodes.getLength(); i++) {
//             Node node = testCaseNodes.item(i);
//             String className = node.getAttributes().getNamedItem("classname").getNodeValue();
//             String methodName = node.getAttributes().getNamedItem("name").getNodeValue();

//             boolean hasFailureOrError = hasChild(node, "failure") || hasChild(node, "error");

//             TestCaseResult r = new TestCaseResult();
//             r.name = className + "." + methodName;
//             r.accepted = !hasFailureOrError;
//             results.add(r);
//         }
//     }

//     private static boolean hasChild(Node node, String childName) {
//         NodeList children = node.getChildNodes();
//         for (int j = 0; j < children.getLength(); j++) {
//             Node c = children.item(j);
//             if (c.getNodeType() == Node.ELEMENT_NODE && c.getNodeName().equals(childName)) {
//                 return true;
//             }
//         }
//         return false;
//     }
// }

package com.example.tools;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class TestResultSummaryGenerator {

    private static final Map<String, String> DESCRIPTION_MAP = Map.ofEntries(
            Map.entry("markBatch_success_savesMultipleAndReturnsSummary",
                    "Valid batch → returns correct summary"),
            Map.entry("markBatch_badRequest_whenPayloadEmpty",
                    "Empty batch → BAD_REQUEST"),
            Map.entry("markBatch_skipsInvalidEntries_andReportsInvalidIds",
                    "Partial invalid students → skip & report"),
            Map.entry("markSingle_success_returnsSavedRecord",
                    "Mark single student → saved & returned"),
            Map.entry("markSingle_badRequest_whenMissingFields",
                    "Missing fields → BAD_REQUEST"),
            Map.entry("getEmployees_returnsAll",
                    "Returns all employees"),
            Map.entry("getEmployee_returnsOptionalWrappedModel",
                    "Returns specific employee"),
            Map.entry("postAndPut_andDelete_callRepo",
                    "Post/Put/Delete call repository correctly")
    );

    private static final Map<String, String> FUNCTION_MAP = Map.ofEntries(
            Map.entry("markBatch_success_savesMultipleAndReturnsSummary",
                    "AttendanceService.markBatch(), AttendanceRepository.save(), StudentRepository.findById()"),
            Map.entry("markBatch_badRequest_whenPayloadEmpty",
                    "AttendanceService.markBatch()"),
            Map.entry("markBatch_skipsInvalidEntries_andReportsInvalidIds",
                    "AttendanceService.markBatch(), StudentRepository.findById()"),
            Map.entry("markSingle_success_returnsSavedRecord",
                    "AttendanceService.markSingle(), AttendanceRepository.save()"),
            Map.entry("markSingle_badRequest_whenMissingFields",
                    "Validation layer"),
            Map.entry("getEmployees_returnsAll",
                    "EmployeeService.getAll(), EmployeeRepository.findAll()"),
            Map.entry("getEmployee_returnsOptionalWrappedModel",
                    "EmployeeService.getEmployeeById(), EmployeeRepository.findById()"),
            Map.entry("postAndPut_andDelete_callRepo",
                    "EmployeeService CRUD, EmployeeRepository")
    );

    public static void main(String[] args) throws Exception {
        File reportDir = new File("target/surefire-reports");
        File[] xmlFiles = reportDir.listFiles((d, n) -> n.endsWith(".xml"));

        if (xmlFiles == null) {
            System.out.println("No test reports found.");
            return;
        }

        int total = 0, accepted = 0, rejected = 0;
        int index = 1;

        for (File xml : xmlFiles) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document doc = dbf.newDocumentBuilder().parse(xml);
            NodeList testcases = doc.getElementsByTagName("testcase");

            for (int i = 0; i < testcases.getLength(); i++) {
                Element tc = (Element) testcases.item(i);
                total++;

                String className = tc.getAttribute("classname");
                String methodName = tc.getAttribute("name");

                boolean failed = tc.getElementsByTagName("failure").getLength() > 0 ||
                                 tc.getElementsByTagName("error").getLength() > 0;

                String status = failed ? "REJECTED" : "ACCEPTED";
                if (failed) rejected++; else accepted++;

                String description = DESCRIPTION_MAP.getOrDefault(methodName, "No description available");
                String functions = FUNCTION_MAP.getOrDefault(methodName, "Not mapped");

                System.out.printf("TEST CASE %d:\n", index++);
                System.out.println("  Description: " + description);
                System.out.println("  Class: " + className);
                System.out.println("  Method: " + methodName);
                System.out.println("  Called Functions: " + functions);
                System.out.println("  Status: " + status);
                System.out.println();
            }
        }

        System.out.println("TOTAL TEST CASES: " + total);
        System.out.println("TOTAL ACCEPTED: " + accepted);
        System.out.println("TOTAL REJECTED: " + rejected);
    }
}
