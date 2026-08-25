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
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:familygrowthapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamilyGrowthApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void bootstrapsAndCompletesAuthenticatedThreeRewardFlowExactlyOnce() throws Exception {
        Session parent = bootstrap("成长家庭", "家长甲", "135790");
        String storedPin = jdbc.queryForObject(
            "SELECT pin_hash FROM parent_pin_credential WHERE parent_id = ?", String.class, parent.parentId);
        String storedToken = jdbc.queryForObject(
            "SELECT token_hash FROM auth_session WHERE actor_id = ?", String.class, parent.parentId);
        assertThat(storedPin).startsWith("$2").doesNotContain("135790");
        assertThat(storedToken).hasSize(64).isNotEqualTo(parent.token);

        mvc.perform(post("/api/v1/families/" + parent.familyId + "/children")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"未认证\",\"birthDate\":\"2018-05-01\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isUnauthorized());

        UUID child = id(mvc.perform(post("/api/v1/families/" + parent.familyId + "/children")
            .header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"小树\",\"birthDate\":\"2018-05-01\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isCreated()).andReturn());
        UUID plan = id(mvc.perform(post("/api/v1/families/" + parent.familyId + "/children/" + child + "/plans")
            .header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"阅读计划\",\"description\":\"每日阅读\",\"startDate\":\"2026-08-25\"}"))
            .andExpect(status().isCreated()).andReturn());
        UUID goal = id(mvc.perform(post("/api/v1/families/" + parent.familyId + "/plans/" + plan + "/goals")
            .header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"独立阅读\",\"description\":\"完成一本书\"}"))
            .andExpect(status().isCreated()).andReturn());
        UUID task = id(mvc.perform(post("/api/v1/families/" + parent.familyId + "/goals/" + goal + "/tasks")
            .header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"阅读20分钟\",\"description\":\"专注阅读\",\"category\":\"LEARNING\",\"difficulty\":\"NORMAL\",\"expectedMinutes\":20}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.category").value("LEARNING"))
            .andReturn());

        String childToken = text(mvc.perform(post("/api/v1/auth/child-sessions")
            .header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn(), "data", "token");

        MvcResult firstSubmit = mvc.perform(post("/api/v1/families/" + parent.familyId
                + "/children/" + child + "/tasks/" + task + "/completions")
            .header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "submit-reading-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"evidenceNote\":\"读完第一章\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andReturn();
        UUID completion = id(firstSubmit);

        mvc.perform(post("/api/v1/families/" + parent.familyId
                + "/children/" + child + "/tasks/" + task + "/completions")
            .header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "submit-reading-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"evidenceNote\":\"重复网络请求\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(completion.toString()));

        String reviewBody = "{\"approved\":true,\"xpReward\":20,\"coinReward\":100," +
            "\"moneyReward\":2.00,\"reviewNote\":\"完成认真\"}";
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/completions/" + completion + "/review")
            .header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "review-reading-1")
            .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/completions/" + completion + "/review")
            .header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "review-reading-1")
            .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"))
            .andExpect(jsonPath("$.data.xpReward").value(20))
            .andExpect(jsonPath("$.data.coinReward").value(100))
            .andExpect(jsonPath("$.data.moneyReward").value(2.00));
        mvc.perform(post("/api/v1/families/" + parent.familyId + "/completions/" + completion + "/review")
            .header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "review-reading-1")
            .contentType(MediaType.APPLICATION_JSON).content(reviewBody))
            .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/families/" + parent.familyId + "/children/" + child + "/wallet")
            .header("Authorization", bearer(childToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moneyBalance").value(2.00))
            .andExpect(jsonPath("$.data.coinBalance").value(100));
        mvc.perform(get("/api/v1/families/" + parent.familyId + "/children/" + child + "/ledger")
            .header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2));

        assertThat(jdbc.queryForObject(
            "SELECT xp_balance FROM child_progress WHERE child_id = ?", Long.class, child)).isEqualTo(20L);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM task_completion WHERE id = ?", Integer.class, completion)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM ledger_entry WHERE business_id = ?", Integer.class, completion)).isEqualTo(2);
    }

    @Test
    void rejectsInvalidRoleObjectAndTaskInput() throws Exception {
        Session first = bootstrap("第一家庭", "家长一", "246802");
        Session second = bootstrap("第二家庭", "家长二", "246803");

        mvc.perform(post("/api/v1/families/" + second.familyId + "/children")
            .header("Authorization", bearer(first.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"越权\",\"birthDate\":\"2018-05-01\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isNotFound());

        mvc.perform(post("/api/v1/families/" + first.familyId + "/goals/" + UUID.randomUUID() + "/tasks")
            .header("Authorization", bearer(first.token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"任务\",\"category\":\"LEARNING\",\"difficulty\":\"EASY\",\"expectedMinutes\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void locksPinAfterFiveFailuresWithoutReturningCredentialDetails() throws Exception {
        Session parent = bootstrap("锁定家庭", "家长锁", "112233");
        String wrong = "{\"familyId\":\"" + parent.familyId + "\",\"parentId\":\"" + parent.parentId
            + "\",\"pin\":\"999999\"}";
        for (int attempt = 1; attempt <= 4; attempt++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error.code").value("PIN_LOCKED"));
        String correct = "{\"familyId\":\"" + parent.familyId + "\",\"parentId\":\"" + parent.parentId
            + "\",\"pin\":\"112233\"}";
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(correct))
            .andExpect(status().isTooManyRequests());
        assertThat(jdbc.queryForObject(
            "SELECT failed_attempts FROM parent_pin_credential WHERE parent_id = ?",
            Integer.class, parent.parentId)).isEqualTo(5);
    }

    private Session bootstrap(String familyName, String parentName, String pin) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + familyName + "\",\"parentName\":\"" + parentName
                + "\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.session.token").isNotEmpty())
            .andReturn();
        return new Session(
            UUID.fromString(text(result, "data", "familyId")),
            UUID.fromString(text(result, "data", "parentId")),
            text(result, "data", "session", "token"));
    }

    private UUID id(MvcResult result) throws Exception {
        return UUID.fromString(text(result, "data", "id"));
    }

    private String text(MvcResult result, String... path) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        for (String part : path) {
            node = node.path(part);
        }
        return node.asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(UUID familyId, UUID parentId, String token) {}
}
