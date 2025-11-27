# Mark Batch Attendance API: QA Test Plan

## 1. Executive Summary of Test Scenarios

This test plan covers the "Mark batch attendance" API designed for faculty usage to record the attendance of a class conveniently. The goals of this testing are to ensure that the API correctly handles valid scenarios and gracefully degrades upon receiving invalid input while maintaining data security and managing high loads.

## 2. Positive Test Cases and Expected Outcomes

### TC-P-001: Valid Request with Active Students

- **Description:** Mark attendance for a batch of active students on a valid date where the subject belongs to the faculty.
- **Input Parameters:**
  - `date: "2023-12-12"`
  - `subjectId: "subj001"`
  - `entries: [{"studentId": "stu100", "present": true}, {"studentId": "stu101", "present": false}]`
- **Expected Outcome:** Response Status 200 with totals and marked students count updated accurately, with an empty `invalidStudentIds` list.

### TC-P-002: Valid Request with All Students Absent

- **Description:** Test with all students marked as absent.
- **Input Parameters:**
  - `date: "2023-12-10"`
  - `subjectId: "subj002"`
  - `entries: [{"studentId": "stu102", "present": false}, {"studentId": "stu103", "present": false}]`
- **Expected Outcome:** Response Status 200 where `marked` count is zero and `total` represents the number of students.

### TC-P-003: Subject With No Enrollment

- **Description:** Submit attendance for a subject with no students enrolled.
- **Input Parameters:**
  - `date: "2023-12-11"`
  - `subjectId: "subj003"`
  - `entries: []`
- **Expected Outcome:** Response Status 400 with code `EMPTY_BATCH`.

## 3. Negative Test Cases and Error Handling

### TC-N-001: Future Attendance Date

- **Description:** Attempt to mark attendance for a future date.
- **Input Parameters:**
  - `date: "2024-01-01"`
  - `subjectId: "subj004"`
  - `entries: [{"studentId": "stu104", "present": true}]`
- **Expected Outcome:** Response Status 400 with context-specific error details.

### TC-N-002: Subject Not Owned by Faculty

- **Description:** Submit attendance for a subject not owned by the signed-in faculty member.
- **Input Parameters:**
  - `date: "2023-12-12"`
  - `subjectId: "subj999"`
  - `entries: [{"studentId": "stu105", "present": true}]`
- **Expected Outcome:** Response Status 404 with code `SUBJECT_NOT_FOUND`.

## 4. Edge Cases and Boundary Value Analysis

### TC-E-001: Duplicate Student IDs in Entries

- **Description:** Check system behavior when duplicate student IDs are provided in the entries.
- **Input Parameters:**
  - `date: "2023-12-12"`
  - `subjectId: "subj005"`
  - `entries: [{"studentId": "stu106", "present": true}, {"studentId": "stu106", "present": false}]`
- **Expected Outcome:** Ensure system handles duplicates appropriately, possibly ignoring or flagging duplicates.

### TC-E-002: Large Batch of 500 Students

- **Description:** Test system performance and accuracy with a large batch of 500 students.
- **Input Parameters:**
  - `date: "2023-12-12"`
  - `subjectId: "subj006"`
  - A generated list of 500 students marked present or absent.
- **Expected Outcome:** Response Status 200 with appropriate counts and without system delays or errors.

## 5. Performance and Stress Scenarios

### TC-PS-001: High Concurrency Test

- **Description:** Test API behavior under high levels of concurrent access.
- **Expected Outcome:** API should handle multiple requests without significant performance degradation or data inaccuracies.

## 6. Security Considerations

### TC-S-001: Unauthorized Access Attempt

- **Description:** Attempt API access with invalid authentication tokens.
- **Expected Outcome:** Response Status 401 or 403, ensuring unauthorized users cannot access or alter attendance data.

### TC-S-002: Injection Attack Vulnerability Test

- **Description:** Test for potential security breaches by injecting malicious code into input fields.
- **Expected Outcome:** API should properly sanitize inputs and prevent injection, ensuring system stability and security.

This comprehensive plan strives to cover functional correctness, performance, and security aspects of the "Mark batch attendance" API, thereby supporting its deployment in a production environment.