package com.example.derivatives.market;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "market_snapshots")
public class MarketSnapshot {
    @Id
    private String underlying;
    private BigDecimal spot;
    private BigDecimal volatility;
    private BigDecimal riskFreeRate;
    private Instant asOf;

    protected MarketSnapshot() {
    }

    public MarketSnapshot(
            String underlying,
            BigDecimal spot,
            BigDecimal volatility,
            BigDecimal riskFreeRate,
            Instant asOf
    ) {
        this.underlying = underlying;
        this.spot = spot;
        this.volatility = volatility;
        this.riskFreeRate = riskFreeRate;
        this.asOf = asOf;
    }

    public String getUnderlying() {
        return underlying;
    }

    public BigDecimal getSpot() {
        return spot;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public BigDecimal getRiskFreeRate() {
        return riskFreeRate;
    }

    public Instant getAsOf() {
        return asOf;
    }
}
