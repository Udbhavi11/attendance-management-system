package com.workforce.attendance.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workforce.attendance.dto.AuditRecordDTO;
import com.workforce.attendance.entity.AuditRecord;
import com.workforce.attendance.repository.AuditRecordRepository;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

@RestController
@RequestMapping("/api/audit-records")
public class AuditRecordController {

    private static final ZoneId REPORT_ZONE =
            ZoneId.of("Asia/Kolkata");

    private final AuditRecordRepository auditRecordRepository;

    public AuditRecordController(
            AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<AuditRecordDTO>> getAuditRecords(

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @RequestParam(required = false)
            String action,

            @RequestParam(required = false)
            String performedBy,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100");
        }

        if (startDate != null && endDate != null
                && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }

        Instant start = startDate == null
                ? null
                : startDate.atStartOfDay(REPORT_ZONE).toInstant();

        Instant endExclusive = endDate == null
                ? null
                : endDate.plusDays(1)
                        .atStartOfDay(REPORT_ZONE)
                        .toInstant();

        String actionFilter = normalize(action);
        String actorFilter = normalize(performedBy);

        Specification<AuditRecord> specification =
                (root, query, cb) -> {

                    Predicate predicate = cb.conjunction();

                    if (start != null) {
                        predicate = cb.and(
                                predicate,
                                cb.greaterThanOrEqualTo(
                                        root.get("timestamp"), start));
                    }

                    if (endExclusive != null) {
                        predicate = cb.and(
                                predicate,
                                cb.lessThan(
                                        root.get("timestamp"), endExclusive));
                    }

                    if (actionFilter != null) {
                        predicate = cb.and(
                                predicate,
                                cb.like(
                                        cb.lower(root.get("action")),
                                        "%" + actionFilter + "%"));
                    }

                    if (actorFilter != null) {
                        var actor = root.join("actor", JoinType.LEFT);

                        predicate = cb.and(
                                predicate,
                                cb.like(
                                        cb.lower(actor.get("username")),
                                        "%" + actorFilter + "%"));
                    }

                    return predicate;
                };

        Page<AuditRecordDTO> response =
                auditRecordRepository.findAll(
                        specification,
                        PageRequest.of(page, size))
                .map(this::convertToDTO);

        return ResponseEntity.ok(response);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private AuditRecordDTO convertToDTO(AuditRecord record) {

        String actorUsername = record.getActor() == null
                ? "System"
                : record.getActor().getUsername();

        return new AuditRecordDTO(
                record.getId(),
                actorUsername,
                record.getAction(),
                record.getTargetRecord(),
                record.getTimestamp(),
                record.getRelevantChanges(),
                record.getRequestCorrelationId());
    }
}