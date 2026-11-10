package com.example.derivatives.demo;

import com.example.derivatives.audit.AuditEventRepository;
import com.example.derivatives.audit.AuditService;
import com.example.derivatives.market.MarketSnapshot;
import com.example.derivatives.market.MarketSnapshotRepository;
import com.example.derivatives.portfolio.Portfolio;
import com.example.derivatives.portfolio.PortfolioRepository;
import com.example.derivatives.trade.TradeRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@Profile("demo")
public class DemoDataService {
    private final TradeRepository tradeRepository;
    private final AuditEventRepository auditRepository;
    private final MarketSnapshotRepository marketRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;
    private final Clock clock;

    public DemoDataService(
            TradeRepository tradeRepository,
            AuditEventRepository auditRepository,
            MarketSnapshotRepository marketRepository,
            PortfolioRepository portfolioRepository,
            AuditService auditService,
            Clock clock
    ) {
        this.tradeRepository = tradeRepository;
        this.auditRepository = auditRepository;
        this.marketRepository = marketRepository;
        this.portfolioRepository = portfolioRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public ResetResponse reset() {
        tradeRepository.deleteAllInBatch();
        auditRepository.deleteAllInBatch();
        marketRepository.deleteAllInBatch();
        portfolioRepository.deleteAllInBatch();

        Instant now = Instant.now(clock);
        portfolioRepository.save(new Portfolio("ALPHA", "Alpha Demo Portfolio"));
        portfolioRepository.save(new Portfolio("BETA", "Beta Demo Portfolio"));
        marketRepository.save(new MarketSnapshot(
                "AAPL", new BigDecimal("100.00"), new BigDecimal("0.20"),
                new BigDecimal("0.03"), now));
        marketRepository.save(new MarketSnapshot(
                "MSFT", new BigDecimal("250.00"), new BigDecimal("0.24"),
                new BigDecimal("0.03"), now));
        auditService.record("DEMO_RESET", "DEMO", "seed",
                "portfolios=2;marketSnapshots=2");
        return new ResetResponse(LocalDate.now(clock), 2, 2);
    }

    public record ResetResponse(
            LocalDate businessDate,
            int portfolioCount,
            int marketSnapshotCount
    ) {
    }
}
