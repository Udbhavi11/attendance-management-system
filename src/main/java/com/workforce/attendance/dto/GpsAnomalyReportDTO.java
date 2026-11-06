package com.workforce.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public class GpsAnomalyReportDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    private long outsideGeofenceEvents;
    private long poorAccuracyEvents;
    private long delayedOfflineEvents;
    private long duplicateRequestAttempts;
    private long eventsAwaitingReview;

    private List<AnomalyFlagDTO> anomalies;

    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public long getOutsideGeofenceEvents() {
        return outsideGeofenceEvents;
    }

    public void setOutsideGeofenceEvents(long outsideGeofenceEvents) {
        this.outsideGeofenceEvents = outsideGeofenceEvents;
    }

    public long getPoorAccuracyEvents() {
        return poorAccuracyEvents;
    }

    public void setPoorAccuracyEvents(long poorAccuracyEvents) {
        this.poorAccuracyEvents = poorAccuracyEvents;
    }

    public long getDelayedOfflineEvents() {
        return delayedOfflineEvents;
    }

    public void setDelayedOfflineEvents(long delayedOfflineEvents) {
        this.delayedOfflineEvents = delayedOfflineEvents;
    }

    public long getDuplicateRequestAttempts() {
        return duplicateRequestAttempts;
    }

    public void setDuplicateRequestAttempts(long duplicateRequestAttempts) {
        this.duplicateRequestAttempts = duplicateRequestAttempts;
    }

    public long getEventsAwaitingReview() {
        return eventsAwaitingReview;
    }

    public void setEventsAwaitingReview(long eventsAwaitingReview) {
        this.eventsAwaitingReview = eventsAwaitingReview;
    }

    public List<AnomalyFlagDTO> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(List<AnomalyFlagDTO> anomalies) {
        this.anomalies = anomalies;
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