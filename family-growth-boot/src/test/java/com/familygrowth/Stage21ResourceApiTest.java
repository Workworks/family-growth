package com.familygrowth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.familygrowth.application.EducationResourceDiscovery;
import com.familygrowth.application.EducationResourceDiscovery.DiscoveredCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage21resource;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Stage21ResourceApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean EducationResourceDiscovery discovery;

    @Test
    void parentDiscoversApprovesAndWithdrawsChildSafeDynamicCatalog() throws Exception {
        Session parent = bootstrap("资源家庭", "581246");
        Session other = bootstrap("其他资源家庭", "581247");
        UUID child = child(parent, "小林", "2017-08-26");
        String childToken = childToken(parent, child);
        String base = "/api/v1/families/" + parent.family + "/education-resource-sources";
        String body = """
            {"title":"公益科学课堂","sourceUrl":"https://learn.example.org/",
             "schoolStages":["PRIMARY","JUNIOR_MIDDLE"],"usageNote":"家长确认该站内容可免费在线浏览"}
            """;
        when(discovery.discover(any())).thenReturn(List.of(
            new DiscoveredCategory("数学", "https://learn.example.org/math"),
            new DiscoveredCategory("科学探索", "https://learn.example.org/science")));

        MvcResult created = mvc.perform(post(base).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-create-1").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.refreshStatus").value("NEVER")).andReturn();
        UUID source = id(created);
        mvc.perform(post(base).header("Authorization", bearer(childToken))
            .header("Idempotency-Key", "resource-child-create").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
        mvc.perform(post(base).header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-localhost").contentType(MediaType.APPLICATION_JSON)
            .content(body.replace("https://learn.example.org/", "https://127.0.0.1/private")))
            .andExpect(status().isBadRequest());

        mvc.perform(post(base + "/" + source + "/refresh").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-refresh-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.refreshStatus").value("READY"))
            .andExpect(jsonPath("$.data.categories.length()").value(2));
        mvc.perform(post(base + "/" + source + "/approve").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-approve-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));

        String catalog = "/api/v1/families/" + parent.family + "/children/" + child
            + "/education-resource-catalog";
        mvc.perform(get(catalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].categories.length()").value(2))
            .andExpect(jsonPath("$.data[0].sourceUrl").doesNotExist())
            .andExpect(jsonPath("$.data[0].categories[0].categoryUrl").doesNotExist())
            .andExpect(jsonPath("$.data[0].parentActionRequired").value(true));
        mvc.perform(get(base).header("Authorization", bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].sourceUrl")
                .value("https://learn.example.org/"));
        mvc.perform(get(base).header("Authorization", bearer(other.token))).andExpect(status().isNotFound());

        mvc.perform(post(base + "/" + source + "/refresh").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-refresh-2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.refreshStatus").value("READY"));
        mvc.perform(get(catalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(post(base + "/" + source + "/approve").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-approve-2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));

        when(discovery.discover(any())).thenThrow(new IllegalArgumentException("来源暂时无法读取"));
        mvc.perform(post(base + "/" + source + "/refresh").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-refresh-3"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.refreshStatus").value("FAILED"))
            .andExpect(jsonPath("$.data.categories.length()").value(2));
        mvc.perform(get(catalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].categories.length()").value(2));

        mvc.perform(post(base + "/" + source + "/withdraw").header("Authorization", bearer(parent.token))
            .header("Idempotency-Key", "resource-withdraw-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
        mvc.perform(get(catalog).header("Authorization", bearer(childToken)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
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

    private UUID id(MvcResult result) throws Exception { return UUID.fromString(text(result, "data", "id")); }
    private String text(MvcResult result, String... path) throws Exception {
        JsonNode node = json.readTree(result.getResponse().getContentAsString());
        for (String item : path) node = node.path(item);
        return node.asText();
    }
    private static String bearer(String token) { return "Bearer " + token; }
    private record Session(UUID family, String token) { }
}
