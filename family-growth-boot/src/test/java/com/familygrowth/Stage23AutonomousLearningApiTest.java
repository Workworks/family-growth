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

    @Test
    void helpIncorrectClassificationAndDueRevisitRemainProcessEvidence() throws Exception {
        Session parent = bootstrap("学习支持家庭", "731428");
        Session other = bootstrap("隔离支持家庭", "731429");
        UUID child = child(parent, "小读者", "2017-08-26");
        String childToken = childToken(parent, child);
        String courseBody = "{\"schoolStage\":\"PRIMARY\",\"subjectCode\":\"MATH\",\"title\":\"分组练习\"," +
            "\"version\":{\"summary\":\"先操作再解释\",\"rightsBasis\":\"Family Growth 原创 · PRIMARY-PACK-1.0.0\"," +
            "\"units\":[{\"title\":\"数与分组\",\"lessons\":[{\"title\":\"十二个怎样分\",\"summary\":\"用实物验证\"," +
            "\"activities\":[{\"type\":\"SINGLE_CHOICE\",\"title\":\"选择没有剩余的分法\",\"instruction\":\"先用积木分一分\"," +
            "\"expectedMinutes\":8,\"prompt\":\"12 个每组几个能正好分完？\",\"hint\":\"摆成同样多的小组\"," +
            "\"options\":[{\"value\":\"5\",\"label\":\"每组 5 个\"},{\"value\":\"3\",\"label\":\"每组 3 个\"}],\"answerKey\":\"3\"}]}]}]}}";
        JsonNode created = data(mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/courses")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "support-course")
            .contentType(MediaType.APPLICATION_JSON).content(courseBody)).andExpect(status().isCreated()).andReturn());
        UUID version = uuid(created, "versionId");
        mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/course-versions/" + version + "/publish")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "support-publish")).andExpect(status().isOk());
        JsonNode published = data(mvc.perform(get("/api/v1/families/" + parent.family + "/teaching/course-versions/" + version)
            .header("Authorization", bearer(parent.token))).andReturn());
        UUID activity = uuid(published.path("units").get(0).path("lessons").get(0).path("activities").get(0), "id");
        JsonNode withdrawn = createAndPublish(parent, "PRIMARY", "已撤回观察", "support-withdraw-course");
        UUID withdrawnVersion = uuid(withdrawn, "versionId");
        UUID withdrawnLesson = uuid(withdrawn.path("units").get(0).path("lessons").get(0), "id");
        mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/course-versions/" + withdrawnVersion + "/withdraw")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "support-withdraw")
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"内容需要重新核对，停止后续加入\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.courseVersionId").value(withdrawnVersion.toString()));
        mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/course-versions/" + withdrawnVersion + "/withdraw")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "support-withdraw")
            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"内容需要重新核对，停止后续加入\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/families/" + parent.family + "/children/" + child + "/learning/assignments")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "withdrawn-manual-assign")
            .contentType(MediaType.APPLICATION_JSON).content("{\"courseVersionId\":\"" + withdrawnVersion + "\",\"lessonId\":\"" + withdrawnLesson + "\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/families/" + parent.family + "/teaching/courses").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[1].status").value("WITHDRAWN"));
        String root = "/api/v1/families/" + parent.family + "/children/" + child + "/autonomous-learning";
        JsonNode assignment = data(mvc.perform(post(root + "/sync").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "support-sync")).andExpect(status().isOk()).andReturn()).get(0);
        UUID assignmentId = uuid(assignment, "id");
        String support = root + "/assignments/" + assignmentId;

        mvc.perform(post(support + "/activities/" + activity + "/help").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "support-help").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"这里我没看懂，请和我一起看看。\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.data.version").value(0));
        mvc.perform(post(support + "/activities/" + activity + "/help").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "support-help").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\":\"这里我没看懂，请和我一起看看。\"}"))
            .andExpect(status().isOk());

        String attempt = "/api/v1/families/" + parent.family + "/children/" + child + "/learning/assignments/" + assignmentId + "/activities/" + activity + "/attempts";
        mvc.perform(post(attempt).header("Authorization", bearer(childToken)).header("Idempotency-Key", "support-wrong")
            .contentType(MediaType.APPLICATION_JSON).content("{\"responseText\":\"5\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activities[0].checkedCorrect").value(false));
        MvcResult timeline = mvc.perform(get(support + "/support-events").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[1].type").value("INCORRECT_OBSERVED")).andReturn();
        UUID wrongEvent = uuid(data(timeline).get(1), "id");
        mvc.perform(get(support.replace(parent.family.toString(), other.family.toString()) + "/support-events")
            .header("Authorization", bearer(other.token))).andExpect(status().isNotFound());

        String revisit = java.time.Instant.now().plus(java.time.Duration.ofDays(2)).toString();
        mvc.perform(post(support + "/support-events/classify").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "support-classify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"sourceEventId\":\"" + wrongEvent + "\",\"category\":\"PROCEDURE\",\"privateNote\":\"先用十二颗积木分组，不评价能力\",\"revisitAt\":\"" + revisit + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(4));
        mvc.perform(get(support + "/support-events").header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[2].privateNote").value(""));
        jdbc.update("UPDATE learning_support_event SET revisit_at=? WHERE assignment_id=? AND event_type='REVISIT_SCHEDULED'",
            java.sql.Timestamp.from(java.time.Instant.now().minus(java.time.Duration.ofDays(1))), assignmentId);
        jdbc.update("INSERT INTO usage_event(id,family_id,child_id,event_type,minutes,occurred_at,idempotency_key,actor_id,created_at) VALUES(?,?,?,?,?,?,?,?,?)",
            UUID.randomUUID(), parent.family, child, "LEARNING", 12, java.sql.Timestamp.from(java.time.Instant.now()),
            "primary-report-minutes", child, java.sql.Timestamp.from(java.time.Instant.now()));
        mvc.perform(get(root + "/primary-report").header("Authorization", bearer(childToken)))
            .andExpect(status().isForbidden());
        mvc.perform(get(root.replace(parent.family.toString(), other.family.toString()) + "/primary-report")
            .header("Authorization", bearer(other.token))).andExpect(status().isNotFound());
        MvcResult report = mvc.perform(get(root + "/primary-report").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.effectiveStage").value("PRIMARY"))
            .andExpect(jsonPath("$.data.effectivePrimaryBand").value("UPPER_PRIMARY"))
            .andExpect(jsonPath("$.data.recordedLearningMinutes").value(12))
            .andExpect(jsonPath("$.data.subjects[0].subjectCode").value("MATH"))
            .andExpect(jsonPath("$.data.subjects[0].inProgress").value(1))
            .andExpect(jsonPath("$.data.subjects[0].openSupport").value(1))
            .andExpect(jsonPath("$.data.subjects[0].scheduledRevisits").value(1))
            .andExpect(jsonPath("$.data.subjects[0].dueRevisits").value(1)).andReturn();
        org.assertj.core.api.Assertions.assertThat(report.getResponse().getContentAsString())
            .doesNotContain("privateNote").doesNotContain("answerKey").doesNotContain("先用十二颗积木");
        mvc.perform(post(attempt).header("Authorization", bearer(childToken)).header("Idempotency-Key", "support-correct")
            .contentType(MediaType.APPLICATION_JSON).content("{\"responseText\":\"3\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.activities[0].checkedCorrect").value(true));
        mvc.perform(get(support + "/support-events").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[4].type").value("REVISIT_COMPLETED"));
        mvc.perform(get(root + "/primary-report").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subjects[0].scheduledRevisits").value(0))
            .andExpect(jsonPath("$.data.subjects[0].dueRevisits").value(0));
        mvc.perform(put("/api/v1/families/" + parent.family + "/children/" + child + "/experience-profile")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"birthDate\":\"2017-08-26\",\"stageOverride\":\"JUNIOR_MIDDLE\",\"primaryBandOverride\":null," +
                "\"overrideReason\":\"已进入初中阶段\",\"hapticsEnabled\":true,\"expectedVersion\":0," +
                "\"auditReason\":\"验证切换学段后保留小学事实\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.effectiveStage").value("JUNIOR_MIDDLE"));
        mvc.perform(get(root + "/primary-report").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.effectiveStage").value("JUNIOR_MIDDLE"))
            .andExpect(jsonPath("$.data.effectivePrimaryBand").isEmpty())
            .andExpect(jsonPath("$.data.subjects[0].subjectCode").value("MATH"))
            .andExpect(jsonPath("$.data.subjects[0].inProgress").value(1));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_attempt WHERE assignment_id=?", Integer.class, assignmentId)).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM learning_support_event WHERE assignment_id=? AND event_type='HELP_REQUESTED'", Integer.class, assignmentId)).isEqualTo(1);
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
