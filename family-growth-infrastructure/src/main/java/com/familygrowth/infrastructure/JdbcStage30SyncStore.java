package com.familygrowth.infrastructure;

import static com.familygrowth.domain.Stage30SyncModels.*;
import com.familygrowth.application.Stage30SyncService;
import com.familygrowth.application.Stage30SyncStore;
import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository class JdbcStage30SyncStore implements Stage30SyncStore {
 private final JdbcTemplate jdbc; JdbcStage30SyncStore(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public List<SyncFact> facts(UUID family,UUID child,UUID actor,boolean parent){
  List<SyncFact> out=new ArrayList<>();
  jdbc.query("SELECT display_name,birth_date,age_stage FROM child_profile WHERE family_id=? AND id=?",r->{fact(out,"child:"+child,map("id",child,"displayName",r.getString(1),"birthDate",String.valueOf(r.getObject(2)),"ageStage",r.getString(3)));},family,child);
  jdbc.query("SELECT money_balance,reserved_money,coin_balance,version FROM wallet WHERE family_id=? AND child_id=?",r->{fact(out,"wallet:"+child,map("money",decimal(r.getBigDecimal(1)),"reservedMoney",decimal(r.getBigDecimal(2)),"coin",r.getLong(3),"version",r.getLong(4)));},family,child);
  jdbc.query("SELECT t.id,t.title,t.expected_minutes,t.active,COALESCE((SELECT tc.status FROM task_completion tc WHERE tc.family_id=t.family_id AND tc.child_id=? AND tc.task_id=t.id ORDER BY tc.submitted_at DESC LIMIT 1),'TODO') FROM growth_task t JOIN growth_goal g ON g.id=t.goal_id JOIN growth_plan p ON p.id=g.plan_id WHERE t.family_id=? AND p.child_id=?",r->{UUID id=uuid(r,1);fact(out,"task:"+id,map("id",id,"title",r.getString(2),"expectedMinutes",r.getInt(3),"active",r.getBoolean(4),"status",r.getString(5)));},child,family,child);
  jdbc.query("SELECT id,lesson_id,status,version,updated_at FROM lesson_assignment WHERE family_id=? AND child_id=?",r->{UUID id=uuid(r,1);fact(out,"assignment:"+id,map("id",id,"lessonId",uuid(r,2),"status",r.getString(3),"version",r.getLong(4),"updatedAt",r.getTimestamp(5).toInstant().toString()));},family,child);
  jdbc.query("SELECT zone_id,daily_limit_minutes,session_limit_minutes,quiet_start,quiet_end,rest_minutes,version FROM usage_policy WHERE family_id=? AND child_id=?",r->{fact(out,"usage-policy:"+child,map("zoneId",r.getString(1),"dailyLimitMinutes",r.getInt(2),"sessionLimitMinutes",r.getInt(3),"quietStart",String.valueOf(r.getObject(4)),"quietEnd",String.valueOf(r.getObject(5)),"restMinutes",r.getInt(6),"version",r.getLong(7)));},family,child);
  if(parent)jdbc.query("SELECT id,notification_type,title,body,status,created_at FROM family_notification WHERE family_id=? AND recipient_actor_id=? AND (child_id=? OR child_id IS NULL)",r->{UUID id=uuid(r,1);fact(out,"notification:"+id,map("id",id,"type",r.getString(2),"title",r.getString(3),"body",r.getString(4),"status",r.getString(5),"createdAt",r.getTimestamp(6).toInstant().toString()));},family,actor,child);
  return out;
 }
 @Override public Optional<SyncCheckpoint> replay(UUID f,UUID a,String client,String key){return jdbc.query("SELECT cursor,request_hash,projection_hash FROM family_sync_checkpoint WHERE family_id=? AND actor_id=? AND client_id=? AND idempotency_key=?",(r,n)->new SyncCheckpoint(r.getLong(1),r.getString(2),r.getString(3)),f,a,client,key).stream().findFirst();}
 @Override public long checkpoint(UUID f,UUID c,UUID a,String client,String key,String request,String projection,Instant now){jdbc.update("INSERT INTO family_sync_checkpoint(family_id,child_id,actor_id,client_id,idempotency_key,request_hash,projection_hash,created_at) VALUES(?,?,?,?,?,?,?,?)",f,c,a,client,key,request,projection,Timestamp.from(now));return jdbc.queryForObject("SELECT cursor FROM family_sync_checkpoint WHERE family_id=? AND actor_id=? AND client_id=? AND idempotency_key=?",Long.class,f,a,client,key);}
 private static void fact(List<SyncFact> out,String key,Map<String,Object> payload){String canonical=payload.entrySet().stream().map(e->e.getKey()+"="+String.valueOf(e.getValue())).reduce((a,b)->a+"|"+b).orElse("");out.add(new SyncFact(key,Stage30SyncService.digest(canonical),payload));}
 private static Map<String,Object> map(Object... values){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)out.put((String)values[i],values[i+1]);return out;}
 private static UUID uuid(ResultSet r,int i)throws SQLException{Object v=r.getObject(i);return v instanceof UUID u?u:UUID.fromString(String.valueOf(v));}
 private static String decimal(BigDecimal value){return value.stripTrailingZeros().toPlainString();}
}
