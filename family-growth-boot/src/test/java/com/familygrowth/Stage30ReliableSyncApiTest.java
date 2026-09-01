package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

@SpringBootTest(properties={"spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage30sync;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}","spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}","spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}","spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc @ActiveProfiles("test") class Stage30ReliableSyncApiTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;
 @Test void deltaUsesStableDigestsTombstonesIdempotencyAndFamilyScope()throws Exception{
  Session one=bootstrap("同步家庭一","713001"),two=bootstrap("同步家庭二","713002");UUID child=child(one,"小云"),other=child(two,"小海");
  JsonNode first=sync(one,child,"client-tablet-01",0,Map.of("removed:old","0".repeat(64)),"sync-1",200);
  assertThat(first.path("cursor").asLong()).isPositive();assertThat(first.path("changed").size()).isGreaterThanOrEqualTo(2);assertThat(first.path("tombstones").get(0).asText()).isEqualTo("removed:old");
  Map<String,String> known=new TreeMap<>();first.path("changed").forEach(f->known.put(f.path("key").asText(),f.path("digest").asText()));
  JsonNode replay=sync(one,child,"client-tablet-01",0,Map.of("removed:old","0".repeat(64)),"sync-1",200);assertThat(replay.path("cursor").asLong()).isEqualTo(first.path("cursor").asLong());
  JsonNode unchanged=sync(one,child,"client-tablet-01",first.path("cursor").asLong(),known,"sync-2",200);assertThat(unchanged.path("changed")).isEmpty();assertThat(unchanged.path("cursor").asLong()).isGreaterThan(first.path("cursor").asLong());
  sync(two,child,"client-tablet-02",0,Map.of(),"wrong-family",404);sync(one,other,"client-tablet-03",0,Map.of(),"wrong-child",404);
 }
 private JsonNode sync(Session s,UUID child,String client,long cursor,Map<String,String> known,String key,int status)throws Exception{ObjectNode body=json.createObjectNode().put("clientId",client).put("afterCursor",cursor);body.set("knownDigests",json.valueToTree(known));MvcResult r=mvc.perform(post("/api/v1/families/"+s.family+"/children/"+child+"/sync/delta").header("Authorization","Bearer "+s.token).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content(body.toString())).andExpect(status().is(status)).andReturn();return status==200?json.readTree(r.getResponse().getContentAsString()).path("data"):json.createObjectNode();}
 private Session bootstrap(String name,String pin)throws Exception{JsonNode d=data(mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON).content("{\"familyName\":\""+name+"\",\"parentName\":\"家长\",\"pin\":\""+pin+"\"}")).andExpect(status().isCreated()).andReturn());return new Session(UUID.fromString(d.path("familyId").asText()),d.path("session").path("token").asText());}
 private UUID child(Session s,String name)throws Exception{return UUID.fromString(data(mvc.perform(post("/api/v1/families/"+s.family+"/children").header("Authorization","Bearer "+s.token).contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\""+name+"\",\"birthDate\":\"2018-01-01\",\"ageStage\":\"CHILD_6_9\"}")).andExpect(status().isCreated()).andReturn()).path("id").asText());}
 private JsonNode data(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString()).path("data");}private record Session(UUID family,String token){}
}
