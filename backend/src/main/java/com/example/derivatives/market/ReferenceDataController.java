package com.example.derivatives.market;

import com.example.derivatives.portfolio.PortfolioRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferenceDataController {
    private final MarketSnapshotRepository marketRepository;
    private final PortfolioRepository portfolioRepository;

    public ReferenceDataController(
            MarketSnapshotRepository marketRepository,
            PortfolioRepository portfolioRepository
    ) {
        this.marketRepository = marketRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @GetMapping("/market-data")
    List<MarketDataResponse> marketData() {
        return marketRepository.findAll().stream()
                .map(snapshot -> new MarketDataResponse(
                        snapshot.getUnderlying(),
                        snapshot.getSpot(),
                        snapshot.getVolatility(),
                        snapshot.getRiskFreeRate(),
                        snapshot.getAsOf()))
                .toList();
    }

    @GetMapping("/portfolios")
    List<PortfolioResponse> portfolios() {
        return portfolioRepository.findAll().stream()
                .map(portfolio -> new PortfolioResponse(portfolio.getCode(), portfolio.getName()))
                .toList();
    }

    public record MarketDataResponse(
            String underlying,
            BigDecimal spot,
            BigDecimal volatility,
            BigDecimal riskFreeRate,
            Instant asOf
    ) {
    }

    public record PortfolioResponse(String code, String name) {
    }
}
