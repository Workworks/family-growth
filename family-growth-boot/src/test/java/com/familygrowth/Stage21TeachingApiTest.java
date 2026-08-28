package com.familygrowth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage21teaching;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage21TeachingApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void immutableVersionNineActivitiesAndReworkEvidenceCloseProductionLoop() throws Exception {
        Session parent = bootstrap("课程家庭", "682146");
        Session other = bootstrap("隔离家庭", "682147");
        UUID child = child(parent, "小课", "2017-08-26");
        String childToken = childToken(parent, child);
        String courses = "/api/v1/families/" + parent.family + "/teaching/courses";

        MvcResult created = mvc.perform(post(courses).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "course-create-1").contentType(MediaType.APPLICATION_JSON).content(courseBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.units[0].lessons[0].activities.length()").value(9)).andReturn();
        JsonNode course = data(created);
        UUID courseId = uuid(course, "courseId");
        UUID versionId = uuid(course, "versionId");
        UUID lessonId = uuid(course.path("units").get(0).path("lessons").get(0), "id");

        mvc.perform(post(courses).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "course-create-1").contentType(MediaType.APPLICATION_JSON).content(courseBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.versionId").value(versionId.toString()));
        mvc.perform(post(courses).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "course-child-create").contentType(MediaType.APPLICATION_JSON).content(courseBody()))
            .andExpect(status().isForbidden());
        mvc.perform(get(courses.replace(parent.family.toString(), other.family.toString()))
            .header("Authorization", bearer(parent.token))).andExpect(status().isNotFound());

        String publish = "/api/v1/families/" + parent.family + "/teaching/course-versions/" + versionId + "/publish";
        mvc.perform(post(publish).header("Authorization", bearer(parent.token)).header("Idempotency-Key", "publish-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        mvc.perform(post(publish).header("Authorization", bearer(parent.token)).header("Idempotency-Key", "publish-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.versionId").value(versionId.toString()));
        mvc.perform(post(publish).header("Authorization", bearer(parent.token)).header("Idempotency-Key", "publish-again"))
            .andExpect(status().isConflict());

        mvc.perform(post(courses + "/" + courseId + "/versions").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "version-create-2").contentType(MediaType.APPLICATION_JSON)
            .content(versionBody("第二版仍须重新发布")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.versionNumber").value(2))
            .andExpect(jsonPath("$.data.status").value("DRAFT"));

        String assignmentBase = "/api/v1/families/" + parent.family + "/children/" + child + "/learning/assignments";
        String assignmentBody = "{\"courseVersionId\":\"" + versionId + "\",\"lessonId\":\"" + lessonId + "\"}";
        MvcResult assigned = mvc.perform(post(assignmentBase).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "assign-1").contentType(MediaType.APPLICATION_JSON).content(assignmentBody))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("ASSIGNED"))
            .andExpect(jsonPath("$.data.activities[2].answerKey").doesNotExist()).andReturn();
        JsonNode assignment = data(assigned);
        UUID assignmentId = uuid(assignment, "id");
        List<UUID> activities = new ArrayList<>();
        assignment.path("activities").forEach(node -> activities.add(uuid(node, "id")));

        mvc.perform(get(assignmentBase).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].activities[2].answerKey").doesNotExist())
            .andExpect(jsonPath("$.data[0].activities[2].requiredEvidence").value("CHECKED"));
        String attemptBase = assignmentBase + "/" + assignmentId + "/activities/";
        mvc.perform(post(attemptBase + activities.get(0) + "/attempts").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "parent-cannot-attempt").contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"VIEWED\"}"))
            .andExpect(status().isForbidden());

        long version = 0;
        version = attemptVideo(childToken, attemptBase, activities.get(0), "attempt-video", version + 1);
        long replayVersion = attemptVideo(childToken, attemptBase, activities.get(0), "attempt-video", version);
        org.assertj.core.api.Assertions.assertThat(replayVersion).isEqualTo(version);
        version = attempt(childToken, attemptBase, activities.get(1), "和家长读完了", "attempt-read", version + 1);
        version = attempt(childToken, attemptBase, activities.get(2), "wrong", "attempt-listen-wrong", version + 1);
        mvc.perform(post(assignmentBase + "/" + assignmentId + "/submit").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "submit-too-early").contentType(MediaType.APPLICATION_JSON)
            .content("{\"expectedVersion\":" + version + "}"))
            .andExpect(status().isConflict());
        version = attempt(childToken, attemptBase, activities.get(2), "ok", "attempt-listen-ok", version + 1);
        for (int i = 3; i <= 5; i++) version = attempt(childToken, attemptBase, activities.get(i), "ok", "attempt-objective-" + i, version + 1);
        for (int i = 6; i <= 8; i++) version = attempt(childToken, attemptBase, activities.get(i), "做完了", "attempt-parent-" + i, version + 1);

        MvcResult submitted = mvc.perform(post(assignmentBase + "/" + assignmentId + "/submit")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "submit-1")
            .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":" + version + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUBMITTED")).andReturn();
        version = data(submitted).path("version").asLong();
        mvc.perform(post(attemptBase + activities.get(0) + "/attempts").header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "attempt-video").contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"VIEWED\",\"playedSeconds\":9,\"durationSeconds\":10}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        mvc.perform(post(assignmentBase + "/" + assignmentId + "/submit")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "submit-1")
            .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":" + (version - 1) + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        MvcResult reworked = mvc.perform(post(assignmentBase + "/" + assignmentId + "/review")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "review-rework")
            .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REWORK\",\"note\":\"再慢慢说一次\",\"expectedVersion\":" + version + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REWORK_REQUIRED")).andReturn();
        version = data(reworked).path("version").asLong();
        mvc.perform(post(assignmentBase + "/" + assignmentId + "/submit")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "submit-without-rework")
            .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":" + version + "}"))
            .andExpect(status().isConflict());
        version = attempt(childToken, attemptBase, activities.get(6), "重新说完了", "attempt-rework", version + 1);
        MvcResult resubmitted = mvc.perform(post(assignmentBase + "/" + assignmentId + "/submit")
            .header("Authorization", bearer(childToken)).header("Idempotency-Key", "submit-2")
            .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":" + version + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("SUBMITTED")).andReturn();
        version = data(resubmitted).path("version").asLong();
        mvc.perform(post(assignmentBase + "/" + assignmentId + "/review")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "review-approve")
            .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"note\":\"看见你认真重试了\",\"expectedVersion\":" + version + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.activities[1].evidence").isArray())
            .andExpect(jsonPath("$.data.activities[1].evidence").value(org.hamcrest.Matchers.hasItem("PARENT_CONFIRMED")));
        mvc.perform(post(assignmentBase + "/" + assignmentId + "/review")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "review-approve")
            .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"note\":\"看见你认真重试了\",\"expectedVersion\":" + version + "}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    private long attempt(String token, String base, UUID activity, String response, String key, long expected) throws Exception {
        MvcResult result = mvc.perform(post(base + activity + "/attempts").header("Authorization", bearer(token))
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"" + response + "\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(expected)).andReturn();
        return data(result).path("version").asLong();
    }
    private long attemptVideo(String token, String base, UUID activity, String key, long expected) throws Exception {
        MvcResult result = mvc.perform(post(base + activity + "/attempts").header("Authorization", bearer(token))
            .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON)
            .content("{\"responseText\":\"VIEWED\",\"playedSeconds\":9,\"durationSeconds\":10}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.version").value(expected)).andReturn();
        return data(result).path("version").asLong();
    }

    private String courseBody() { return "{\"schoolStage\":\"PRIMARY\",\"subjectCode\":\"SCIENCE\",\"title\":\"身边的自然\",\"version\":" + versionBody("家庭原创与公共领域素材") + "}"; }
    private String versionBody(String rights) {
        return """
            {"summary":"一次只学一个小主题","rightsBasis":"%s","units":[{"title":"第一单元","lessons":[{
              "title":"听见雨声","summary":"看看、选选，再和家长说一说","activities":[
                {"type":"SHORT_VIDEO","title":"看一小段","instruction":"看完后停下来看看窗外","contentRef":"lesson_color_garden","expectedMinutes":3},
                {"type":"PARENT_CHILD_READING","title":"一起读","instruction":"请家长陪你读一页","expectedMinutes":5},
                {"type":"LISTEN_CHOOSE","title":"听一听","instruction":"选出雨声","expectedMinutes":2,"prompt":"哪个是雨声？","hint":"慢慢听","options":[{"value":"ok","label":"滴答"},{"value":"no","label":"喵喵"}],"answerKey":"ok"},
                {"type":"SINGLE_CHOICE","title":"选一选","instruction":"选出雨天用品","expectedMinutes":2,"prompt":"下雨带什么？","options":[{"value":"ok","label":"雨伞"},{"value":"no","label":"扇子"}],"answerKey":"ok"},
                {"type":"MATCHING","title":"连一连","instruction":"把声音和天气连起来","expectedMinutes":3,"prompt":"完成配对","options":[{"value":"ok","label":"雨-滴答"},{"value":"no","label":"雨-鸟叫"}],"answerKey":"ok"},
                {"type":"SORTING","title":"排一排","instruction":"排出观察顺序","expectedMinutes":3,"prompt":"先看再说","options":[{"value":"ok","label":"看,想,说"},{"value":"no","label":"说,看,想"}],"answerKey":"ok"},
                {"type":"ORAL_RESPONSE","title":"说一说","instruction":"告诉家长你听到了什么","expectedMinutes":3},
                {"type":"OFFLINE_PRACTICE","title":"找一找","instruction":"离开屏幕找一处雨滴","expectedMinutes":5},
                {"type":"PARENT_CONFIRMATION","title":"请家长回应","instruction":"把你的发现说给家长听","expectedMinutes":2}
              ]}]}]}
            """.formatted(rights).replaceAll("\\s+", " ");
    }

    private Session bootstrap(String name, String pin) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        JsonNode data = data(result);
        return new Session(uuid(data, "familyId"), data.path("session").path("token").asText());
    }
    private UUID child(Session parent, String name, String birthDate) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/families/" + parent.family + "/children")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"" + name + "\",\"birthDate\":\"" + birthDate + "\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isCreated()).andReturn();
        return uuid(data(result), "id");
    }
    private String childToken(Session parent, UUID child) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/child-sessions").header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        return data(result).path("token").asText();
    }
    private JsonNode data(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).path("data"); }
    private static UUID uuid(JsonNode node, String field) { return UUID.fromString(node.path(field).asText()); }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID family, String token) { }
}
