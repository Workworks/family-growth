package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
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
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage20api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage20ApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void parentConfiguresStageChildReadsAndAuditIsImmutable() throws Exception {
        Session parent = bootstrap("学段家庭", "357924");
        Session other = bootstrap("其他家庭", "357925");
        UUID child = child(parent, "小禾", "2022-08-26");
        String childToken = childToken(parent, child);
        String url = profileUrl(parent, child);

        mvc.perform(get(url).header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recommendedStage").value("KINDERGARTEN"))
            .andExpect(jsonPath("$.data.effectiveStage").value("KINDERGARTEN"))
            .andExpect(jsonPath("$.data.feedbackProfile.maxAnimationMs").value(320))
            .andExpect(jsonPath("$.data.version").value(0));
        mvc.perform(get(url).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk());
        mvc.perform(get(url).header("Authorization", bearer(other.token)))
            .andExpect(status().isNotFound());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());

        String update = """
            {"birthDate":"2017-08-26","stageOverride":"JUNIOR_MIDDLE",
             "overrideReason":"按实际入学阶段配置","hapticsEnabled":false,"expectedVersion":0,
             "auditReason":"家长确认本学期阶段"}
            """;
        mvc.perform(put(url).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.recommendedStage").value("PRIMARY"))
            .andExpect(jsonPath("$.data.effectiveStage").value("JUNIOR_MIDDLE"))
            .andExpect(jsonPath("$.data.feedbackProfile.hapticsEnabled").value(false))
            .andExpect(jsonPath("$.data.version").value(1));
        mvc.perform(put(url).header("Authorization", bearer(childToken))
            .contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isForbidden());
        mvc.perform(put(url).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(update))
            .andExpect(status().isConflict());
        mvc.perform(put(url).header("Authorization", bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content(update.replace("JUNIOR_MIDDLE", "PARENT_ONLY")))
            .andExpect(status().isBadRequest());

        mvc.perform(get(url + "/audit").header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].reason").value("家长确认本学期阶段"));
        mvc.perform(get(url + "/audit").header("Authorization", bearer(childToken)))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM child_experience_audit WHERE child_id=?",
            Integer.class, child)).isEqualTo(1);
    }

    @Test
    void documentaryCatalogRequiresRightsApprovalAndHidesChildLaunchUrl() throws Exception {
        Session parent = bootstrap("纪录片家庭", "468135");
        Session other = bootstrap("外部家庭", "468136");
        UUID child = child(parent, "小芽", "2017-08-26");
        String childToken = childToken(parent, child);
        String catalog = "/api/v1/families/" + parent.family + "/documentary-sources";
        String body = """
            {"schoolStage":"PRIMARY","title":"NASA 科学活动","description":"由家长打开官方资源",
             "languageTag":"zh-CN","durationSeconds":300,"accessMode":"OFFICIAL_LINK",
             "sourceReference":"https://science.nasa.gov/learn/lessons-and-activities/",
             "rightsHolder":"NASA","rightsReference":"NASA media usage review required"}
            """;

        MvcResult created = mvc.perform(post(catalog).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "documentary-create-1")
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andReturn();
        UUID source = id(created);
        mvc.perform(post(catalog).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "documentary-create-1")
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(source.toString()));
        mvc.perform(post(catalog).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "documentary-child-denied")
            .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post(catalog).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "documentary-http-denied")
            .contentType(MediaType.APPLICATION_JSON).content(body.replace("https://", "http://")))
            .andExpect(status().isBadRequest());

        mvc.perform(post(catalog + "/" + source + "/approve")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "documentary-approve-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        mvc.perform(post(catalog + "/" + source + "/approve")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "documentary-approve-1"))
            .andExpect(status().isOk());

        String childCatalog = "/api/v1/families/" + parent.family + "/children/" + child + "/documentaries";
        mvc.perform(get(childCatalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].parentActionRequired").value(true))
            .andExpect(jsonPath("$.data[0].sourceReference").isEmpty())
            .andExpect(jsonPath("$.data[0].rightsReference").isEmpty());
        mvc.perform(get(catalog).header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].sourceReference").value("https://science.nasa.gov/learn/lessons-and-activities/"));
        mvc.perform(get(catalog).header("Authorization", bearer(other.token))).andExpect(status().isNotFound());

        mvc.perform(post(catalog + "/" + source + "/withdraw")
            .header("Authorization", bearer(parent.token)).header("Idempotency-Key", "documentary-withdraw-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
        mvc.perform(get(childCatalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM documentary_source_action WHERE source_id=?",
            Integer.class, source)).isEqualTo(2);
    }

    private Session bootstrap(String name, String pin) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON)
            .content("{\"familyName\":\"" + name + "\",\"parentName\":\"家长\",\"pin\":\"" + pin + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        return new Session(UUID.fromString(text(result, "data", "familyId")), text(result, "data", "session", "token"));
    }

    private UUID child(Session parent, String name, String birthDate) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/families/" + parent.family + "/children")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"" + name + "\",\"birthDate\":\"" + birthDate
                + "\",\"ageStage\":\"CHILD_6_9\"}"))
            .andExpect(status().isCreated()).andReturn();
        return id(result);
    }

    private String childToken(Session parent, UUID child) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/child-sessions")
            .header("Authorization", bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"childId\":\"" + child + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        return text(result, "data", "token");
    }

    private static String profileUrl(Session parent, UUID child) {
        return "/api/v1/families/" + parent.family + "/children/" + child + "/experience-profile";
    }

    private UUID id(MvcResult result) throws Exception {
        return UUID.fromString(text(result, "data", "id"));
    }

    private String text(MvcResult result, String... path) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        for (String item : path) node = node.path(item);
        return node.asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record Session(UUID family, String token) {
    }
}
