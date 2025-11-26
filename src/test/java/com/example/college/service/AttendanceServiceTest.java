package com.example.college.service;

import com.example.college.model.AttendanceModel;
import com.example.college.model.EmployeeModel;
import com.example.college.repository.AttendanceRepo;
import com.example.college.repository.EmployeeRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Unit tests for AttendanceService
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepo attendanceRepo; 

    @Mock
    private EmployeeRepo employeeRepo;

    @InjectMocks
    private AttendanceService service;

    private LocalDate date;

    @BeforeEach
    void setUp() {
        date = LocalDate.of(2025, 6, 1);
    }

    @Test
    void markSingleAttendance_createsNewAttendance_andUpdatesEmployeeCounters_whenNoExisting() {
        Long empId = 10L;
        when(attendanceRepo.findByEmployeeIdAndDate(empId, date)).thenReturn(Optional.empty());

        EmployeeModel emp = new EmployeeModel();
        emp.setId(empId);
        emp.setTotalPresent(0);
        emp.setTotalAbsent(0);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(emp));

        AttendanceModel created = new AttendanceModel(empId, date, true);
        created.setId(55L);
        when(attendanceRepo.save(any(AttendanceModel.class))).thenReturn(created);

        AttendanceModel result = service.markSingleAttendance(empId, date, true);

        assertThat(result.getId()).isEqualTo(55L);
        // employee counters updated: absent increments by 1
        assertThat(emp.getTotalAbsent()).isEqualTo(1);
        assertThat(emp.getTotalPresent()).isEqualTo(0);
        verify(employeeRepo, times(1)).save(emp);
    }

    @Test
    void markSingleAttendance_updatesExisting_and_switchesCounters_whenStateChanges() {
        Long empId = 20L;
        AttendanceModel existing = new AttendanceModel(empId, date, true);
        existing.setId(66L);
        when(attendanceRepo.findByEmployeeIdAndDate(empId, date)).thenReturn(Optional.of(existing));

        EmployeeModel emp = new EmployeeModel();
        emp.setId(empId);
        emp.setTotalPresent(5);
        emp.setTotalAbsent(3);
        when(employeeRepo.findById(empId)).thenReturn(Optional.of(emp));

        // saving returns the updated record
        when(attendanceRepo.save(any(AttendanceModel.class))).thenAnswer(inv -> inv.getArgument(0));

        AttendanceModel res = service.markSingleAttendance(empId, date, false);

        // existing was absent=true, now set to false => absent--, present++
        assertThat(res.getAbsent()).isFalse();
        assertThat(emp.getTotalAbsent()).isEqualTo(2); // 3 - 1
        assertThat(emp.getTotalPresent()).isEqualTo(6); // 5 + 1
        verify(employeeRepo).save(emp);
    }

    @Test
    void markSingleAttendance_handlesMissingEmployee() {
        Long empId = 99L;
        when(attendanceRepo.findByEmployeeIdAndDate(empId, date)).thenReturn(Optional.empty());
        when(attendanceRepo.save(any(AttendanceModel.class))).thenAnswer(inv -> {
            AttendanceModel a = inv.getArgument(0);
            a.setId(999L);
            return a;
        });
        when(employeeRepo.findById(empId)).thenReturn(Optional.empty());

        AttendanceModel out = service.markSingleAttendance(empId, date, false);
        assertThat(out.getId()).isEqualTo(999L);
        // since employee missing, no employeeRepo.save() should be called
        verify(employeeRepo, never()).save(any());
    }
}
