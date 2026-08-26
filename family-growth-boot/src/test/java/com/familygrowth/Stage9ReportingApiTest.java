package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
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
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage9report;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage9ReportingApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void crossDomainFactsProduceAuthorizedIdempotentReports() throws Exception {
        Session parent = bootstrap("Stage9家庭", "579135");
        Session other = bootstrap("其他Stage9家庭", "579136");
        UUID child = id(post("/api/v1/families/" + parent.family + "/children", parent.token,
            "{\"displayName\":\"报告孩子\",\"birthDate\":\"2017-01-01\",\"ageStage\":\"CHILD_6_9\"}", null, 201));
        String childToken = text(post("/api/v1/auth/child-sessions", parent.token,
            "{\"childId\":\"" + child + "\"}", null, 201), "data", "token");
        String childBase = "/api/v1/families/" + parent.family + "/children/" + child;

        mvc.perform(put(childBase + "/usage-policy").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"zoneId\":\"Asia/Shanghai\",\"dailyLimitMinutes\":20,\"sessionLimitMinutes\":10}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.dailyLimitMinutes").value(20));
        mvc.perform(put(childBase + "/usage-policy").header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"zoneId\":\"UTC\",\"dailyLimitMinutes\":60,\"sessionLimitMinutes\":30}"))
            .andExpect(status().isForbidden());

        Instant occurred = Instant.now().minusSeconds(5);
        String usageBody = "{\"type\":\"APP_ACTIVE\",\"minutes\":3,\"occurredAt\":\"" + occurred + "\"}";
        String eventId = text(post(childBase + "/usage-events", childToken, usageBody, "usage-stable-1", 201), "data", "id");
        assertThat(text(post(childBase + "/usage-events", childToken, usageBody, "usage-stable-1", 201), "data", "id")).isEqualTo(eventId);
        post(childBase + "/usage-events", childToken,
            "{\"type\":\"APP_ACTIVE\",\"minutes\":4,\"occurredAt\":\"" + occurred + "\"}", "usage-stable-1", 409);
        post(childBase + "/usage-events", childToken,
            "{\"type\":\"LEARNING\",\"minutes\":2,\"occurredAt\":\"" + occurred + "\"}", "usage-learning-1", 201);

        UUID plan = id(post(childBase + "/plans", parent.token,
            "{\"title\":\"成长计划\",\"startDate\":\"" + LocalDate.now() + "\"}", null, 201));
        UUID goal = id(post("/api/v1/families/" + parent.family + "/plans/" + plan + "/goals", parent.token,
            "{\"title\":\"阅读目标\"}", null, 201));
        UUID task = id(post("/api/v1/families/" + parent.family + "/goals/" + goal + "/tasks", parent.token,
            "{\"title\":\"读一页\",\"category\":\"READING\",\"difficulty\":\"EASY\",\"expectedMinutes\":5}", null, 201));
        UUID completion = id(post(childBase + "/tasks/" + task + "/completions", childToken,
            "{\"evidenceNote\":\"完成\"}", "report-submit", 201));
        post("/api/v1/families/" + parent.family + "/completions/" + completion + "/review", parent.token,
            "{\"approved\":true,\"xpReward\":10,\"coinReward\":100,\"moneyReward\":100.00,\"reviewNote\":\"确认\"}", "report-review", 200);

        post(childBase + "/gift-money", parent.token, "{\"amount\":20.00,\"note\":\"压岁钱\"}", "report-gift", 201);
        post("/api/v1/families/" + parent.family + "/exchange-rules", parent.token,
            "{\"moneyToCoinRate\":10,\"coinToMoneyRate\":10,\"moneyToCoinFeeRate\":0,\"coinToMoneyFeeRate\":0,\"maxSourceAmount\":1000.00}", null, 201);
        UUID exchange = id(post(childBase + "/exchange-previews", childToken,
            "{\"direction\":\"MONEY_TO_COIN\",\"sourceAmount\":10.00}", null, 201));
        post("/api/v1/families/" + parent.family + "/exchange-previews/" + exchange + "/confirm", childToken, "", "report-exchange", 200);
        post(childBase + "/saving/transfers", parent.token,
            "{\"direction\":\"DEPOSIT\",\"amount\":10.00}", "report-saving", 201);

        UUID fund = id(post("/api/v1/families/" + parent.family + "/funds", parent.token,
            "{\"name\":\"家庭成长模拟基金\",\"riskLabel\":\"纯模拟，会涨会跌\"}", null, 201));
        post("/api/v1/families/" + parent.family + "/funds/" + fund + "/nav", parent.token,
            "{\"navDate\":\"" + LocalDate.now() + "\",\"nav\":1.000000}", null, 201);
        post("/api/v1/families/" + parent.family + "/funds/" + fund + "/fee-rules", parent.token,
            "{\"buyFeeRate\":0.010000,\"sellFeeRate\":0.010000}", null, 201);
        UUID trade = id(post(childBase + "/funds/" + fund + "/trade-previews", parent.token,
            "{\"side\":\"BUY\",\"inputAmount\":10.00}", null, 201));
        post("/api/v1/families/" + parent.family + "/fund-trade-previews/" + trade + "/confirm", parent.token, "", "report-fund", 200);

        String today = childBase + "/reports/today?date=" + LocalDate.now(ZoneId.of("Asia/Shanghai"));
        mvc.perform(get(today).header("Authorization", bearer(childToken))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appMinutes").value(3)).andExpect(jsonPath("$.data.learningMinutes").value(2))
            .andExpect(jsonPath("$.data.submittedTasks").value(1)).andExpect(jsonPath("$.data.approvedTasks").value(1));
        mvc.perform(get(today).header("Authorization", bearer(other.token))).andExpect(status().isNotFound());

        String monthly = childBase + "/reports/monthly?month=" + YearMonth.now(ZoneId.of("Asia/Shanghai"));
        mvc.perform(get(monthly).header("Authorization", bearer(childToken))).andExpect(status().isForbidden());
        mvc.perform(get(monthly).header("Authorization", bearer(parent.token))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appMinutes").value(3)).andExpect(jsonPath("$.data.learningMinutes").value(2))
            .andExpect(jsonPath("$.data.moneyIncome").value(120.00)).andExpect(jsonPath("$.data.moneyExpense").value(30.00))
            .andExpect(jsonPath("$.data.coinIncome").value(200)).andExpect(jsonPath("$.data.giftMoney").value(20.00))
            .andExpect(jsonPath("$.data.savingBalance").value(10.00)).andExpect(jsonPath("$.data.fundFees").value(0.10))
            .andExpect(jsonPath("$.data.walletLedgerBalanced").value(true));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usage_event WHERE family_id=?", Integer.class, parent.family)).isEqualTo(2);
    }

    private Session bootstrap(String name, String pin) throws Exception {
        MvcResult result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        return new Session(UUID.fromString(text(result, "data", "familyId")), text(result, "data", "session", "token"));
    }

    private MvcResult post(String url, String token, String body, String key, int expected) throws Exception {
        var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).header("Authorization", bearer(token));
        if (key != null) request.header("Idempotency-Key", key);
        if (!body.isEmpty()) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(request).andExpect(status().is(expected)).andReturn();
    }
    private UUID id(MvcResult result) throws Exception { return UUID.fromString(text(result, "data", "id")); }
    private String text(MvcResult result, String... path) throws Exception { JsonNode node = json.readTree(result.getResponse().getContentAsString()); for (String part : path) node = node.path(part); return node.asText(); }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID family, String token) {}
}
