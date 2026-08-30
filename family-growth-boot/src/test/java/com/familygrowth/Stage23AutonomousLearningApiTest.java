package com.familygrowth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage23autonomous;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage23AutonomousLearningApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void matchingPublishedCourseSnapshotsPolicyAndSettlesLedgerExactlyOnce() throws Exception {
        Session parent = bootstrap("自主学习家庭", "731426");
        Session other = bootstrap("其他自主家庭", "731427");
        UUID child = child(parent, "小探索", "2017-08-26");
        String childToken = childToken(parent, child);
        String root = "/api/v1/families/" + parent.family + "/children/" + child + "/autonomous-learning";

        mvc.perform(get(root + "/reward-policy").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.moneyReward").value(0.00))
            .andExpect(jsonPath("$.data.version").value(0));
        mvc.perform(put(root + "/reward-policy").header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON).content(policy("1.25", 6, 9, 0)))
            .andExpect(status().isForbidden());
        mvc.perform(get(root.replace(parent.family.toString(), other.family.toString()) + "/reward-policy")
            .header("Authorization", bearer(other.token))).andExpect(status().isNotFound());
        mvc.perform(put(root + "/reward-policy").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(policy("1.25", 6, 9, 0)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(put(root + "/reward-policy").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(policy("2.00", 8, 12, 0)))
            .andExpect(status().isConflict());

        JsonNode primary = createAndPublish(parent, "PRIMARY", "自然观察", "auto-primary");
        createAndPublish(parent, "JUNIOR_MIDDLE", "初中观察", "auto-middle");
        UUID activityId = uuid(primary.path("units").get(0).path("lessons").get(0).path("activities").get(0), "id");

        MvcResult synced = mvc.perform(post(root + "/sync").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "auto-sync-1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].schoolStage").value("PRIMARY"))
            .andExpect(jsonPath("$.data[0].assignmentSource").value("AUTONOMOUS"))
            .andExpect(jsonPath("$.data[0].reward.money").value(1.25))
            .andExpect(jsonPath("$.data[0].reward.coin").value(6))
            .andExpect(jsonPath("$.data[0].reward.xp").value(9))
            .andExpect(jsonPath("$.data[0].reward.settledAt").doesNotExist()).andReturn();
        JsonNode assignment = data(synced).get(0);
        UUID assignmentId = uuid(assignment, "id");
        mvc.perform(post(root + "/sync").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "auto-sync-1")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(post(root + "/sync").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "auto-sync-2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1));

        mvc.perform(put(root + "/reward-policy").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(policy("9.00", 20, 30, 1)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(2));

        String assignments = "/api/v1/families/" + parent.family + "/children/" + child + "/learning/assignments/" + assignmentId;
        mvc.perform(post(assignments + "/activities/" + activityId + "/attempts")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "auto-watch")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"VIEWED\",\"playedSeconds\":8,\"durationSeconds\":10}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/families/" + parent.family + "/children/" + child + "/wallet")
            .header("Authorization", bearer(parent.token))).andExpect(jsonPath("$.data.moneyBalance").value(0.00));
        mvc.perform(post(assignments + "/activities/" + activityId + "/attempts")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "auto-watch-ok")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"VIEWED\",\"playedSeconds\":9,\"durationSeconds\":10}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(post(assignments + "/submit").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "auto-submit").contentType(MediaType.APPLICATION_JSON)
            .content("{\"expectedVersion\":1}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        mvc.perform(post(assignments + "/review").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "auto-approve").contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVE\",\"note\":\"看见你认真看完了\",\"expectedVersion\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.reward.money").value(1.25))
            .andExpect(jsonPath("$.data.reward.settledAt").isNotEmpty());
        mvc.perform(post(assignments + "/review").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "auto-approve").contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"APPROVE\",\"note\":\"看见你认真看完了\",\"expectedVersion\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.reward.settledAt").isNotEmpty());

        mvc.perform(get("/api/v1/families/" + parent.family + "/children/" + child + "/wallet")
            .header("Authorization", bearer(parent.token))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.moneyBalance").value(1.25)).andExpect(jsonPath("$.data.coinBalance").value(6));
        mvc.perform(get("/api/v1/families/" + parent.family + "/children/" + child + "/ledger")
            .header("Authorization", bearer(parent.token))).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].businessType").value("LEARNING_ASSIGNMENT"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT xp_balance FROM child_progress WHERE family_id=? AND child_id=?", Long.class, parent.family, child)).isEqualTo(9L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM ledger_entry WHERE family_id=? AND business_type='LEARNING_ASSIGNMENT' AND business_id=?",
            Integer.class, parent.family, assignmentId)).isEqualTo(2);
    }

    private JsonNode createAndPublish(Session parent, String stage, String title, String key) throws Exception {
        String body = "{\"schoolStage\":\"" + stage + "\",\"subjectCode\":\"SCIENCE\",\"title\":\"" + title +
            "\",\"version\":{\"summary\":\"一段短课程\",\"rightsBasis\":\"家庭原创\",\"units\":[{\"title\":\"单元\",\"lessons\":[{\"title\":\"观察颜色\",\"summary\":\"认真看完再休息\",\"activities\":[{\"type\":\"SHORT_VIDEO\",\"title\":\"看颜色\",\"instruction\":\"看完就停下来\",\"contentRef\":\"lesson_color_garden\",\"expectedMinutes\":3}]}]}]}}";
        MvcResult created = mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/courses")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        JsonNode version = data(created);
        mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/course-versions/" + version.path("versionId").asText() + "/publish")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", key + "-publish"))
            .andExpect(status().isOk());
        return data(mvc.perform(get("/api/v1/families/" + parent.family + "/teaching/course-versions/" + version.path("versionId").asText())
            .header("Authorization", bearer(parent.token))).andReturn());
    }
    private String policy(String money, long coin, long xp, long version) {
        return "{\"moneyReward\":" + money + ",\"coinReward\":" + coin + ",\"xpReward\":" + xp +
            ",\"expectedVersion\":" + version + ",\"auditReason\":\"家长设置固定自主学习奖励\"}";
    }
    private Session bootstrap(String name, String pin) throws Exception {
        JsonNode data = data(mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn());
        return new Session(uuid(data,"familyId"), data.path("session").path("token").asText());
    }
    private UUID child(Session parent, String name, String birthDate) throws Exception {
        return uuid(data(mvc.perform(post("/api/v1/families/" + parent.family + "/children")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"" + name + "\",\"birthDate\":\"" + birthDate + "\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isCreated()).andReturn()), "id");
    }
    private String childToken(Session parent, UUID child) throws Exception {
        return data(mvc.perform(post("/api/v1/auth/child-sessions").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn()).path("token").asText();
    }
    private JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).path("data"); }
    private static UUID uuid(JsonNode node, String field) { return UUID.fromString(node.path(field).asText()); }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID family, String token) { }
}
