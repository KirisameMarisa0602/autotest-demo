package com.example.derivatives.risk;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk/portfolios")
public class RiskController {
    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{portfolioCode}")
    @PreAuthorize("hasRole('risk_manager')")
    RiskService.RiskReport report(@PathVariable String portfolioCode) {
        return riskService.report(portfolioCode);
    }
}
