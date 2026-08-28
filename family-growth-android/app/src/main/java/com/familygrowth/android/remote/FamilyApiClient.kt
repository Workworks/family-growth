package com.familygrowth.android.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import java.time.LocalDate
import com.familygrowth.android.core.SchoolStage

interface FamilyApiTransport{
 suspend fun login(base:String,family:String,parent:String,pin:String):RemoteResult<String>
 suspend fun childSession(base:String,parentToken:String,child:String):RemoteResult<String>
 suspend fun snapshot(base:String,token:String,family:String,child:String):RemoteResult<RemoteSnapshot>
 suspend fun experience(base:String,token:String,family:String,child:String):RemoteResult<RemoteExperienceProfile>
 suspend fun updateExperience(base:String,token:String,family:String,child:String,birthDate:LocalDate,stageOverride:SchoolStage?,overrideReason:String,hapticsEnabled:Boolean,expectedVersion:Long):RemoteResult<RemoteExperienceProfile>
 suspend fun submit(base:String,childToken:String,family:String,child:String,task:String,key:String):RemoteResult<Unit>
 suspend fun review(base:String,parentToken:String,family:String,completion:String,key:String):RemoteResult<Unit>
 suspend fun usage(base:String,childToken:String,family:String,child:String,key:String,occurredAt:Instant):RemoteResult<Unit>
}

class HttpFamilyApiTransport:FamilyApiTransport{
 override suspend fun login(base:String,family:String,parent:String,pin:String)=request(base,"/api/v1/auth/login","POST",null,JSONObject().put("familyId",family).put("parentId",parent).put("pin",pin)){it.getJSONObject("data").getString("token")}
 override suspend fun childSession(base:String,parentToken:String,child:String)=request(base,"/api/v1/auth/child-sessions","POST",parentToken,JSONObject().put("childId",child)){it.getJSONObject("data").getString("token")}
 override suspend fun snapshot(base:String,token:String,family:String,child:String)=request(base,"/api/v1/families/$family/children/$child/sync","GET",token,null){root->val d=root.getJSONObject("data");val w=d.getJSONObject("wallet");val a=d.getJSONArray("tasks");RemoteSnapshot(d.getString("familyId"),d.getString("childId"),d.getString("childName"),w.getString("moneyBalance").toBigDecimal(),w.getInt("coinBalance"),buildList{for(i in 0 until a.length()){val t=a.getJSONObject(i);add(RemoteTask(t.getString("id"),t.getString("title"),t.getInt("expectedMinutes"),t.getString("status"),if(t.isNull("latestCompletionId"))null else t.getString("latestCompletionId"))) }},d.getInt("pendingReviews"),d.getInt("approvedToday"))}
 override suspend fun experience(base:String,token:String,family:String,child:String)=request(base,"/api/v1/families/$family/children/$child/experience-profile","GET",token,null){decodeExperience(it.getJSONObject("data"))}
 override suspend fun updateExperience(base:String,token:String,family:String,child:String,birthDate:LocalDate,stageOverride:SchoolStage?,overrideReason:String,hapticsEnabled:Boolean,expectedVersion:Long)=request(base,"/api/v1/families/$family/children/$child/experience-profile","PUT",token,JSONObject().put("birthDate",birthDate.toString()).put("stageOverride",stageOverride?.name ?: JSONObject.NULL).put("overrideReason",overrideReason).put("hapticsEnabled",hapticsEnabled).put("expectedVersion",expectedVersion).put("auditReason","家长在 Android 端更新学习阶段")){decodeExperience(it.getJSONObject("data"))}
 override suspend fun submit(base:String,childToken:String,family:String,child:String,task:String,key:String)=request(base,"/api/v1/families/$family/children/$child/tasks/$task/completions","POST",childToken,JSONObject().put("evidenceNote","在孩子端确认完成"),key){Unit}
 override suspend fun review(base:String,parentToken:String,family:String,completion:String,key:String)=request(base,"/api/v1/families/$family/completions/$completion/review","POST",parentToken,JSONObject().put("approved",true).put("xpReward",10).put("coinReward",10).put("moneyReward",0).put("reviewNote","家长端确认"),key){Unit}
 override suspend fun usage(base:String,childToken:String,family:String,child:String,key:String,occurredAt:Instant)=request(base,"/api/v1/families/$family/children/$child/usage-events","POST",childToken,JSONObject().put("type","APP_ACTIVE").put("minutes",1).put("occurredAt",occurredAt.toString()),key){Unit}
 private suspend fun <T> request(base:String,path:String,method:String,token:String?,body:JSONObject?,key:String?=null,decode:(JSONObject)->T):RemoteResult<T> = withContext(Dispatchers.IO){
  try{val connection=(URL(base+path).openConnection() as HttpURLConnection).apply{requestMethod=method;connectTimeout=8_000;readTimeout=10_000;setRequestProperty("Accept","application/json");if(token!=null)setRequestProperty("Authorization","Bearer $token");if(key!=null)setRequestProperty("Idempotency-Key",key);if(body!=null){doOutput=true;setRequestProperty("Content-Type","application/json");outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}}};val code=connection.responseCode;if(code==401){connection.disconnect();return@withContext RemoteResult.Unauthorized};val stream=if(code in 200..299)connection.inputStream else connection.errorStream;val text=stream?.bufferedReader()?.use{it.readText()}.orEmpty();connection.disconnect();if(code !in 200..299)return@withContext RemoteResult.Failure(if(code==403)"当前角色无权执行" else if(code==409)"数据已变化，请刷新后重试" else "服务请求失败（$code）");RemoteResult.Ok(decode(JSONObject(text)))}catch(_:Exception){RemoteResult.Failure("无法连接家庭服务，请检查地址、证书和网络")}
 }
 private fun decodeExperience(d:JSONObject)=RemoteExperienceProfile(d.getString("birthDate"),d.getString("recommendedStage"),if(d.isNull("stageOverride"))null else d.getString("stageOverride"),d.getString("effectiveStage"),d.optString("overrideReason"),d.getBoolean("hapticsEnabled"),d.getLong("version"))
}

