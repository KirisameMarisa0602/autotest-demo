package com.example.derivatives.trade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
    Optional<Trade> findByIdempotencyKey(String idempotencyKey);

    List<Trade> findByPortfolioCodeOrderByCreatedAtDesc(String portfolioCode);

    List<Trade> findByPortfolioCodeAndStatus(
            String portfolioCode,
            Trade.TradeStatus status
    );
}
