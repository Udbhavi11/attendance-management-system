package com.workforce.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyAttendanceReportDTO {

    private LocalDate reportDate;

    private long expectedEmployees;
    private long checkedInEmployees;
    private long absentEmployees;
    private long lateArrivals;
    private long employeesOnBreak;
    private long checkedOutEmployees;

    private List<DailyAttendanceEmployeeDTO> employees;

    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public DailyAttendanceReportDTO() {
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public long getExpectedEmployees() {
        return expectedEmployees;
    }

    public void setExpectedEmployees(long expectedEmployees) {
        this.expectedEmployees = expectedEmployees;
    }

    public long getCheckedInEmployees() {
        return checkedInEmployees;
    }

    public void setCheckedInEmployees(long checkedInEmployees) {
        this.checkedInEmployees = checkedInEmployees;
    }

    public long getAbsentEmployees() {
        return absentEmployees;
    }

    public void setAbsentEmployees(long absentEmployees) {
        this.absentEmployees = absentEmployees;
    }

    public long getLateArrivals() {
        return lateArrivals;
    }

    public void setLateArrivals(long lateArrivals) {
        this.lateArrivals = lateArrivals;
    }

    public long getEmployeesOnBreak() {
        return employeesOnBreak;
    }

    public void setEmployeesOnBreak(long employeesOnBreak) {
        this.employeesOnBreak = employeesOnBreak;
    }

    public long getCheckedOutEmployees() {
        return checkedOutEmployees;
    }

    public void setCheckedOutEmployees(long checkedOutEmployees) {
        this.checkedOutEmployees = checkedOutEmployees;
    }

    public List<DailyAttendanceEmployeeDTO> getEmployees() {
        return employees;
    }

    public void setEmployees(
            List<DailyAttendanceEmployeeDTO> employees) {
        this.employees = employees;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}