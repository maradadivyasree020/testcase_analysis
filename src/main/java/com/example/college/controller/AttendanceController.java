package com.example.college.controller;

import com.example.college.model.AttendanceModel;
import com.example.college.repository.AttendanceRepo;
import com.example.college.service.AttendanceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceRepo repo;
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceRepo repo, AttendanceService attendanceService) {
        this.repo = repo;
        this.attendanceService = attendanceService;
    }

    public static class AttendanceDTO {
        private Long employeeId;
        private Boolean absent;
        private LocalDate date;
        public AttendanceDTO() {}
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public Boolean getAbsent() { return absent; }
        public void setAbsent(Boolean absent) { this.absent = absent; }
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
    }

    @PutMapping("/mark")
    public ResponseEntity<?> markSingle(@RequestBody AttendanceDTO dto) {
        if (dto == null || dto.getEmployeeId() == null || dto.getAbsent() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "employeeId and absent required"));
        }

        LocalDate markDate = dto.getDate() == null ? LocalDate.now() : dto.getDate();
        AttendanceModel result = attendanceService.markSingleAttendance(dto.getEmployeeId(), markDate, dto.getAbsent());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/mark-batch")
    @Transactional
    public ResponseEntity<Map<String, Object>> markBatch(@RequestBody List<AttendanceDTO> payload) {
        if (payload == null || payload.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "payload is empty"));
        }

        List<AttendanceModel> saved = new ArrayList<>();
        List<Long> invalid = new ArrayList<>();

        for (AttendanceDTO dto : payload) {
            if (dto == null || dto.getEmployeeId() == null || dto.getAbsent() == null) {
                if (dto != null && dto.getEmployeeId() != null) invalid.add(dto.getEmployeeId());
                continue;
            }

            LocalDate markDate = dto.getDate() == null ? LocalDate.now() : dto.getDate();

            AttendanceModel rec = attendanceService.markSingleAttendance(dto.getEmployeeId(), markDate, dto.getAbsent());
            saved.add(rec);
        }

        long absentCount = saved.stream().filter(AttendanceModel::getAbsent).count();
        long presentCount = saved.size() - absentCount;

        Map<String, Object> result = new HashMap<>();
        result.put("savedCount", saved.size());
        result.put("savedRecords", saved);
        result.put("presentCount", presentCount);
        result.put("absentCount", absentCount);
        result.put("invalidEntries", invalid);

        return ResponseEntity.ok(result);
    }

    // @GetMapping("/employee/{employeeId}")
    // public ResponseEntity<List<AttendanceModel>> getAttendanceFor(@PathVariable Long employeeId,
    //                                                               @RequestParam(name = "from", required = false) LocalDate from,
    //                                                               @RequestParam(name = "to", required = false) LocalDate to) {
    //     if (employeeId == null) return ResponseEntity.badRequest().build();

    //     List<AttendanceModel> list;
    //     if (from != null && to != null) {
    //         list = repo.findByEmployeeIdAndDateBetween(employeeId, from, to);
    //     } else {
    //         list = repo.findByEmployeeIdOrderByDateDesc(employeeId);
    //     }
    //     return ResponseEntity.ok(list);
    // }
}
