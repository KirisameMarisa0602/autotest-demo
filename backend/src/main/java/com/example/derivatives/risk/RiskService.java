package com.example.derivatives.risk;

import com.example.derivatives.market.MarketSnapshotRepository;
import com.example.derivatives.portfolio.PortfolioRepository;
import com.example.derivatives.shared.ApiException;
import com.example.derivatives.trade.PricingService;
import com.example.derivatives.trade.Trade;
import com.example.derivatives.trade.TradeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RiskService {
    private final PortfolioRepository portfolioRepository;
    private final TradeRepository tradeRepository;
    private final MarketSnapshotRepository marketRepository;
    private final PricingService pricingService;

    public RiskService(
            PortfolioRepository portfolioRepository,
            TradeRepository tradeRepository,
            MarketSnapshotRepository marketRepository,
            PricingService pricingService
    ) {
        this.portfolioRepository = portfolioRepository;
        this.tradeRepository = tradeRepository;
        this.marketRepository = marketRepository;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public RiskReport report(String portfolioCode) {
        if (!portfolioRepository.existsById(portfolioCode)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND",
                    "Portfolio " + portfolioCode + " does not exist");
        }
        var trades = tradeRepository.findByPortfolioCodeAndStatus(
                portfolioCode, Trade.TradeStatus.APPROVED);
        var base = aggregate(trades, BigDecimal.ZERO);
        var scenarios = List.of(
                scenario("SPOT_DOWN_10", new BigDecimal("-0.10"), trades),
                scenario("SPOT_UP_10", new BigDecimal("0.10"), trades)
        );
        return new RiskReport(
                portfolioCode,
                trades.size(),
                base.presentValue(),
                base.delta(),
                "USD",
                scenarios
        );
    }

    private RiskScenario scenario(
            String key,
            BigDecimal spotShockPct,
            List<Trade> trades
    ) {
        var aggregate = aggregate(trades, spotShockPct);
        return new RiskScenario(
                key,
                spotShockPct,
                aggregate.presentValue(),
                aggregate.presentValue()
                        .subtract(aggregate(trades, BigDecimal.ZERO).presentValue())
                        .setScale(6, RoundingMode.HALF_UP)
        );
    }

    private Aggregate aggregate(List<Trade> trades, BigDecimal spotShockPct) {
        BigDecimal presentValue = BigDecimal.ZERO;
        BigDecimal delta = BigDecimal.ZERO;
        for (var trade : trades) {
            var market = marketRepository.findById(trade.getUnderlying())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "MARKET_DATA_NOT_FOUND",
                            "No market snapshot exists for " + trade.getUnderlying()));
            var shockedSpot = market.getSpot().multiply(BigDecimal.ONE.add(spotShockPct));
            var value = pricingService.price(trade, market, shockedSpot);
            BigDecimal tradePv = value.presentValue();
            BigDecimal tradeDelta = value.delta();

            // Intentional deterministic product bug for test-harness evaluation:
            // SHORT European options are aggregated without their direction sign.
            if (trade.getTradeType() == Trade.TradeType.EUROPEAN_OPTION
                    && trade.getSide() == Trade.Side.SHORT) {
                tradePv = tradePv.negate();
                tradeDelta = tradeDelta.negate();
            }

            presentValue = presentValue.add(tradePv);
            delta = delta.add(tradeDelta);
        }
        return new Aggregate(
                presentValue.setScale(6, RoundingMode.HALF_UP),
                delta.setScale(6, RoundingMode.HALF_UP)
        );
    }

    public record RiskReport(
            String portfolioCode,
            int approvedTradeCount,
            BigDecimal presentValue,
            BigDecimal delta,
            String currency,
            List<RiskScenario> scenarios
    ) {
    }

    public record RiskScenario(
            String key,
            BigDecimal spotShockPct,
            BigDecimal stressedPresentValue,
            BigDecimal pnl
    ) {
    }

    private record Aggregate(BigDecimal presentValue, BigDecimal delta) {
    }
}
