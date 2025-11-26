package com.example.college.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.college.model.AttendanceModel;
import com.example.college.model.EmployeeModel;
import com.example.college.repository.AttendanceRepo;
import com.example.college.repository.EmployeeRepo;

@Service
public class AttendanceService {
    private final AttendanceRepo attendanceRepo;
    private final EmployeeRepo employeeRepo;

    public AttendanceService(AttendanceRepo attendanceRepo, EmployeeRepo employeeRepo) {
        this.attendanceRepo = attendanceRepo;
        this.employeeRepo = employeeRepo;
    }

    @Transactional
    public AttendanceModel markSingleAttendance(Long employeeId, LocalDate date, Boolean absent) {
        LocalDate markDate = (date == null ? LocalDate.now() : date);

        // find existing attendance
        Optional<AttendanceModel> existingOpt = attendanceRepo.findByEmployeeIdAndDate(employeeId, markDate);

        AttendanceModel saved;
        Boolean previousAbsent = null;

        if (existingOpt.isPresent()) {
            AttendanceModel existing = existingOpt.get();
            previousAbsent = existing.getAbsent();
            existing.setAbsent(absent);
            saved = attendanceRepo.save(existing);
        } else {
            AttendanceModel created = new AttendanceModel(employeeId, markDate, absent);
            saved = attendanceRepo.save(created);
        }

        // update employee counters — ensure employee id types align (see note below)
        Optional<EmployeeModel> eOpt = employeeRepo.findById(employeeId); // EmployeeRepo should use Long id

        if (eOpt.isPresent()) {
            EmployeeModel employee = eOpt.get();

            if (employee.getTotalPresent() == null) employee.setTotalPresent(0);
            if (employee.getTotalAbsent() == null) employee.setTotalAbsent(0);

            if (previousAbsent == null) {
                if (absent) employee.setTotalAbsent(employee.getTotalAbsent() + 1);
                else employee.setTotalPresent(employee.getTotalPresent() + 1);
            } else if (!previousAbsent.equals(absent)) {
                if (previousAbsent) {
                    // was absent, now present
                    employee.setTotalAbsent(employee.getTotalAbsent() - 1);
                    employee.setTotalPresent(employee.getTotalPresent() + 1);
                } else {
                    // was present, now absent
                    employee.setTotalPresent(employee.getTotalPresent() - 1);
                    employee.setTotalAbsent(employee.getTotalAbsent() + 1);
                }
            }

            employee.setLastAttendanceDate(markDate);
            employeeRepo.save(employee);
        }

        return saved;
    }
}
