package com.familygrowth;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage22kindergarten;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage22KindergartenTeachingApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void kindergartenMetadataPersistsAndPublicationRejectsUnsafeLessonShapes() throws Exception {
        Session parent = bootstrap("幼儿园内容家庭", "624810");
        UUID valid = create(parent, "valid", validVersion())
            .andExpect(jsonPath("$.data.kindergartenAgeBand").value("SHARED_3_4"))
            .andExpect(jsonPath("$.data.kindergartenDomains.length()").value(2))
            .andReturnVersion(json);
        publish(parent, valid, "publish-valid").andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        UUID missingBand = create(parent, "missing-band", unsafeVersion("", validActivities())).andReturnVersion(json);
        publish(parent, missingBand, "publish-missing-band").andExpect(status().isBadRequest());

        String threeChoices = """
            [{"type":"LISTEN_CHOOSE","title":"听一听","instruction":"选出雨声","expectedMinutes":2,
              "prompt":"哪个是雨声？","options":[{"value":"a","label":"滴答"},{"value":"b","label":"喵喵"},{"value":"c","label":"叮咚"}],"answerKey":"a"},
             {"type":"OFFLINE_PRACTICE","title":"找一找","instruction":"离开屏幕找一片叶子","expectedMinutes":5}]
            """.replaceAll("\\s+", " ");
        UUID crowded = create(parent, "crowded", unsafeVersion("\"kindergartenAgeBand\":\"SHARED_3_4\",", threeChoices)).andReturnVersion(json);
        publish(parent, crowded, "publish-crowded").andExpect(status().isBadRequest());

        String screenOnly = """
            [{"type":"SHORT_VIDEO","title":"看一小段","instruction":"和家长一起看","contentRef":"lesson_color_garden","expectedMinutes":3},
             {"type":"LISTEN_CHOOSE","title":"听一听","instruction":"选出雨声","expectedMinutes":2,
              "prompt":"哪个是雨声？","options":[{"value":"a","label":"滴答"},{"value":"b","label":"喵喵"}],"answerKey":"a"}]
            """.replaceAll("\\s+", " ");
        UUID noOffline = create(parent, "screen-only", unsafeVersion("\"kindergartenAgeBand\":\"TRANSITION_5_6\",", screenOnly)).andReturnVersion(json);
        publish(parent, noOffline, "publish-screen-only").andExpect(status().isBadRequest());
    }

    private Creation create(Session parent, String key, String version) throws Exception {
        String body = "{\"schoolStage\":\"KINDERGARTEN\",\"subjectCode\":\"SCIENCE\",\"title\":\"自然小发现\",\"version\":" + version + "}";
        MvcResult result = mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/courses")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "stage22-" + key)
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        return new Creation(result);
    }

    private org.springframework.test.web.servlet.ResultActions publish(Session parent, UUID version, String key) throws Exception {
        return mvc.perform(post("/api/v1/families/" + parent.family + "/teaching/course-versions/" + version + "/publish")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", key));
    }

    private String validVersion() {
        return unsafeVersion("\"kindergartenAgeBand\":\"SHARED_3_4\",", validActivities());
    }

    private String validActivities() {
        return """
            [{"type":"SHORT_VIDEO","title":"看一小段","instruction":"和家长一起看","contentRef":"lesson_color_garden","expectedMinutes":3},
             {"type":"LISTEN_CHOOSE","title":"听一听","instruction":"选出雨声","expectedMinutes":2,
              "prompt":"哪个是雨声？","options":[{"value":"a","label":"滴答"},{"value":"b","label":"喵喵"}],"answerKey":"a"},
             {"type":"OFFLINE_PRACTICE","title":"找一找","instruction":"离开屏幕找一片叶子","expectedMinutes":5}]
            """.replaceAll("\\s+", " ");
    }

    private String unsafeVersion(String ageBandProperty, String activities) {
        return "{\"summary\":\"看看再去现实中做\",\"rightsBasis\":\"家庭原创活动\"," + ageBandProperty
            + "\"kindergartenDomains\":[\"SCIENCE\",\"LANGUAGE\"],\"units\":[{\"title\":\"自然\",\"lessons\":[{\"title\":\"听雨\",\"summary\":\"听听再找找\",\"activities\":"
            + activities + "}]}]}";
    }

    private Session bootstrap(String name, String pin) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        JsonNode data = json.readTree(result.getResponse().getContentAsString()).path("data");
        return new Session(UUID.fromString(data.path("familyId").asText()), data.path("session").path("token").asText());
    }

    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID family, String token) { }

    private static final class Creation {
        private final MvcResult result;
        Creation(MvcResult result) { this.result = result; }
        Creation andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }
        UUID andReturnVersion(ObjectMapper json) throws Exception {
            return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).path("data").path("versionId").asText());
        }
    }
}
