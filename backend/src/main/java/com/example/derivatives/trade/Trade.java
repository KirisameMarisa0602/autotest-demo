package com.example.derivatives.trade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trades")
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String requestFingerprint;

    @Column(nullable = false, length = 40)
    private String portfolioCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType tradeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Column(nullable = false, length = 40)
    private String underlying;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal strike;

    @Column(nullable = false)
    private LocalDate maturity;

    @Enumerated(EnumType.STRING)
    private OptionType optionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private String createdBy;

    private String decisionBy;
    private String decisionComment;

    @Version
    private long version;

    protected Trade() {
    }

    public Trade(
            String idempotencyKey,
            String requestFingerprint,
            String portfolioCode,
            TradeType tradeType,
            Side side,
            String underlying,
            BigDecimal quantity,
            BigDecimal strike,
            LocalDate maturity,
            OptionType optionType,
            Instant now,
            String createdBy
    ) {
        this.idempotencyKey = idempotencyKey;
        this.requestFingerprint = requestFingerprint;
        this.portfolioCode = portfolioCode;
        this.tradeType = tradeType;
        this.side = side;
        this.underlying = underlying;
        this.quantity = quantity;
        this.strike = strike;
        this.maturity = maturity;
        this.optionType = optionType;
        this.status = TradeStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = createdBy;
    }

    public void submit(Instant now) {
        status = TradeStatus.SUBMITTED;
        updatedAt = now;
    }

    public void approve(Instant now, String actor, String comment) {
        status = TradeStatus.APPROVED;
        updatedAt = now;
        decisionBy = actor;
        decisionComment = comment;
    }

    public void reject(Instant now, String actor, String comment) {
        status = TradeStatus.REJECTED;
        updatedAt = now;
        decisionBy = actor;
        decisionComment = comment;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public String getPortfolioCode() {
        return portfolioCode;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public Side getSide() {
        return side;
    }

    public String getUnderlying() {
        return underlying;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getStrike() {
        return strike;
    }

    public LocalDate getMaturity() {
        return maturity;
    }

    public OptionType getOptionType() {
        return optionType;
    }

    public TradeStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getDecisionBy() {
        return decisionBy;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public long getVersion() {
        return version;
    }

    public enum TradeType {
        EUROPEAN_OPTION,
        FORWARD
    }

    public enum Side {
        LONG(1),
        SHORT(-1);

        private final int sign;

        Side(int sign) {
            this.sign = sign;
        }

        public int sign() {
            return sign;
        }
    }

    public enum OptionType {
        CALL,
        PUT
    }

    public enum TradeStatus {
        DRAFT,
        SUBMITTED,
        APPROVED,
        REJECTED
    }
}
