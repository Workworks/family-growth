package com.familygrowth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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

@SpringBootTest(properties={
    "spring.datasource.url=${FAMILY_GROWTH_TEST_POSTGRES_URL:jdbc:h2:mem:stage6api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${FAMILY_GROWTH_TEST_POSTGRES_USER:sa}",
    "spring.datasource.password=${FAMILY_GROWTH_TEST_POSTGRES_PASSWORD:}","spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class Stage6ApiTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Test void rewardSavingAndWishLoopsPreserveBalancesAndAuthorization()throws Exception{
        Session parent=bootstrap(); UUID child=id(mvc.perform(post("/api/v1/families/"+parent.family+"/children")
            .header("Authorization",bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"小松\",\"birthDate\":\"2021-01-01\",\"ageStage\":\"EARLY_CHILDHOOD_2_5\"}"))
            .andExpect(status().isCreated()).andReturn());
        String childToken=text(mvc.perform(post("/api/v1/auth/child-sessions").header("Authorization",bearer(parent.token))
            .contentType(MediaType.APPLICATION_JSON).content("{\"childId\":\""+child+"\"}"))
            .andExpect(status().isCreated()).andReturn(),"data","token");
        adjust(parent,child,"MONEY",100,"stage6-money"); adjust(parent,child,"COIN",100,"stage6-coin");

        String products="/api/v1/families/"+parent.family+"/reward-products";
        UUID product=id(mvc.perform(post(products).header("Authorization",bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"一起去公园\",\"coinCost\":30,\"stockCount\":3,\"active\":true}"))
            .andExpect(status().isCreated()).andReturn());
        UUID disabled=id(mvc.perform(post(products).header("Authorization",bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"暂不可用\",\"coinCost\":10,\"stockCount\":1,\"active\":false}"))
            .andExpect(status().isCreated()).andReturn());
        UUID expensive=id(mvc.perform(post(products).header("Authorization",bearer(parent.token)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"长期目标\",\"coinCost\":1000,\"stockCount\":1,\"active\":true}"))
            .andExpect(status().isCreated()).andReturn());
        mvc.perform(get(products).header("Authorization",bearer(childToken))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        String orders="/api/v1/families/"+parent.family+"/children/"+child+"/reward-orders";
        mvc.perform(post(orders).header("Authorization",bearer(childToken)).header("Idempotency-Key","order-disabled")
            .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\""+disabled+"\"}"))
            .andExpect(status().isConflict());
        UUID insufficient=createOrder(parent,child,childToken,orders,expensive,"order-insufficient");
        mvc.perform(post("/api/v1/families/"+parent.family+"/reward-orders/"+insufficient+"/review")
            .header("Authorization",bearer(parent.token)).header("Idempotency-Key","review-insufficient")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
            .andExpect(status().isConflict());
        UUID approved=id(mvc.perform(post(orders).header("Authorization",bearer(childToken)).header("Idempotency-Key","order-approve")
            .contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\""+product+"\"}"))
            .andExpect(status().isCreated()).andReturn());
        String review="/api/v1/families/"+parent.family+"/reward-orders/"+approved+"/review";
        mvc.perform(post(review).header("Authorization",bearer(childToken)).header("Idempotency-Key","review-denied")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andExpect(status().isForbidden());
        mvc.perform(post(review).header("Authorization",bearer(parent.token)).header("Idempotency-Key","review-approved")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("APPROVED"));
        mvc.perform(post(review).header("Authorization",bearer(parent.token)).header("Idempotency-Key","review-approved")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andExpect(status().isOk());
        mvc.perform(post(review).header("Authorization",bearer(parent.token)).header("Idempotency-Key","review-second")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andExpect(status().isConflict());

        UUID rejected=createOrder(parent,child,childToken,orders,product,"order-reject");
        mvc.perform(post("/api/v1/families/"+parent.family+"/reward-orders/"+rejected+"/review")
            .header("Authorization",bearer(parent.token)).header("Idempotency-Key","review-reject")
            .contentType(MediaType.APPLICATION_JSON).content("{\"approved\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("REJECTED"));
        UUID canceled=createOrder(parent,child,childToken,orders,product,"order-cancel");
        mvc.perform(post("/api/v1/families/"+parent.family+"/reward-orders/"+canceled+"/cancel")
            .header("Authorization",bearer(childToken)).header("Idempotency-Key","cancel-1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELED"));
        mvc.perform(get("/api/v1/families/"+parent.family+"/children/"+child+"/wallet").header("Authorization",bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.coinBalance").value(70));
        UUID raced=createOrder(parent,child,childToken,orders,product,"order-race");
        String racedReview="/api/v1/families/"+parent.family+"/reward-orders/"+raced+"/review";
        var pool=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try{
            var first=pool.submit(()->{start.await();return reviewStatus(racedReview,parent.token,"race-a");});
            var second=pool.submit(()->{start.await();return reviewStatus(racedReview,parent.token,"race-b");});
            start.countDown(); assertThat(Set.of(first.get(),second.get())).containsExactlyInAnyOrder(200,409);
        }finally{pool.shutdownNow();}

        String transfers="/api/v1/families/"+parent.family+"/children/"+child+"/saving/transfers";
        mvc.perform(post(transfers).header("Authorization",bearer(childToken)).header("Idempotency-Key","save-20")
            .contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"DEPOSIT\",\"amount\":20.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.savingAfter").value(20.00));
        mvc.perform(post(transfers).header("Authorization",bearer(childToken)).header("Idempotency-Key","save-20")
            .contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"DEPOSIT\",\"amount\":20.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.walletAfter").value(80.00));
        mvc.perform(post(transfers).header("Authorization",bearer(childToken)).header("Idempotency-Key","withdraw-5")
            .contentType(MediaType.APPLICATION_JSON).content("{\"direction\":\"WITHDRAW\",\"amount\":5.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.savingAfter").value(15.00));

        UUID wish=id(mvc.perform(post("/api/v1/families/"+parent.family+"/children/"+child+"/wishes")
            .header("Authorization",bearer(childToken)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"大积木\",\"targetAmount\":50.00}")) .andExpect(status().isCreated()).andReturn());
        String allocation="/api/v1/families/"+parent.family+"/wishes/"+wish+"/allocation";
        mvc.perform(post(allocation).header("Authorization",bearer(childToken)).header("Idempotency-Key","wish-10")
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":10.00}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.progressPercent").value(20.00));
        mvc.perform(post(allocation).header("Authorization",bearer(childToken)).header("Idempotency-Key","wish-over")
            .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":20.00}")) .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/families/"+parent.family+"/children/"+child+"/saving").header("Authorization",bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.balance").value(15.00));
        mvc.perform(get("/api/v1/families/"+parent.family+"/children/"+child+"/wallet").header("Authorization",bearer(parent.token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.moneyBalance").value(85.00));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry WHERE family_id=? AND entry_type='REWARD_ORDER'",Integer.class,parent.family)).isEqualTo(2);
    }
    private UUID createOrder(Session p,UUID child,String childToken,String url,UUID product,String key)throws Exception{return id(mvc.perform(post(url).header("Authorization",bearer(childToken)).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"productId\":\""+product+"\"}")) .andExpect(status().isCreated()).andReturn());}
    private int reviewStatus(String url,String token,String key)throws Exception{return mvc.perform(post(url).header("Authorization",bearer(token)).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true}")) .andReturn().getResponse().getStatus();}
    private void adjust(Session p,UUID child,String asset,int amount,String key)throws Exception{mvc.perform(post("/api/v1/families/"+p.family+"/children/"+child+"/wallet/adjustments").header("Authorization",bearer(p.token)).header("Idempotency-Key",key).contentType(MediaType.APPLICATION_JSON).content("{\"assetType\":\""+asset+"\",\"delta\":"+amount+",\"reason\":\"Stage6 fixture\"}")) .andExpect(status().isOk());}
    private Session bootstrap()throws Exception{MvcResult r=mvc.perform(post("/api/v1/auth/bootstrap").contentType(MediaType.APPLICATION_JSON).content("{\"familyName\":\"Stage6家庭\",\"parentName\":\"家长\",\"pin\":\"246802\"}")) .andExpect(status().isCreated()).andReturn();return new Session(UUID.fromString(text(r,"data","familyId")),text(r,"data","session","token"));}
    private UUID id(MvcResult r)throws Exception{return UUID.fromString(text(r,"data","id"));} private String text(MvcResult r,String...path)throws Exception{JsonNode n=json.readTree(r.getResponse().getContentAsString());for(String p:path)n=n.path(p);return n.asText();} private static String bearer(String t){return "Bearer "+t;} private record Session(UUID family,String token){}
}
