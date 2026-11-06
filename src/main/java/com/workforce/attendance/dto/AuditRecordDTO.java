package com.workforce.attendance.dto;

import java.time.Instant;

public class AuditRecordDTO {

    private Long id;
    private String actorUsername;
    private String action;
    private String targetRecord;
    private Instant timestamp;
    private String relevantChanges;
    private String requestCorrelationId;

    public AuditRecordDTO() {
    }

    public AuditRecordDTO(
            Long id,
            String actorUsername,
            String action,
            String targetRecord,
            Instant timestamp,
            String relevantChanges,
            String requestCorrelationId) {

        this.id = id;
        this.actorUsername = actorUsername;
        this.action = action;
        this.targetRecord = targetRecord;
        this.timestamp = timestamp;
        this.relevantChanges = relevantChanges;
        this.requestCorrelationId = requestCorrelationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetRecord() {
        return targetRecord;
    }

    public void setTargetRecord(String targetRecord) {
        this.targetRecord = targetRecord;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getRelevantChanges() {
        return relevantChanges;
    }

    public void setRelevantChanges(String relevantChanges) {
        this.relevantChanges = relevantChanges;
    }

    public String getRequestCorrelationId() {
        return requestCorrelationId;
    }

    public void setRequestCorrelationId(String requestCorrelationId) {
        this.requestCorrelationId = requestCorrelationId;
    }
}