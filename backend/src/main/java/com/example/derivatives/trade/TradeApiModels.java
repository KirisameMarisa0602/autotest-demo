package com.example.derivatives.trade;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class TradeApiModels {
    private TradeApiModels() {
    }

    public record CreateTradeRequest(
            @NotBlank @Size(max = 40) String portfolioCode,
            @NotNull Trade.TradeType tradeType,
            @NotNull Trade.Side side,
            @NotBlank @Size(max = 40)
            @Pattern(regexp = "[A-Z0-9.-]+", message = "must use uppercase market symbol characters")
            String underlying,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 12, fraction = 6)
            BigDecimal quantity,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 12, fraction = 6)
            BigDecimal strike,
            @NotNull LocalDate maturity,
            Trade.OptionType optionType
    ) {
    }

    public record TransitionRequest(
            @NotNull @PositiveOrZero Long expectedVersion
    ) {
    }

    public record DecisionRequest(
            @NotNull @PositiveOrZero Long expectedVersion,
            @Size(max = 500) String comment
    ) {
    }

    public record TradeResponse(
            UUID id,
            String portfolioCode,
            Trade.TradeType tradeType,
            Trade.Side side,
            String underlying,
            BigDecimal quantity,
            BigDecimal strike,
            LocalDate maturity,
            Trade.OptionType optionType,
            Trade.TradeStatus status,
            long version,
            Instant createdAt,
            Instant updatedAt,
            String createdBy,
            String decisionBy,
            String decisionComment
    ) {
    }

    public record ValuationResponse(
            UUID tradeId,
            BigDecimal presentValue,
            BigDecimal delta,
            String currency,
            Instant marketDataAsOf
    ) {
    }

    /**
     * Page envelope for the trade list. The list contract is intentionally
     * newer than docs/openapi.yaml so external analyzers have to read the
     * controller rather than trusting the published contract.
     */
    public record PagedTradesResponse(
            List<TradeResponse> items,
            int page,
            int size,
            int totalItems,
            int totalPages
    ) {
    }

    /**
     * One audit entry scoped to a single trade aggregate.
     */
    public record TradeHistoryEvent(
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
