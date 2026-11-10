package com.example.derivatives.trade;

import com.example.derivatives.market.MarketSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
public class PricingService {
    private static final int OUTPUT_SCALE = 6;

    /**
     * Clock is retained for Spring wiring parity with other demo services.
     * Day-count intentionally uses {@link ZoneId#systemDefault()} (spec gap).
     */
    public PricingService(Clock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("clock is required");
        }
    }

    public PricingResult price(Trade trade, MarketSnapshot market) {
        return price(trade, market, market.getSpot());
    }

    public PricingResult price(Trade trade, MarketSnapshot market, BigDecimal spotOverride) {
        double spot = spotOverride.doubleValue();
        double strike = trade.getStrike().doubleValue();
        double rate = market.getRiskFreeRate().doubleValue();
        // SPEC GAP: "today" for same-day expiry uses the JVM local zone, not the demo Clock zone.
        // Requirements intentionally leave UTC vs local unresolved; do not treat this as oracle.
        LocalDate todayLocal = LocalDate.now(ZoneId.systemDefault());
        long days = ChronoUnit.DAYS.between(todayLocal, trade.getMaturity());
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
                unitDelta = normalCdf(d1) - 1.0;
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
