package com.familygrowth.android.remote

import java.math.BigDecimal
import java.net.URI
import java.util.UUID

data class RemoteTask(val id:String,val title:String,val minutes:Int,val status:String,val completionId:String?)
data class RemoteExperienceProfile(val birthDate:String,val recommendedStage:String,val stageOverride:String?,val effectiveStage:String,
 val recommendedPrimaryBand:String?=null,val primaryBandOverride:String?=null,val effectivePrimaryBand:String?=null,
 val overrideReason:String,val hapticsEnabled:Boolean,val version:Long)
data class RemoteResourceCategory(val id:String,val title:String,val displayOrder:Int,val categoryUrl:String?=null)
data class RemoteEducationSource(val id:String,val title:String,val sourceUrl:String,val schoolStages:List<String>,val usageNote:String,val status:String,val refreshStatus:String,val refreshError:String,val lastRefreshedAt:String?,val categories:List<RemoteResourceCategory>)
data class RemoteChildEducationSource(val id:String,val title:String,val categories:List<RemoteResourceCategory>,val lastRefreshedAt:String?,val parentActionRequired:Boolean)
data class RemoteQuestionOption(val value:String,val label:String)
data class RemoteLearningActivity(val id:String,val type:String,val title:String,val instruction:String,val contentRef:String,val expectedMinutes:Int,val prompt:String,val hint:String,val options:List<RemoteQuestionOption>,val requiredEvidence:String,val evidence:Set<String>,val checkedCorrect:Boolean?) {
 fun childReady():Boolean = if(requiredEvidence=="PARENT_CONFIRMED") "ATTEMPTED" in evidence else requiredEvidence in evidence && (requiredEvidence!="CHECKED" || checkedCorrect==true)
}
object KindergartenActivityPolicy {
 const val MAX_EXPECTED_MINUTES = 8
 const val MAX_VISIBLE_CHOICES = 2
 fun renderIssue(activity:RemoteLearningActivity):String? = when {
  activity.title.isBlank() || activity.instruction.isBlank() -> "这一步还没有准备好，请家长看看。"
  activity.expectedMinutes !in 1..MAX_EXPECTED_MINUTES -> "这一步有点长，请家长换成短一点的活动。"
  activity.options.size > MAX_VISIBLE_CHOICES -> "这里的选择太多了，请家长帮忙。"
  else -> null
 }
}
data class RemoteRewardSnapshot(val money:String="0.00",val coin:Long=0,val xp:Long=0,val settledAt:String?=null)
data class RemoteSupportEvent(val id:String,val assignmentId:String,val activityId:String?,val type:String,
                              val category:String?,val childMessage:String,val privateNote:String,
                              val revisitAt:String?,val parentEventId:String?,val createdAt:String)
