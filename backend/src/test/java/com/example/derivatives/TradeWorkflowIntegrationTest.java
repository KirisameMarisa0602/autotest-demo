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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class TradeWorkflowIntegrationTest {
    private static final String TRADER = "Bearer trader-token";
    private static final String APPROVER = "Bearer approver-token";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void reset() throws Exception {
        mockMvc.perform(post("/api/demo/reset").header("Authorization", TRADER))
                .andExpect(status().isOk());
    }

    @Test
    void followsHappyStateMachineAndProducesSensibleOptionValue() throws Exception {
        JsonNode created = body(createTrade("workflow-1", optionBody("LONG"), TRADER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn());
        String id = created.get("id").asText();

        mockMvc.perform(get("/api/trades/{id}/valuation", id)
                        .header("Authorization", TRADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentValue").isNumber())
                .andExpect(jsonPath("$.delta").isNumber())
                .andExpect(result -> {
                    JsonNode valuation = body(result);
                    assertThat(valuation.get("presentValue").decimalValue()).isPositive();
                    assertThat(valuation.get("delta").decimalValue()).isPositive();
                });

        mockMvc.perform(post("/api/trades/{id}/submit", id)
                        .header("Authorization", TRADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/trades/{id}/approve", id)
                        .header("Authorization", APPROVER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"comment\":\"checked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void rejectsInvalidTransition() throws Exception {
        String id = body(createTrade("state-1", optionBody("LONG"), TRADER)
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();

        mockMvc.perform(post("/api/trades/{id}/approve", id)
                        .header("Authorization", APPROVER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
    }

    @Test
    void rejectsStaleVersionAndSupportsTheRejectionTerminalPath() throws Exception {
        String id = body(createTrade("state-reject-1", optionBody("LONG"), TRADER)
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();
        mockMvc.perform(post("/api/trades/{id}/submit", id)
                        .header("Authorization", TRADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trades/{id}/reject", id)
                        .header("Authorization", APPROVER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"comment\":\"limit breached\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));

        mockMvc.perform(post("/api/trades/{id}/reject", id)
                        .header("Authorization", APPROVER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"comment\":\"limit breached\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void enforcesRolePermissions() throws Exception {
        createTrade("permission-1", optionBody("LONG"), APPROVER)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String id = body(createTrade("permission-2", optionBody("LONG"), TRADER)
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();
        mockMvc.perform(post("/api/trades/{id}/approve", id)
                        .header("Authorization", TRADER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void replaysSameIdempotencyKeyAndRejectsChangedPayload() throws Exception {
        JsonNode first = body(createTrade("idem-1", optionBody("LONG"), TRADER)
                .andExpect(status().isCreated())
                .andReturn());
        JsonNode replay = body(createTrade("idem-1", optionBody("LONG"), TRADER)
                .andExpect(status().isOk())
                .andReturn());
        assertThat(replay.get("id").asText()).isEqualTo(first.get("id").asText());

        createTrade("idem-1", optionBody("SHORT"), TRADER)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void reportsBeanValidationErrors() throws Exception {
        String invalid = """
                {
                  "portfolioCode":"ALPHA",
                  "tradeType":"FORWARD",
                  "side":"LONG",
                  "underlying":"AAPL",
                  "quantity":0,
                  "strike":100,
                  "maturity":"2027-12-31"
                }
                """;
        createTrade("validation-1", invalid, TRADER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
    }

    @Test
    void pricesALongForwardWithQuantityScaledDelta() throws Exception {
        String forward = """
                {
                  "portfolioCode":"ALPHA",
                  "tradeType":"FORWARD",
                  "side":"LONG",
                  "underlying":"AAPL",
                  "quantity":5,
                  "strike":95,
                  "maturity":"2027-12-31"
                }
                """;
        String id = body(createTrade("pricing-forward-1", forward, TRADER)
                .andExpect(status().isCreated())
                .andReturn()).get("id").asText();

        mockMvc.perform(get("/api/trades/{id}/valuation", id)
                        .header("Authorization", TRADER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.presentValue").isNumber())
                .andExpect(jsonPath("$.delta").value(5.0));
    }

    private org.springframework.test.web.servlet.ResultActions createTrade(
            String key,
            String body,
            String bearer
    ) throws Exception {
        return mockMvc.perform(post("/api/trades")
                .header("Authorization", bearer)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
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
