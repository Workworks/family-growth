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
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage5api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage5ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void giftAndBidirectionalExchangeAreAuthorizedAuditableAndIdempotent() throws Exception {
        Session parent = bootstrap();
        UUID child = id(mvc.perform(post("/api/v1/families/" + parent.familyId + "/children")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"小芽\",\"birthDate\":\"2022-01-01\",\"ageStage\":\"EARLY_CHILDHOOD_2_5\"}"))
            .andExpect(status().isCreated()).andReturn());
        String childToken = text(mvc.perform(post("/api/v1/auth/child-sessions")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn(), "data", "token");

        String giftUrl = "/api/v1/families/" + parent.familyId + "/children/" + child + "/gift-money";
        mvc.perform(post(giftUrl).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "gift-denied").contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":1.00}"))
            .andExpect(status().isForbidden());
        mvc.perform(post(giftUrl).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "gift-spring-1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":100.00,\"note\":\"压岁钱\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.amount").value(100.00));
        mvc.perform(post(giftUrl).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "gift-spring-1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":100.00,\"note\":\"网络重试\"}"))
            .andExpect(status().isCreated());

        String ruleUrl = "/api/v1/families/" + parent.familyId + "/exchange-rules";
        mvc.perform(post(ruleUrl).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON).content(ruleBody()))
            .andExpect(status().isForbidden());
        mvc.perform(post(ruleUrl).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(ruleBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(get(ruleUrl + "/active").header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.moneyToCoinRate").value(10.0));

        String previews = "/api/v1/families/" + parent.familyId + "/children/" + child + "/exchange-previews";
        UUID buyPreview = id(mvc.perform(post(previews).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"direction\":\"MONEY_TO_COIN\",\"sourceAmount\":10.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.targetAmount").value(100.00))
            .andExpect(jsonPath("$.data.appliedRate").value(10.0))
            .andExpect(jsonPath("$.data.educationNotice").isNotEmpty())
            .andReturn());
        String confirm = "/api/v1/families/" + parent.familyId + "/exchange-previews/" + buyPreview + "/confirm";
        String orderId = text(mvc.perform(post(confirm).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "exchange-buy-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceFee").value(0.00))
            .andReturn(), "data", "id");
        mvc.perform(post(confirm).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "exchange-buy-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(orderId));
        mvc.perform(post(confirm).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "exchange-buy-second-key"))
            .andExpect(status().isConflict());

        UUID sellPreview = id(mvc.perform(post(previews).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"direction\":\"COIN_TO_MONEY\",\"sourceAmount\":60.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.targetAmount").value(5.00))
            .andReturn());
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/exchange-previews/" + sellPreview + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "exchange-sell-1"))
            .andExpect(status().isOk());

        UUID stalePreview = id(mvc.perform(post(previews).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"direction\":\"MONEY_TO_COIN\",\"sourceAmount\":1.00}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(post(ruleUrl).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(ruleBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.version").value(2));
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/exchange-previews/" + stalePreview + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "exchange-stale-1"))
            .andExpect(status().isConflict());
        UUID overdrawPreview = id(mvc.perform(post(previews).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"direction\":\"MONEY_TO_COIN\",\"sourceAmount\":1000.00}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/exchange-previews/" + overdrawPreview + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "exchange-overdraw-1"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/exchange-previews/" + overdrawPreview + "/confirm")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "exchange-buy-1"))
            .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/families/" + parent.familyId + "/children/" + child + "/wallet")
            .header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.moneyBalance").value(95.00))
            .andExpect(jsonPath("$.data.coinBalance").value(40));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM gift_money WHERE family_id=?", Integer.class,
            parent.familyId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM exchange_order WHERE family_id=?", Integer.class,
            parent.familyId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry WHERE family_id=? AND child_id=?",
            Integer.class, parent.familyId, child)).isEqualTo(5);
    }

    private Session bootstrap() throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"Stage5家庭\",\"parentName\":\"家长\",\"pin\":\"135790\"}"))
            .andExpect(status().isCreated()).andReturn();
        return new Session(UUID.fromString(text(result,"data","familyId")), text(result,"data","session","token"));
    }
    private static String ruleBody() {
        return "{\"moneyToCoinRate\":10,\"coinToMoneyRate\":12,\"moneyToCoinFeeRate\":0," +
            "\"coinToMoneyFeeRate\":0,\"maxSourceAmount\":1000.00}";
    }
    private UUID id(MvcResult result) throws Exception { return UUID.fromString(text(result,"data","id")); }
    private String text(MvcResult result, String... path) throws Exception {
        JsonNode node=json.readTree(result.getResponse().getContentAsString());
        for(String part:path) node=node.path(part); return node.asText();
    }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID familyId,String token) {}
}
