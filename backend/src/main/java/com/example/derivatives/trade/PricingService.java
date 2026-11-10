package com.example.derivatives.trade;

import com.example.derivatives.market.MarketSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class PricingService {
    private static final int OUTPUT_SCALE = 6;

    private final Clock clock;

    /**
     * Day-count uses the injected business {@link Clock}: the demo profile fixes
     * it to 2026-01-15T10:00:00Z so pricing is reproducible. The same-day floor
     * below is demo behavior only and is not an approved oracle (spec gap).
     */
    public PricingService(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
        this.clock = clock;
    }

    public PricingResult price(Trade trade, MarketSnapshot market) {
        return price(trade, market, market.getSpot());
    }

    public PricingResult price(Trade trade, MarketSnapshot market, BigDecimal spotOverride) {
        double spot = spotOverride.doubleValue();
        double strike = trade.getStrike().doubleValue();
        double rate = market.getRiskFreeRate().doubleValue();
        // SPEC GAP: same-day maturity day-count convention is not approved.
        // The suite must not assert same-day PV/Delta; only future maturities are testable.
        LocalDate businessDate = LocalDate.now(clock);
        long days = ChronoUnit.DAYS.between(businessDate, trade.getMaturity());
        // Same-day expiry floor of 1 day is current demo behavior only.
        double years = Math.max(days, 1L) / 365.0;

        double unitPv;
        double unitDelta;
        if (trade.getTradeType() == Trade.TradeType.FORWARD) {
            unitPv = spot - strike * Math.exp(-rate * years);
            unitDelta = 1.0;
        } else {
            double volatility = market.getVolatility().doubleValue();
            double rootTime = Math.sqrt(years);
            double d1 = (Math.log(spot / strike)
                    + (rate + 0.5 * volatility * volatility) * years)
                    / (volatility * rootTime);
            double d2 = d1 - volatility * rootTime;
            if (trade.getOptionType() == Trade.OptionType.CALL) {
                unitPv = spot * normalCdf(d1)
                        - strike * Math.exp(-rate * years) * normalCdf(d2);
                unitDelta = normalCdf(d1);
            } else {
                unitPv = strike * Math.exp(-rate * years) * normalCdf(-d2)
                        - spot * normalCdf(-d1);
                unitDelta = 1.0 - normalCdf(d1);
            }
        }

        double scale = trade.getQuantity().doubleValue() * trade.getSide().sign();
        return new PricingResult(decimal(unitPv * scale), decimal(unitDelta * scale));
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(OUTPUT_SCALE, RoundingMode.HALF_UP);
    }

    private static double normalCdf(double value) {
        if (value < 0) {
            return 1.0 - normalCdf(-value);
        }
        double t = 1.0 / (1.0 + 0.2316419 * value);
        double density = Math.exp(-0.5 * value * value) / Math.sqrt(2.0 * Math.PI);
        double polynomial = t * (0.319381530
                + t * (-0.356563782
                + t * (1.781477937
                + t * (-1.821255978 + t * 1.330274429))));
        return 1.0 - density * polynomial;
    }

    public record PricingResult(BigDecimal presentValue, BigDecimal delta) {
    }
}
