package com.example.college.model;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
public class EmployeeModel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String role;

    private Boolean absent;

    @Column(name = "total_present", nullable = false)
    private Integer totalPresent = 0;

    @Column(name = "total_absent", nullable = false)
    private Integer totalAbsent = 0;

    @Column(name = "last_attendance_date")
    private LocalDate lastAttendanceDate;

    public EmployeeModel() {}
    public EmployeeModel(String name, Integer annualLeaveEntitlement) {
        this.name = name;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() {return role;}
    public Boolean getAbsent(){return absent;}
    public Integer getTotalPresent() { return totalPresent; }
    public void setTotalPresent(Integer totalPresent) { this.totalPresent = totalPresent; }
    public Integer getTotalAbsent() { return totalAbsent; }
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setRole(String role) {this.role=role;}
    public void setAbsent(Boolean absent){this.absent=absent;}
    public void setTotalAbsent(Integer totalAbsent) { this.totalAbsent = totalAbsent; }
    public LocalDate getLastAttendanceDate() { return lastAttendanceDate; }
    public void setLastAttendanceDate(LocalDate lastAttendanceDate) { this.lastAttendanceDate = lastAttendanceDate; }
}
