package com.familygrowth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties={"spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage24plan;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
 "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}","spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}","spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class Stage24JuniorPlanApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
 @Test void childReordersOnlyUnstartedJuniorAutonomousAssignmentsWithAudit() throws Exception {
  Session parent=bootstrap("初中计划家庭","741852"),other=bootstrap("隔离计划家庭","741853");
  UUID child=child(parent,"小研究员","2013-08-26");String childToken=childToken(parent,child);
  publish(parent,"物理观察","PHYSICS","junior-course-1");publish(parent,"数学模型","MATH","junior-course-2");publish(parent,"文本证据","CHINESE","junior-course-3");
  String auto="/api/v1/families/"+parent.family+"/children/"+child+"/autonomous-learning/sync";
  mvc.perform(post(auto).header("Authorization",bearer(childToken)).header("Idempotency-Key","junior-sync"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(3))
   .andExpect(jsonPath("$.data[0].juniorMetadata.chapterTitle").value("证据章节"))
   .andExpect(jsonPath("$.data[0].juniorMetadata.knowledgePoints.length()").value(2))
   .andExpect(jsonPath("$.data[0].juniorMetadata.safetyNote").value("只使用纸笔和日常安全物品"));
  String plan="/api/v1/families/"+parent.family+"/children/"+child+"/junior-learning/plan";
  JsonNode first=data(mvc.perform(get(plan).header("Authorization",bearer(childToken))).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.items.length()").value(3)).andExpect(jsonPath("$.data.revision").value(1)).andReturn());
  UUID originalFirst=UUID.fromString(first.path("items").get(0).path("assignmentId").asText());
  UUID second=UUID.fromString(first.path("items").get(1).path("assignmentId").asText());
  UUID third=UUID.fromString(first.path("items").get(2).path("assignmentId").asText());
  String move="{\"assignmentId\":\""+second+"\",\"direction\":\"UP\",\"expectedRevision\":1}";
  mvc.perform(post(plan+"/move").header("Authorization",bearer(childToken)).header("Idempotency-Key","junior-move")
   .contentType(MediaType.APPLICATION_JSON).content(move)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.revision").value(2)).andExpect(jsonPath("$.data.items[0].assignmentId").value(second.toString()));
  mvc.perform(post(plan+"/move").header("Authorization",bearer(childToken)).header("Idempotency-Key","junior-move")
   .contentType(MediaType.APPLICATION_JSON).content(move)).andExpect(status().isOk()).andExpect(jsonPath("$.data.revision").value(2));
  jdbc.update("UPDATE lesson_assignment SET status='IN_PROGRESS' WHERE id=?",originalFirst);
  String skipStarted="{\"assignmentId\":\""+third+"\",\"direction\":\"UP\",\"expectedRevision\":2}";
  mvc.perform(post(plan+"/move").header("Authorization",bearer(childToken)).header("Idempotency-Key","junior-skip-started")
   .contentType(MediaType.APPLICATION_JSON).content(skipStarted)).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.revision").value(3)).andExpect(jsonPath("$.data.items.length()").value(2))
   .andExpect(jsonPath("$.data.items[0].assignmentId").value(third.toString()));
  mvc.perform(post(plan+"/move").header("Authorization",bearer(childToken)).header("Idempotency-Key","junior-stale")
   .contentType(MediaType.APPLICATION_JSON).content(move)).andExpect(status().isConflict());
  mvc.perform(get(plan.replace(parent.family.toString(),other.family.toString())).header("Authorization",bearer(other.token))).andExpect(status().isNotFound());
  String report="/api/v1/families/"+parent.family+"/children/"+child+"/junior-learning/report";
  mvc.perform(get(report).header("Authorization",bearer(parent.token))).andExpect(status().isOk())
   .andExpect(jsonPath("$.data.planRevision").value(3)).andExpect(jsonPath("$.data.subjects.length()").value(3))
   .andExpect(jsonPath("$.data.recordedLearningMinutes").value(0));
  mvc.perform(get(report).header("Authorization",bearer(childToken))).andExpect(status().isForbidden());
  org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM junior_learning_plan_action WHERE family_id=? AND child_id=?",Integer.class,parent.family,child)).isEqualTo(2);
 }
 @Test void concurrentMovesUseOnePlanRevisionAndOnlyOneCommits() throws Exception {
  Session parent=bootstrap("初中并发计划家庭","963852");UUID child=child(parent,"计划同学","2013-04-12");String token=childToken(parent,child);
  publish(parent,"并发语文","CHINESE","junior-race-1");publish(parent,"并发数学","MATH","junior-race-2");publish(parent,"并发物理","PHYSICS","junior-race-3");
  mvc.perform(post("/api/v1/families/"+parent.family+"/children/"+child+"/autonomous-learning/sync").header("Authorization",bearer(token)).header("Idempotency-Key","junior-race-sync")).andExpect(status().isOk());
  String path="/api/v1/families/"+parent.family+"/children/"+child+"/junior-learning/plan";
  JsonNode plan=data(mvc.perform(get(path).header("Authorization",bearer(token))).andExpect(status().isOk()).andReturn());
  UUID first=UUID.fromString(plan.path("items").get(0).path("assignmentId").asText()),third=UUID.fromString(plan.path("items").get(2).path("assignmentId").asText());
  var pool=Executors.newFixedThreadPool(2);
  try {
   Callable<Integer> down=()->mvc.perform(post(path+"/move").header("Authorization",bearer(token)).header("Idempotency-Key","race-down").contentType(MediaType.APPLICATION_JSON).content("{\"assignmentId\":\""+first+"\",\"direction\":\"DOWN\",\"expectedRevision\":1}")).andReturn().getResponse().getStatus();
   Callable<Integer> up=()->mvc.perform(post(path+"/move").header("Authorization",bearer(token)).header("Idempotency-Key","race-up").contentType(MediaType.APPLICATION_JSON).content("{\"assignmentId\":\""+third+"\",\"direction\":\"UP\",\"expectedRevision\":1}")).andReturn().getResponse().getStatus();
   var results=pool.invokeAll(java.util.List.of(down,up)).stream().map(future->{try{return future.get();}catch(Exception ex){throw new RuntimeException(ex);}}).toList();
   org.assertj.core.api.Assertions.assertThat(results).containsExactlyInAnyOrder(200,409);
  } finally { pool.shutdownNow(); }
  org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM junior_learning_plan_action WHERE family_id=? AND child_id=?",Integer.class,parent.family,child)).isEqualTo(1);
 }
 private void publish(Session p,String title,String subject,String key)throws Exception{String body="{\"schoolStage\":\"JUNIOR_MIDDLE\",\"subjectCode\":\""+subject+"\",\"title\":\""+title+"\",\"version\":{\"summary\":\"提出问题并寻找证据\",\"rightsBasis\":\"Family Growth 原创\",\"units\":[{\"title\":\"证据章节\",\"lessons\":[{\"title\":\"观察与解释\",\"summary\":\"一次不超过二十五分钟\",\"chapterTitle\":\"证据章节\",\"knowledgePoints\":[\"观察\",\"解释\"],\"learningGoal\":\"用一条观察支持自己的解释\",\"safetyNote\":\"只使用纸笔和日常安全物品\",\"activities\":[{\"type\":\"OFFLINE_PRACTICE\",\"title\":\"记录观察\",\"instruction\":\"用安全的日常材料观察并写下一条证据\",\"expectedMinutes\":12}]}]}]}}";JsonNode created=data(mvc.perform(post("/api/v1/families/"+p.family+"/teaching/courses").header("Authorization",bearer(p.token)).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn());mvc.perform(post("/api/v1/families/"+p.family+"/teaching/course-versions/"+created.path("versionId").asText()+"/publish").header("Authorization",bearer(p.token)).header("Idempotency-Key",key+"-publish")).andExpect(status().isOk());}
 private Session bootstrap(String name,String pin)throws Exception{JsonNode d=data(mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON).content("{\"familyName\":\""+name+"\",\"parentName\":\"家长\",\"pin\":\""+pin+"\"}")).andExpect(status().isCreated()).andReturn());return new Session(UUID.fromString(d.path("familyId").asText()),d.path("session").path("token").asText());}
 private UUID child(Session p,String name,String birth)throws Exception{return UUID.fromString(data(mvc.perform(post("/api/v1/families/"+p.family+"/children").header("Authorization",bearer(p.token)).contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\""+name+"\",\"birthDate\":\""+birth+"\",\"ageStage\":\"TEEN_13_PLUS\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText());}
 private String childToken(Session p,UUID child)throws Exception{return data(mvc.perform(post("/api/v1/auth/child-sessions").header("Authorization",bearer(p.token)).contentType(MediaType.APPLICATION_JSON).content("{\"childId\":\""+child+"\"}")).andExpect(status().isCreated()).andReturn()).path("token").asText();}
 private JsonNode data(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString()).path("data");}private static String bearer(String t){return "Bearer "+t;}private record Session(UUID family,String token){}
}