class RemoteFamilyRepository(private val transport:FamilyApiTransport,private val sessions:MemorySessionStore,private val allowPrivateHttp:Boolean){
 suspend fun connect(rawBase:String,family:String,parent:String,child:String,pin:String):RemoteResult<RemoteSnapshot>{val base=ServiceUrlPolicy.normalize(rawBase,allowPrivateHttp).getOrElse{return RemoteResult.Failure(it.message?:"服务地址无效")};runCatching{validateConnectionIds(family,parent,child)}.getOrElse{return RemoteResult.Failure("家庭、家长或孩子 ID 格式无效")};val parentToken=when(val r=transport.login(base,family,parent,pin)){is RemoteResult.Ok->r.value;RemoteResult.Unauthorized->return RemoteResult.Unauthorized;is RemoteResult.Failure->return r};val childToken=when(val r=transport.childSession(base,parentToken,child)){is RemoteResult.Ok->r.value;RemoteResult.Unauthorized->return expired();is RemoteResult.Failure->return r};val session=RemoteSession(base,family,parent,child,parentToken,childToken);sessions.set(session);return refresh()}
 suspend fun refresh():RemoteResult<RemoteSnapshot>{val s=sessions.get()?:return RemoteResult.Failure("尚未连接家庭服务");val snapshot=when(val r=transport.snapshot(s.baseUrl,s.parentToken,s.familyId,s.childId)){is RemoteResult.Ok->r.value;RemoteResult.Unauthorized->return expired();is RemoteResult.Failure->return r};return when(val p=transport.experience(s.baseUrl,s.parentToken,s.familyId,s.childId)){is RemoteResult.Ok->RemoteResult.Ok(snapshot.copy(experience=p.value));RemoteResult.Unauthorized->expired();is RemoteResult.Failure->p}}
 suspend fun updateExperience(birthDate:LocalDate,stageOverride:SchoolStage?,overrideReason:String,hapticsEnabled:Boolean,expectedVersion:Long):RemoteResult<RemoteSnapshot>{val s=sessions.get()?:return RemoteResult.Failure("尚未连接家庭服务");return when(val r=transport.updateExperience(s.baseUrl,s.parentToken,s.familyId,s.childId,birthDate,stageOverride,overrideReason,hapticsEnabled,expectedVersion)){is RemoteResult.Ok->refresh();RemoteResult.Unauthorized->expired();is RemoteResult.Failure->r}}
 suspend fun submitTask(taskId:String):RemoteResult<RemoteSnapshot>{val s=sessions.get()?:return RemoteResult.Failure("尚未连接家庭服务");return when(val r=transport.submit(s.baseUrl,s.childToken,s.familyId,s.childId,taskId,UUID.randomUUID().toString())){is RemoteResult.Ok->refresh();RemoteResult.Unauthorized->expired();is RemoteResult.Failure->r}}
 suspend fun approveTask(completionId:String):RemoteResult<RemoteSnapshot>{val s=sessions.get()?:return RemoteResult.Failure("尚未连接家庭服务");return when(val r=transport.review(s.baseUrl,s.parentToken,s.familyId,completionId,UUID.randomUUID().toString())){is RemoteResult.Ok->refresh();RemoteResult.Unauthorized->expired();is RemoteResult.Failure->r}}
 suspend fun recordUsage(key:String,occurredAt:Instant):RemoteResult<Unit>{val s=sessions.get()?:return RemoteResult.Failure("尚未连接家庭服务");return when(val r=transport.usage(s.baseUrl,s.childToken,s.familyId,s.childId,key,occurredAt)){RemoteResult.Unauthorized->expired();else->r}}
 fun disconnect(){sessions.clear()} fun hasSession()=sessions.get()!=null
 private fun <T> expired():RemoteResult<T>{sessions.clear();return RemoteResult.Unauthorized}
}
