package com.workforce.attendance.dto;

public class FieldVisitTypeBreakdownDTO {

    private String visitType;
    private long count;

    public FieldVisitTypeBreakdownDTO() {
    }

    public FieldVisitTypeBreakdownDTO(String visitType, long count) {
        this.visitType = visitType;
        this.count = count;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}