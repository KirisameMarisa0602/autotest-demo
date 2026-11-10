package com.example.derivatives;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class TradeWorkflowIntegrationTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(withIdentity(post("/api/demo/reset"), "alice", "trader"))
                .andExpect(status().isOk());
    }

    @Test
    void followsHappyStateMachineAndProducesSensibleOptionValue() throws Exception {
        JsonNode created = body(createTrade("workflow-1", optionBody("LONG"), "alice", "trader")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn());
        String id = created.get("id").asText();

        mockMvc.perform(withIdentity(get("/api/trades/{id}/valuation", id), "alice", "trader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentValue").isNumber())
                .andExpect(jsonPath("$.delta").isNumber())
                .andExpect(result -> {
                    JsonNode valuation = body(result);
                    assertThat(valuation.get("presentValue").decimalValue()).isPositive();
                    assertThat(valuation.get("delta").decimalValue()).isPositive();
                });

        mockMvc.perform(withIdentity(post("/api/trades/{id}/submit", id), "alice", "trader")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(withIdentity(post("/api/trades/{id}/approve", id), "bob", "risk_manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"comment\":\"checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.decisionBy").value("bob/risk_manager"));
    }

    @Test
    void rejectsInvalidTransition() throws Exception {
        String id = body(createTrade("state-1", optionBody("LONG"), "alice", "trader")
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();

        mockMvc.perform(withIdentity(post("/api/trades/{id}/approve", id), "bob", "risk_manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"comment\":\"too early\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void shortOptionValuationIsNegativeWhileRiskAggregationIsBuggy() throws Exception {
        String id = body(createTrade("short-1", optionBody("SHORT"), "alice", "trader")
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();

        JsonNode valuation = body(mockMvc.perform(
                        withIdentity(get("/api/trades/{id}/valuation", id), "alice", "trader"))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(valuation.get("presentValue").decimalValue()).isNegative();
        assertThat(valuation.get("delta").decimalValue()).isNegative();

        mockMvc.perform(withIdentity(post("/api/trades/{id}/submit", id), "alice", "trader")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(withIdentity(post("/api/trades/{id}/approve", id), "bob", "risk_manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"comment\":\"known defect probe\"}"))
                .andExpect(status().isOk());

        JsonNode risk = body(mockMvc.perform(
                        withIdentity(get("/api/risk/portfolios/ALPHA"), "bob", "risk_manager"))
                .andExpect(status().isOk())
                .andReturn());
        // Intentional product bug: SHORT option contributions are flipped back to LONG-looking signs.
        assertThat(risk.get("presentValue").decimalValue())
                .isEqualByComparingTo(valuation.get("presentValue").decimalValue().negate());
        assertThat(risk.get("delta").decimalValue())
                .isEqualByComparingTo(valuation.get("delta").decimalValue().negate());
    }

    private ResultActions createTrade(
            String idempotencyKey,
            String body,
            String user,
            String role
    ) throws Exception {
        return mockMvc.perform(withIdentity(post("/api/trades"), user, role)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static MockHttpServletRequestBuilder withIdentity(
            MockHttpServletRequestBuilder builder,
            String user,
            String role
    ) {
        return builder
                .header("X-Demo-User", user)
                .header("X-Demo-Role", role)
                .header("X-Run-Id", "integration")
                .header("X-Case-Key", "integration");
    }

    private static String optionBody(String side) {
        return """
                {
                  "portfolioCode":"ALPHA",
                  "tradeType":"EUROPEAN_OPTION",
                  "side":"%s",
                  "underlying":"AAPL",
                  "quantity":10,
                  "strike":100,
                  "maturity":"2027-12-31",
                  "optionType":"CALL"
                }
                """.formatted(side);
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
