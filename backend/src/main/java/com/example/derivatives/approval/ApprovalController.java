package com.example.derivatives.approval;

import com.example.derivatives.trade.Trade;
import com.example.derivatives.trade.TradeApiModels.DecisionRequest;
import com.example.derivatives.trade.TradeApiModels.TradeResponse;
import com.example.derivatives.trade.TradeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
    private final TradeService tradeService;

    public ApprovalController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('risk_manager', 'viewer')")
    List<TradeResponse> pending() {
        return tradeService.findAll(null, Trade.TradeStatus.SUBMITTED, 0, 200).items();
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
