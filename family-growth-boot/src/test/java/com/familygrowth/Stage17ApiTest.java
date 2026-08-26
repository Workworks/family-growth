package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage17api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage17ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void withdrawalIsQuotedReservedProtectedPaidAndIdempotent() throws Exception {
        Session parent = bootstrap("零钱家庭", "357924");
        Session other = bootstrap("其他家庭", "357925");
        UUID child = child(parent, "小禾");
        String childToken = childToken(parent, child);
        adjust(parent, child, "100.00", "stage17-credit");

        String rules = "/api/v1/families/" + parent.family + "/withdrawal-rules";
        mvc.perform(get(rules + "/active").header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.payoutRate").value(1.0))
            .andExpect(jsonPath("$.data.feeAmount").doesNotExist());
        mvc.perform(post(rules).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "rule-denied").contentType(MediaType.APPLICATION_JSON)
            .content(ruleBody())).andExpect(status().isForbidden());
        mvc.perform(post(rules).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "rule-1").contentType(MediaType.APPLICATION_JSON)
            .content(ruleBody())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.version").value(2));
        mvc.perform(post(rules).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "rule-1").contentType(MediaType.APPLICATION_JSON)
            .content(ruleBody())).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.version").value(2));
        mvc.perform(post(rules).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "rule-1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"payoutRate\":2,\"feeRate\":0,\"fixedFee\":0}"))
            .andExpect(status().isConflict());

        UUID quote = quote(parent, child, childToken, "40.00", "quote-1", "38.00");
        mvc.perform(post(quoteUrl(parent, child)).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "quote-1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"moneyAmount\":41.00}"))
            .andExpect(status().isConflict());
        UUID request = request(parent, child, childToken, quote, "request-1");
        String actions = actionUrl(parent, request);
        mvc.perform(post(actions + "/approve").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "approve-denied")).andExpect(status().isForbidden());
        mvc.perform(post(actions + "/approve").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "approve-1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mvc.perform(post(actions + "/approve").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "approve-1")).andExpect(status().isOk());
        assertWallet(parent, child, "100.00", "40.00", "60.00");

        adjustStatus(parent, child, "-61.00", "reserved-adjust-denied", 409);
        verifyOtherMoneySpendingCannotUseReserved(parent, child, childToken);
        adjustStatus(parent, child, "-60.00", "available-adjust", 200);
        assertWallet(parent, child, "40.00", "40.00", "0.00");

        mvc.perform(post(actions + "/paid").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "paid-denied")).andExpect(status().isForbidden());
        mvc.perform(post(actions + "/paid").header("Authorization", bearer(other.token))
            .header("Idempotency-Key", "paid-other-family")).andExpect(status().isNotFound());
        mvc.perform(post(actions + "/paid").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "paid-1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PAID"))
            .andExpect(jsonPath("$.data.feeAmount").value(2.00))
            .andExpect(jsonPath("$.data.netPayout").value(38.00));
        mvc.perform(post(actions + "/paid").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "paid-1")).andExpect(status().isOk());
        mvc.perform(post(actions + "/paid").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "paid-second-key")).andExpect(status().isConflict());
        assertWallet(parent, child, "0.00", "0.00", "0.00");

        assertThat(jdbc.queryForObject("""
            SELECT COUNT(*) FROM ledger_entry
            WHERE family_id=? AND business_type='WITHDRAWAL' AND business_id=?
            """, Integer.class, parent.family, request)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT fee_amount FROM withdrawal_request WHERE id=?",
            java.math.BigDecimal.class, request)).isEqualByComparingTo("2.00");
    }

    @Test
    void rejectAndCancelReleaseWithoutLedgerAndRespectChildScope() throws Exception {
        Session parent = bootstrap("状态家庭", "468135");
        UUID child = child(parent, "小芽");
        String childToken = childToken(parent, child);
        adjust(parent, child, "50.00", "state-credit");

        UUID reject = request(parent, child, childToken,
            quote(parent, child, childToken, "10.00", "quote-reject", "10.00"), "request-reject");
        mvc.perform(post(actionUrl(parent, reject) + "/reject")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "reject-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));

        UUID pendingCancel = request(parent, child, childToken,
            quote(parent, child, childToken, "10.00", "quote-pending-cancel", "10.00"),
            "request-pending-cancel");
        mvc.perform(post(actionUrl(parent, pendingCancel) + "/cancel")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "cancel-pending"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));

        UUID approvedCancel = request(parent, child, childToken,
            quote(parent, child, childToken, "20.00", "quote-approved-cancel", "20.00"),
            "request-approved-cancel");
        mvc.perform(post(actionUrl(parent, approvedCancel) + "/approve")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "approve-cancel"))
            .andExpect(status().isOk());
        assertWallet(parent, child, "50.00", "20.00", "30.00");
        mvc.perform(post(actionUrl(parent, approvedCancel) + "/cancel")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "child-cancel-approved"))
            .andExpect(status().isForbidden());
        mvc.perform(post(actionUrl(parent, approvedCancel) + "/cancel")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "parent-cancel-approved"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertWallet(parent, child, "50.00", "0.00", "50.00");

        mvc.perform(get("/api/v1/families/" + parent.family + "/children/" + child
                + "/withdrawal-requests").header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry WHERE family_id=? AND business_type='WITHDRAWAL'",
            Integer.class, parent.family)).isZero();
    }

    private void verifyOtherMoneySpendingCannotUseReserved(Session parent, UUID child,
                                                            String childToken) throws Exception {
        String exchangeRules = "/api/v1/families/" + parent.family + "/exchange-rules";
        mvc.perform(post(exchangeRules).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"moneyToCoinRate\":1,\"coinToMoneyRate\":1,\"moneyToCoinFeeRate\":0,\"coinToMoneyFeeRate\":0,\"maxSourceAmount\":1000}"))
            .andExpect(status().isCreated());
        UUID exchange = id(mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child + "/exchange-previews")
            .header("Authorization", bearer(childToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"direction\":\"MONEY_TO_COIN\",\"sourceAmount\":70.00}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/families/" + parent.family + "/exchange-previews/" + exchange + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "reserved-exchange"))
            .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child + "/saving/transfers")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "reserved-saving")
            .contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"DEPOSIT\",\"amount\":70.00}"))
            .andExpect(status().isConflict());

        UUID fund = id(mvc.perform(post("/api/v1/families/" + parent.family + "/funds")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"成长模拟\",\"riskLabel\":\"会涨会跌\"}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/families/" + parent.family + "/funds/" + fund + "/nav")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"navDate\":\"2026-08-26\",\"nav\":1.000000}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/families/" + parent.family + "/funds/" + fund + "/fee-rules")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"buyFeeRate\":0,\"sellFeeRate\":0}"))
            .andExpect(status().isCreated());
        UUID fundPreview = id(mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child
                + "/funds/" + fund + "/trade-previews")
            .header("Authorization", bearer(childToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"side\":\"BUY\",\"inputAmount\":70.00}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/families/" + parent.family + "/fund-trade-previews/" + fundPreview + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "reserved-fund"))
            .andExpect(status().isConflict());
    }

    private UUID quote(Session parent, UUID child, String token, String amount, String key,
                       String expectedNet) throws Exception {
        return id(mvc.perform(post(quoteUrl(parent, child)).header("Authorization", bearer(token))
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
            .content("{\"moneyAmount\":" + amount + "}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.netPayout").value(Double.parseDouble(expectedNet)))
            .andExpect(jsonPath("$.data.expiresAt").isNotEmpty()).andReturn());
    }

    private UUID request(Session parent, UUID child, String token, UUID quote, String key) throws Exception {
        return id(mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child
                + "/withdrawal-requests").header("Authorization", bearer(token))
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
            .content("{\"quoteId\":\"" + quote + "\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("REQUESTED"))
            .andReturn());
    }

    private void assertWallet(Session parent, UUID child, String money, String reserved,
                              String available) throws Exception {
        mvc.perform(get("/api/v1/families/" + parent.family + "/children/" + child + "/wallet")
            .header("Authorization", bearer(parent.token))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moneyBalance").value(Double.parseDouble(money)))
            .andExpect(jsonPath("$.data.reservedMoney").value(Double.parseDouble(reserved)))
            .andExpect(jsonPath("$.data.availableMoney").value(Double.parseDouble(available)));
    }

    private UUID child(Session parent, String name) throws Exception {
        return id(mvc.perform(post("/api/v1/families/" + parent.family + "/children")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"" + name + "\",\"birthDate\":\"2017-01-01\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isCreated()).andReturn());
    }

    private String childToken(Session parent, UUID child) throws Exception {
        return text(mvc.perform(post("/api/v1/auth/child-sessions")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn(), "data", "token");
    }

    private Session bootstrap(String name, String pin) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        return new Session(UUID.fromString(text(result, "data", "familyId")),
            text(result, "data", "session", "token"));
    }

    private void adjust(Session parent, UUID child, String delta, String key) throws Exception {
        adjustStatus(parent, child, delta, key, 200);
    }

    private void adjustStatus(Session parent, UUID child, String delta, String key, int statusCode)
        throws Exception {
        mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child + "/wallet/adjustments")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"assetType\":\"MONEY\",\"delta\":" + delta + ",\"reason\":\"Stage17 fixture\"}"))
            .andExpect(status().is(statusCode));
    }

    private String quoteUrl(Session parent, UUID child) {
        return "/api/v1/families/" + parent.family + "/children/" + child + "/withdrawal-quotes";
    }

    private String actionUrl(Session parent, UUID request) {
        return "/api/v1/families/" + parent.family + "/withdrawal-requests/" + request;
    }

    private static String ruleBody() {
        return "{\"payoutRate\":1,\"feeRate\":0.025,\"fixedFee\":1.00}";
    }

    private UUID id(MvcResult result) throws Exception {
        return UUID.fromString(text(result, "data", "id"));
    }

    private String text(MvcResult result, String... path) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        for (String part : path) node = node.path(part);
        return node.asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(UUID family, String token) {
    }
}
