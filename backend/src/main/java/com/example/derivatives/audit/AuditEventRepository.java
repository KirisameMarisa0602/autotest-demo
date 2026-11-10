package com.example.derivatives.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findAllByOrderByOccurredAtDescIdDesc();

    List<AuditEvent> findByAggregateTypeAndAggregateIdOrderByOccurredAtDescIdDesc(
            String aggregateType,
            String aggregateId
    );
}
