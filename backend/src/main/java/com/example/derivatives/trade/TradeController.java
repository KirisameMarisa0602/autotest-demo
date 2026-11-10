package com.example.derivatives.trade;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static com.example.derivatives.trade.TradeApiModels.CreateTradeRequest;
import static com.example.derivatives.trade.TradeApiModels.DecisionRequest;
import static com.example.derivatives.trade.TradeApiModels.TradeResponse;
import static com.example.derivatives.trade.TradeApiModels.TransitionRequest;
import static com.example.derivatives.trade.TradeApiModels.ValuationResponse;

@Validated
@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('trader')")
    ResponseEntity<TradeResponse> create(
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @Valid @RequestBody CreateTradeRequest request
    ) {
        var result = tradeService.create(idempotencyKey, request);
        if (result.replayed()) {
            return ResponseEntity.ok(result.trade());
        }
        return ResponseEntity.created(URI.create("/api/trades/" + result.trade().id()))
                .body(result.trade());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('trader', 'risk_manager', 'viewer')")
    List<TradeResponse> findAll(
            @RequestParam(required = false) String portfolioCode
    ) {
        return tradeService.findAll(portfolioCode);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('trader', 'risk_manager', 'viewer')")
    TradeResponse find(@PathVariable UUID id) {
        return tradeService.find(id);
    }

    @GetMapping("/{id}/valuation")
    @PreAuthorize("hasAnyRole('trader', 'risk_manager', 'viewer')")
    ValuationResponse value(@PathVariable UUID id) {
        return tradeService.value(id);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('trader')")
    TradeResponse submit(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionRequest request
    ) {
        return tradeService.submit(id, request.expectedVersion());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('risk_manager')")
    TradeResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request
    ) {
        return tradeService.approve(id, request.expectedVersion(), request.comment());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('risk_manager')")
    TradeResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request
    ) {
        return tradeService.reject(id, request.expectedVersion(), request.comment());
    }
}
