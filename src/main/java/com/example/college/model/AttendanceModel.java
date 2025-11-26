package com.example.college.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "attendance",uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "date"}))
public class AttendanceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Boolean absent;   // true = absent, false = present

    public AttendanceModel() {}

    public AttendanceModel(Long employeeId, LocalDate date, Boolean absent) {
        this.employeeId = employeeId;
        this.date = date;
        this.absent = absent;
    }

    // GETTERS
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public LocalDate getDate() { return date; }
    public Boolean getAbsent() { return absent; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setAbsent(Boolean absent) { this.absent = absent; }
}
