package com.example.derivatives.audit;

import com.example.derivatives.security.ActorProvider;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final ActorProvider actorProvider;
    private final Clock clock;

    public AuditService(
            AuditEventRepository repository,
            ActorProvider actorProvider,
            Clock clock
    ) {
        this.repository = repository;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    public void record(
            String eventType,
            String aggregateType,
            String aggregateId,
            String details
    ) {
        repository.save(new AuditEvent(
                eventType,
                aggregateType,
                aggregateId,
                actorProvider.currentActor(),
                Instant.now(clock),
                details
        ));
    }
}
