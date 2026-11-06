
package com.workforce.attendance.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "audit_records")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private UserAccount actor;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String action;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String targetRecord;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String relevantChanges;

    @Size(max = 100)
    @Column(length = 100)
    private String requestCorrelationId;

    public AuditRecord() {
    	super();
    }
	public AuditRecord(Long id, UserAccount actor, String action,
			String targetRecord, Instant timestamp, String relevantChanges,
			String requestCorrelationId) {
		super();
		this.id = id;
		this.actor = actor;
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
	public UserAccount getActor() {
		return actor;
	}
	public void setActor(UserAccount actor) {
		this.actor = actor;
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