data class RemoteRewardPolicy(val money:String,val coin:Long,val xp:Long,val version:Long)
data class RemoteJuniorMetadata(val chapterTitle:String,val knowledgePoints:List<String>,val learningGoal:String,val safetyNote:String)
data class RemoteSeniorMetadata(val moduleType:String,val topicTitle:String,val inquiryQuestion:String,val expectedEvidence:String,val safetyNote:String)
data class RemoteSeniorModule(val subjectCode:String,val moduleType:String)
data class RemoteSeniorModuleConfiguration(val revision:Long,val selections:List<RemoteSeniorModule>)
data class RemoteSeniorGoal(val id:String,val assignmentId:String?,val module:RemoteSeniorModule,val weekStart:String,val title:String,val evidenceTarget:String,val nextAction:String,val status:String,val revision:Long)
data class RemoteSeniorReflection(val id:String,val goalId:String?,val assignmentId:String?,val evidenceSummary:String,val strategy:String,val nextAction:String,val supportRequested:Boolean,val createdAt:String)
data class RemoteSeniorLearningReport(val recordedLearningMinutes:Long,val subjects:List<RemoteSubjectLearningFacts>,val activeGoals:Long,val archivedGoals:Long,val reflections:Long,val supportRequests:Long)
data class RemoteUsagePolicy(val zoneId:String,val dailyLimitMinutes:Int,val sessionLimitMinutes:Int,val quietStart:String,val quietEnd:String,val version:Long)
data class RemoteUsageAccess(val allowed:Boolean,val reasonCode:String,val message:String,val usedTodayMinutes:Int,val dailyLimitMinutes:Int,val allowanceExpiresAt:String?,val sessionUsedMinutes:Int=0,val sessionLimitMinutes:Int=0,val restMinutes:Int=0,val restUntil:String?=null)
data class RemoteChildDataExport(val generatedAt:String,val json:String)
data class RemoteErasurePreview(val requestId:String,val confirmationToken:String,val confirmationExpiresAt:String,val deletedOrRedacted:List<String>,val retained:List<String>)
data class RemoteErasureResult(val requestId:String,val status:String,val completedAt:String)
data class RemoteStageTransitionPreview(val oldStage:String,val newStage:String,val willArchiveUnstartedAutonomous:Int,val willRestorePreviouslyArchived:Int,val message:String)
data class RemoteJuniorPlanItem(val assignmentId:String,val subjectCode:String,val courseTitle:String,val lessonTitle:String,val status:String,val position:Int)
data class RemoteJuniorPlan(val revision:Long,val items:List<RemoteJuniorPlanItem>)
data class RemoteJuniorLearningReport(val recordedLearningMinutes:Long,val subjects:List<RemoteSubjectLearningFacts>,val planRevision:Long)
data class RemoteSubjectLearningFacts(val subjectCode:String,val assigned:Long,val inProgress:Long,val submitted:Long,
 val completed:Long,val reworkRequired:Long,val openSupport:Long,val scheduledRevisits:Long,val dueRevisits:Long)
data class RemotePrimaryLearningReport(val effectiveStage:String,val effectivePrimaryBand:String?,
 val recordedLearningMinutes:Long,val subjects:List<RemoteSubjectLearningFacts>)
data class RemoteLearningAssignment(val id:String,val courseTitle:String,val unitTitle:String,val lessonTitle:String,val lessonSummary:String,val schoolStage:String,val subjectCode:String,val status:String,val version:Long,val activities:List<RemoteLearningActivity>,val reviewNote:String,val assignmentSource:String="PARENT",val reward:RemoteRewardSnapshot=RemoteRewardSnapshot(),val juniorMetadata:RemoteJuniorMetadata?=null,val seniorMetadata:RemoteSeniorMetadata?=null) {
 fun canSubmit():Boolean = status=="IN_PROGRESS" && activities.isNotEmpty() && activities.all(RemoteLearningActivity::childReady)
}
data class RemoteCourseSummary(val courseId:String,val title:String,val schoolStage:String,val subjectCode:String,val versionId:String,val versionNumber:Int,val status:String,val lessonCount:Int)
data class RemoteTeachingVersion(val courseId:String,val versionId:String,val title:String,val schoolStage:String,val subjectCode:String,val versionNumber:Int,val status:String,val lessonIds:List<String>)
data class RemoteSnapshot(val familyId:String,val childId:String,val childName:String,val money:BigDecimal,val coin:Int,val tasks:List<RemoteTask>,val pendingReviews:Int,val approvedToday:Int,val experience:RemoteExperienceProfile?=null)
data class RemoteSession(val baseUrl:String,val familyId:String,val parentId:String,val childId:String,val parentToken:String,val childToken:String)
sealed interface ConnectionState{data object Disconnected:ConnectionState;data object Connecting:ConnectionState;data class Connected(val snapshot:RemoteSnapshot):ConnectionState;data class Error(val message:String):ConnectionState;data object Expired:ConnectionState}
enum class RemoteFailureKind { RETRYABLE, CONFLICT, PERMANENT }
sealed interface RemoteResult<out T>{data class Ok<T>(val value:T):RemoteResult<T>;data object Unauthorized:RemoteResult<Nothing>;data class Failure(val message:String,val kind:RemoteFailureKind=RemoteFailureKind.PERMANENT):RemoteResult<Nothing>}

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
