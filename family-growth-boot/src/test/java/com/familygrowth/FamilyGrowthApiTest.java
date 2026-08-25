package com.familygrowth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode; import com.fasterxml.jackson.databind.ObjectMapper; import java.util.UUID; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.http.MediaType; import org.springframework.test.context.ActiveProfiles; import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:familygrowthapi;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","spring.datasource.username=sa","spring.datasource.password=","spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class FamilyGrowthApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json;
 @Test void createsFamilyChildPlanGoalAndLearningTask() throws Exception {
   var family=id(mvc.perform(post("/api/v1/families").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"成长家庭\"}" )).andExpect(status().isCreated()).andReturn());
   var child=id(mvc.perform(post("/api/v1/families/"+family+"/children").contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"小树\",\"birthDate\":\"2018-05-01\",\"ageStage\":\"CHILD_6_9\"}" )).andExpect(status().isCreated()).andReturn());
   var plan=id(mvc.perform(post("/api/v1/families/"+family+"/children/"+child+"/plans").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"阅读计划\",\"description\":\"每日阅读\",\"startDate\":\"2026-08-25\"}" )).andExpect(status().isCreated()).andReturn());
   var goal=id(mvc.perform(post("/api/v1/families/"+family+"/plans/"+plan+"/goals").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"独立阅读\",\"description\":\"完成一本书\"}" )).andExpect(status().isCreated()).andReturn());
   mvc.perform(post("/api/v1/families/"+family+"/goals/"+goal+"/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"阅读20分钟\",\"description\":\"专注阅读\",\"category\":\"LEARNING\",\"difficulty\":\"NORMAL\",\"expectedMinutes\":20}" )).andExpect(status().isCreated()).andExpect(jsonPath("$.data.category").value("LEARNING"));
   mvc.perform(post("/api/v1/families/"+UUID.randomUUID()+"/children/"+child+"/plans").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"越权\",\"startDate\":\"2026-08-25\"}" )).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
 }
 @Test void rejectsInvalidTaskDuration() throws Exception { mvc.perform(post("/api/v1/families/"+UUID.randomUUID()+"/goals/"+UUID.randomUUID()+"/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"任务\",\"category\":\"LEARNING\",\"difficulty\":\"EASY\",\"expectedMinutes\":0}" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED")); }
 private UUID id(org.springframework.test.web.servlet.MvcResult result) throws Exception { JsonNode root=json.readTree(result.getResponse().getContentAsString()); return UUID.fromString(root.path("data").path("id").asText()); }
}
