package com.workforce.attendance.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.workforce.attendance.entity.AuditRecord;

@Repository
public interface AuditRecordRepository
        extends JpaRepository<AuditRecord, Long>,
                JpaSpecificationExecutor<AuditRecord> {

    @Override
    @EntityGraph(attributePaths = {"actor"})
    Page<AuditRecord> findAll(
            Specification<AuditRecord> specification,
            Pageable pageable);
}