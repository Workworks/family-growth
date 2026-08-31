package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.JsonNode;import com.fasterxml.jackson.databind.ObjectMapper;import java.util.UUID;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.http.MediaType;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.ActiveProfiles;import org.springframework.test.web.servlet.MockMvc;import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties={"spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage26usage;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}","spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}","spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}","spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc @ActiveProfiles("test") class Stage26UsageAccessApiTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
 @Test void quietHoursAndParentAllowanceAreServerAuthoritativeAuditedAndIdempotent()throws Exception{
  Session parent=bootstrap("Stage26家庭","681357"),other=bootstrap("其他26家庭","681358");
  UUID child=id(post("/api/v1/families/"+parent.family+"/children",parent.token,"{\"displayName\":\"休息孩子\",\"birthDate\":\"2015-01-01\",\"ageStage\":\"PRETEEN_10_12\"}",null,201));
  String childToken=text(post("/api/v1/auth/child-sessions",parent.token,"{\"childId\":\""+child+"\"}",null,201),"data","token");String base="/api/v1/families/"+parent.family+"/children/"+child;
  mvc.perform(put(base+"/usage-policy").header("Authorization",bearer(parent.token)).contentType(MediaType.APPLICATION_JSON).content("{\"zoneId\":\"Asia/Shanghai\",\"dailyLimitMinutes\":30,\"sessionLimitMinutes\":10,\"quietStart\":\"00:00:00\",\"quietEnd\":\"23:59:59\"}" )).andExpect(status().isOk()).andExpect(jsonPath("$.data.quietStart").value("00:00:00"));
  mvc.perform(get(base+"/usage-access").header("Authorization",bearer(childToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(false)).andExpect(jsonPath("$.data.reasonCode").value("QUIET_HOURS"));
  String body="{\"minutes\":15,\"reason\":\"今晚和家长一起完成学校布置的短任务\"}";MvcResult first=post(base+"/usage-allowances",parent.token,body,"allow-26",200);String allowance=text(first,"data","id");assertThat(text(post(base+"/usage-allowances",parent.token,body,"allow-26",200),"data","id")).isEqualTo(allowance);
  post(base+"/usage-allowances",parent.token,"{\"minutes\":20,\"reason\":\"不同请求\"}","allow-26",409);post(base+"/usage-allowances",childToken,body,"child-cannot-allow",403);post(base+"/usage-allowances",other.token,body,"cross-family",404);
  mvc.perform(get(base+"/usage-access").header("Authorization",bearer(childToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.allowed").value(true)).andExpect(jsonPath("$.data.reasonCode").value("TEMPORARY_ALLOWANCE"));
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usage_policy_action WHERE family_id=? AND child_id=?",Integer.class,parent.family,child)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usage_temporary_allowance WHERE family_id=? AND child_id=?",Integer.class,parent.family,child)).isEqualTo(1);
 }
 private Session bootstrap(String name,String pin)throws Exception{MvcResult r=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON).content("{\"familyName\":\""+name+"\",\"parentName\":\"家长\",\"pin\":\""+pin+"\"}")).andExpect(status().isCreated()).andReturn();return new Session(UUID.fromString(text(r,"data","familyId")),text(r,"data","session","token"));}
 private MvcResult post(String url,String token,String body,String key,int status)throws Exception{var r=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).header("Authorization",bearer(token));if(key!=null)r.header("Idempotency-Key",key);if(!body.isEmpty())r.contentType(MediaType.APPLICATION_JSON).content(body);return mvc.perform(r).andExpect(status().is(status)).andReturn();}
 private UUID id(MvcResult r)throws Exception{return UUID.fromString(text(r,"data","id"));}private String text(MvcResult r,String...path)throws Exception{JsonNode n=json.readTree(r.getResponse().getContentAsString());for(String p:path)n=n.path(p);return n.asText();}private static String bearer(String token){return "Bearer "+token;}private record Session(UUID family,String token){}
}
