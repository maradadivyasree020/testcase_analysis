
package com.example.college.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.college.model.AttendanceModel;

public interface AttendanceRepo extends JpaRepository<AttendanceModel, Long> {
    Optional<AttendanceModel> findByEmployeeIdAndDate(Long employeeId, LocalDate date);
    List<AttendanceModel> findByEmployeeIdOrderByDateDesc(Long employeeId);
    List<AttendanceModel> findByEmployeeIdAndDateBetween(Long employeeId, LocalDate from, LocalDate to);
}
