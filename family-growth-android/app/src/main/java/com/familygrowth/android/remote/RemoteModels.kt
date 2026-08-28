package com.familygrowth.android.remote

import java.math.BigDecimal
import java.net.URI
import java.util.UUID

data class RemoteTask(val id:String,val title:String,val minutes:Int,val status:String,val completionId:String?)
data class RemoteExperienceProfile(val birthDate:String,val recommendedStage:String,val stageOverride:String?,val effectiveStage:String,val overrideReason:String,val hapticsEnabled:Boolean,val version:Long)
data class RemoteSnapshot(val familyId:String,val childId:String,val childName:String,val money:BigDecimal,val coin:Int,val tasks:List<RemoteTask>,val pendingReviews:Int,val approvedToday:Int,val experience:RemoteExperienceProfile?=null)
data class RemoteSession(val baseUrl:String,val familyId:String,val parentId:String,val childId:String,val parentToken:String,val childToken:String)
sealed interface ConnectionState{data object Disconnected:ConnectionState;data object Connecting:ConnectionState;data class Connected(val snapshot:RemoteSnapshot):ConnectionState;data class Error(val message:String):ConnectionState;data object Expired:ConnectionState}
sealed interface RemoteResult<out T>{data class Ok<T>(val value:T):RemoteResult<T>;data object Unauthorized:RemoteResult<Nothing>;data class Failure(val message:String):RemoteResult<Nothing>}

object ServiceUrlPolicy{
 fun normalize(raw:String,allowPrivateHttp:Boolean):Result<String> = runCatching{
  val uri=URI(raw.trim());require(uri.scheme.equals("https",true)||uri.scheme.equals("http",true)){"服务地址必须使用 HTTPS"};require(uri.userInfo==null&&uri.query==null&&uri.fragment==null){"服务地址不能包含凭据、参数或片段"};require(!uri.host.isNullOrBlank()){"服务地址缺少主机"};require(uri.path.isNullOrBlank()||uri.path=="/"){"服务地址只填写协议、主机和端口"}
  if(uri.scheme.equals("http",true)){require(allowPrivateHttp&&isPrivateLiteral(uri.host)){"HTTP 仅限开发构建的 loopback/私网字面地址"}}
  URI(uri.scheme.lowercase(),null,uri.host.lowercase(),uri.port,null,null,null).toString().removeSuffix("/")
 }
 private fun isPrivateLiteral(host:String):Boolean{
  val h=host.lowercase();if(h=="localhost"||h=="127.0.0.1"||h=="::1")return true
  val p=h.split('.').mapNotNull(String::toIntOrNull);if(p.size==4&&p.all{it in 0..255})return p[0]==10||(p[0]==172&&p[1] in 16..31)||(p[0]==192&&p[1]==168)||(p[0]==127)
  return ':' in h&&(h.startsWith("fc")||h.startsWith("fd")||h.startsWith("fe80"))
 }
}

class MemorySessionStore{private var value:RemoteSession?=null;fun get():RemoteSession?=value;fun set(session:RemoteSession){value=session};fun clear(){value=null}}

fun validateConnectionIds(family:String,parent:String,child:String){UUID.fromString(family);UUID.fromString(parent);UUID.fromString(child)}
