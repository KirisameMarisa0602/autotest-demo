package com.example.derivatives.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-events")
public class AuditController {
    private final AuditEventRepository repository;

    public AuditController(AuditEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('trader', 'risk_manager', 'viewer')")
    List<AuditEventResponse> findAll() {
        return repository.findAllByOrderByOccurredAtDescIdDesc().stream()
                .map(event -> new AuditEventResponse(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getActor(),
                        event.getOccurredAt(),
                        event.getDetails()))
                .toList();
    }

    public record AuditEventResponse(
            UUID id,
            String eventType,
            String aggregateType,
            String aggregateId,
            String actor,
            Instant occurredAt,
            String details
    ) {
    }
}
