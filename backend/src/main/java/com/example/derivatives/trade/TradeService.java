package com.example.derivatives.trade;

import com.example.derivatives.audit.AuditService;
import com.example.derivatives.market.MarketSnapshotRepository;
import com.example.derivatives.portfolio.PortfolioRepository;
import com.example.derivatives.security.ActorProvider;
import com.example.derivatives.shared.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static com.example.derivatives.trade.TradeApiModels.CreateTradeRequest;
import static com.example.derivatives.trade.TradeApiModels.PagedTradesResponse;
import static com.example.derivatives.trade.TradeApiModels.TradeHistoryEvent;
import static com.example.derivatives.trade.TradeApiModels.TradeResponse;
import static com.example.derivatives.trade.TradeApiModels.ValuationResponse;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;
    private final PortfolioRepository portfolioRepository;
    private final MarketSnapshotRepository marketRepository;
    private final PricingService pricingService;
    private final AuditService auditService;
    private final ActorProvider actorProvider;
    private final Clock clock;

    public TradeService(
            TradeRepository tradeRepository,
            PortfolioRepository portfolioRepository,
            MarketSnapshotRepository marketRepository,
            PricingService pricingService,
            AuditService auditService,
            ActorProvider actorProvider,
            Clock clock
    ) {
        this.tradeRepository = tradeRepository;
        this.portfolioRepository = portfolioRepository;
        this.marketRepository = marketRepository;
        this.pricingService = pricingService;
        this.auditService = auditService;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    @Transactional
    public CreateResult create(String idempotencyKey, CreateTradeRequest request) {
        String fingerprint = fingerprint(request);
        var existing = tradeRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestFingerprint().equals(fingerprint)) {
                throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT",
                        "The Idempotency-Key was already used with a different request");
            }
            return new CreateResult(toResponse(existing.get()), true);
        }

        validateReferencesAndTerms(request);
        var now = Instant.now(clock);
        var trade = new Trade(
                idempotencyKey,
                fingerprint,
                request.portfolioCode(),
                request.tradeType(),
                request.side(),
                request.underlying(),
                request.quantity(),
                request.strike(),
                request.maturity(),
                request.optionType(),
                now,
                actorProvider.currentActor()
        );
        tradeRepository.saveAndFlush(trade);
        auditService.record("TRADE_CREATED", "TRADE", trade.getId().toString(),
                "status=DRAFT;portfolio=" + trade.getPortfolioCode());
        return new CreateResult(toResponse(trade), false);
    }

    @Transactional(readOnly = true)
    public PagedTradesResponse findAll(
            String portfolioCode,
            Trade.TradeStatus status,
            int page,
            int size
    ) {
        String portfolio = (portfolioCode == null || portfolioCode.isBlank()) ? null : portfolioCode;
        if (portfolio != null) {
            requirePortfolio(portfolio);
        }
        List<Trade> matching = tradeRepository.findAll().stream()
                .filter(trade -> portfolio == null || trade.getPortfolioCode().equals(portfolio))
                .filter(trade -> status == null || trade.getStatus() == status)
                .sorted(Comparator.comparing(Trade::getCreatedAt).reversed()
                        .thenComparing(trade -> trade.getId().toString()))
                .toList();
        int totalItems = matching.size();
        int totalPages = totalItems == 0 ? 0 : (totalItems + size - 1) / size;
        int from = (int) Math.min((long) page * size, totalItems);
        int to = Math.min(from + size, totalItems);
        List<TradeResponse> items = matching.subList(from, to).stream()
                .map(this::toResponse)
                .toList();
        return new PagedTradesResponse(items, page, size, totalItems, totalPages);
    }

    @Transactional(readOnly = true)
    public TradeResponse find(UUID id) {
        return toResponse(requireTrade(id));
    }

    @Transactional
    public TradeResponse submit(UUID id, long expectedVersion) {
        var trade = requireTrade(id);
        assertVersion(trade, expectedVersion);
        assertStatus(trade, Trade.TradeStatus.DRAFT, "submit");
        trade.submit(Instant.now(clock));
        tradeRepository.saveAndFlush(trade);
        auditService.record("TRADE_SUBMITTED", "TRADE", id.toString(),
                "status=SUBMITTED");
        return toResponse(trade);
    }

    @Transactional
    public TradeResponse approve(UUID id, long expectedVersion, String comment) {
        var trade = requireTrade(id);
        assertVersion(trade, expectedVersion);
        assertStatus(trade, Trade.TradeStatus.SUBMITTED, "approve");
        trade.approve(Instant.now(clock), actorProvider.currentActor(), normalized(comment));
        tradeRepository.saveAndFlush(trade);
        auditService.record("TRADE_APPROVED", "TRADE", id.toString(),
                "status=APPROVED");
        return toResponse(trade);
    }

    @Transactional
    public TradeResponse reject(UUID id, long expectedVersion, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "A non-blank comment is required when rejecting a trade");
        }
        var trade = requireTrade(id);
        assertVersion(trade, expectedVersion);
        assertStatus(trade, Trade.TradeStatus.SUBMITTED, "reject");
        trade.reject(Instant.now(clock), actorProvider.currentActor(), comment.trim());
        tradeRepository.saveAndFlush(trade);
        auditService.record("TRADE_REJECTED", "TRADE", id.toString(),
                "status=REJECTED");
        return toResponse(trade);
    }

    @Transactional(readOnly = true)
    public List<TradeHistoryEvent> history(UUID id) {
        requireTrade(id);
        return auditService.history("TRADE", id.toString()).stream()
                .map(event -> new TradeHistoryEvent(
                        event.getId(),
                        event.getEventType(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        event.getActor(),
                        event.getOccurredAt(),
                        event.getDetails()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ValuationResponse value(UUID id) {
        var trade = requireTrade(id);
        var market = marketRepository.findById(trade.getUnderlying())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "MARKET_DATA_NOT_FOUND",
                        "No market snapshot exists for " + trade.getUnderlying()));
        var value = pricingService.price(trade, market);
        return new ValuationResponse(
                trade.getId(),
                value.presentValue(),
                value.delta(),
                "USD",
                market.getAsOf()
        );
    }

    private void validateReferencesAndTerms(CreateTradeRequest request) {
        requirePortfolio(request.portfolioCode());
        if (!marketRepository.existsById(request.underlying())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MARKET_DATA_NOT_FOUND",
                    "No market snapshot exists for " + request.underlying());
        }
        // SPEC GAP: maturity vs "today" is compared in the JVM local timezone.
        // Docs intentionally leave UTC vs local unresolved; demo Clock zone is not used here.
        LocalDate todayLocal = LocalDate.now(ZoneId.systemDefault());
        if (request.maturity().isBefore(todayLocal)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "maturity must not be before today (local timezone comparison)");
        }
        if (request.tradeType() == Trade.TradeType.EUROPEAN_OPTION
                && request.optionType() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "optionType is required for EUROPEAN_OPTION");
        }
        if (request.tradeType() == Trade.TradeType.FORWARD
                && request.optionType() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                    "optionType must be omitted for FORWARD");
        }
    }

    private void requirePortfolio(String portfolioCode) {
        if (!portfolioRepository.existsById(portfolioCode)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PORTFOLIO_NOT_FOUND",
                    "Portfolio " + portfolioCode + " does not exist");
        }
    }

    private Trade requireTrade(UUID id) {
        return tradeRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "TRADE_NOT_FOUND", "Trade " + id + " does not exist"));
    }

    private static void assertVersion(Trade trade, long expectedVersion) {
        if (trade.getVersion() != expectedVersion) {
            throw new ApiException(HttpStatus.CONFLICT, "VERSION_CONFLICT",
                    "Expected version " + expectedVersion + " but found " + trade.getVersion());
        }
    }

    private static void assertStatus(
            Trade trade,
            Trade.TradeStatus requiredStatus,
            String action
    ) {
        if (trade.getStatus() != requiredStatus) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Cannot " + action + " a trade in " + trade.getStatus() + " status");
        }
    }

    private TradeResponse toResponse(Trade trade) {
        return new TradeResponse(
                trade.getId(),
                trade.getPortfolioCode(),
                trade.getTradeType(),
                trade.getSide(),
                trade.getUnderlying(),
                trade.getQuantity(),
                trade.getStrike(),
                trade.getMaturity(),
                trade.getOptionType(),
                trade.getStatus(),
                trade.getVersion(),
                trade.getCreatedAt(),
                trade.getUpdatedAt(),
                trade.getCreatedBy(),
                trade.getDecisionBy(),
                trade.getDecisionComment()
        );
    }

    private static String fingerprint(CreateTradeRequest request) {
        String canonical = String.join("|",
                request.portfolioCode(),
                request.tradeType().name(),
                request.side().name(),
                request.underlying(),
                canonical(request.quantity()),
                canonical(request.strike()),
                request.maturity().toString(),
                request.optionType() == null ? "" : request.optionType().name());
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the JVM", exception);
        }
    }

    private static String canonical(BigDecimal value) {
        return value.toPlainString();
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateResult(TradeResponse trade, boolean replayed) {
    }
}
