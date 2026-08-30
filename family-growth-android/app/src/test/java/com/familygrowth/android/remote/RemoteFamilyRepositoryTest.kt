package com.familygrowth.android.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import com.familygrowth.android.core.SchoolStage
import com.familygrowth.android.core.PrimaryGradeBand

class RemoteFamilyRepositoryTest {
    @Test fun urlPolicyAllowsHttpsAndDevelopmentPrivateHttpOnly() {
        assertEquals("https://family.example:8443", ServiceUrlPolicy.normalize("https://family.example:8443/", false).getOrThrow())
        assertTrue(ServiceUrlPolicy.normalize("http://192.168.1.20:8080", true).isSuccess)
        assertTrue(ServiceUrlPolicy.normalize("http://192.168.1.20:8080", false).isFailure)
        assertTrue(ServiceUrlPolicy.normalize("http://example.com", true).isFailure)
        assertTrue(ServiceUrlPolicy.normalize("https://user:pass@example.com", false).isFailure)
    }

    @Test fun sessionIsMemoryOnlyAndUnauthorizedClearsIt() = runBlocking {
        val store = MemorySessionStore()
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, store, false)
        val connected = repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456")
        assertTrue(connected is RemoteResult.Ok)
        assertTrue(repository.hasSession())
        transport.expire = true
        assertSame(RemoteResult.Unauthorized, repository.refresh())
        assertFalse(repository.hasSession())
    }

    @Test fun usageRetryKeepsCallerIdempotencyKey() = runBlocking {
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, MemorySessionStore(), false)
        assertTrue(repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456") is RemoteResult.Ok)
        val occurredAt = Instant.parse("2026-08-25T10:00:00Z")
        assertTrue(repository.recordUsage("stable-event-key", occurredAt) is RemoteResult.Ok)
        assertTrue(repository.recordUsage("stable-event-key", occurredAt) is RemoteResult.Ok)
        assertEquals(listOf("stable-event-key", "stable-event-key"), transport.usageKeys)
    }

    @Test fun parentStageUpdateRefreshesServerExperience() = runBlocking {
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, MemorySessionStore(), false)
        assertTrue(repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456") is RemoteResult.Ok)
        val result = repository.updateExperience(LocalDate.of(2013, 8, 26), SchoolStage.JUNIOR_MIDDLE, null, "实际入学阶段", false, 0)
        assertTrue(result is RemoteResult.Ok)
        assertEquals(SchoolStage.JUNIOR_MIDDLE, transport.updatedStage)
        assertFalse(transport.updatedHaptics)
    }

    @Test fun educationSourceActionsUseParentSessionAndChildCatalogUsesChildSession() = runBlocking {
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, MemorySessionStore(), false)
        assertTrue(repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456") is RemoteResult.Ok)
        assertTrue(repository.createEducationSource("公益课堂", "https://learn.example.org", listOf(SchoolStage.PRIMARY), "免费浏览") is RemoteResult.Ok)
        assertTrue(repository.educationSourceAction("source-1", "refresh") is RemoteResult.Ok)
        assertTrue(repository.childEducationCatalog() is RemoteResult.Ok)
        assertEquals(listOf("create", "refresh", "child-catalog"), transport.resourceCalls)
    }

    @Test fun learningOutboxExecutionKeepsOriginalKeyAndClassifiesConflicts() = runBlocking {
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, MemorySessionStore(), false)
        assertTrue(repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456") is RemoteResult.Ok)
        val action = PendingLearningAction(idempotencyKey="persisted-key", familyId=FAMILY, childId=CHILD,
            type=LearningActionType.ATTEMPT, assignmentId="assignment", activityId="activity", responseText="完成")
        assertTrue(repository.executeLearning(action) is RemoteResult.Ok)
        assertEquals("persisted-key", transport.learningKey)
        assertEquals("child-token", transport.learningToken)
        assertEquals(RemoteFailureKind.CONFLICT, remoteFailureForStatus(409).kind)
        assertEquals(RemoteFailureKind.RETRYABLE, remoteFailureForStatus(429).kind)
        assertEquals(RemoteFailureKind.RETRYABLE, remoteFailureForStatus(503).kind)
        assertEquals(RemoteFailureKind.PERMANENT, remoteFailureForStatus(400).kind)
    }

    @Test fun parentTeachingStudioUsesParentTokenAndResolvesLessonBeforeAssignment() = runBlocking {
        val transport = FakeTransport()
        val repository = RemoteFamilyRepository(transport, MemorySessionStore(), false)
        assertTrue(repository.connect("https://family.example", FAMILY, PARENT, CHILD, "123456") is RemoteResult.Ok)
        val draft = RemoteTeachingCourseDraft(stage="KINDERGARTEN",courseTitle="自然课",
            lessonTitle="找叶子",lessonSummary="去户外观察",activityType="OFFLINE_PRACTICE",
            activityTitle="找一找",instruction="找到三种叶子",expectedMinutes=6,
            rightsBasis="Family Growth 原创亲子活动 · KG-PACK-1.0.0",
            kindergartenAgeBand="SHARED_3_4",kindergartenDomains=listOf("SCIENCE"))
        assertTrue(repository.createTeachingCourse(draft) is RemoteResult.Ok)
        assertEquals(draft, transport.teachingDraft)
        assertTrue(repository.publishTeachingVersion("version") is RemoteResult.Ok)
        assertTrue(repository.assignTeachingVersion("version") is RemoteResult.Ok)
        assertEquals(listOf("create:parent-token","publish:parent-token","detail:parent-token","assign:parent-token"), transport.teachingCalls)
    }

    private class FakeTransport : FamilyApiTransport {
        var expire = false
        val usageKeys = mutableListOf<String>()
        var updatedStage: SchoolStage? = null
        var updatedHaptics = true
        val resourceCalls = mutableListOf<String>()
        var learningKey = ""
        var learningToken = ""
        val teachingCalls = mutableListOf<String>()
        var teachingDraft: RemoteTeachingCourseDraft? = null
        override suspend fun login(base:String,family:String,parent:String,pin:String)=RemoteResult.Ok("parent-token")
        override suspend fun childSession(base:String,parentToken:String,child:String)=RemoteResult.Ok("child-token")
        override suspend fun snapshot(base:String,token:String,family:String,child:String):RemoteResult<RemoteSnapshot> = if(expire) RemoteResult.Unauthorized else RemoteResult.Ok(RemoteSnapshot(family,child,"小树",BigDecimal("12.00"),30,emptyList(),0,0))
        override suspend fun experience(base:String,token:String,family:String,child:String):RemoteResult<RemoteExperienceProfile> = if(expire) RemoteResult.Unauthorized else RemoteResult.Ok(RemoteExperienceProfile("2022-08-26","KINDERGARTEN",null,"KINDERGARTEN",overrideReason="",hapticsEnabled=true,version=0))
        override suspend fun updateExperience(base:String,token:String,family:String,child:String,birthDate:LocalDate,stageOverride:SchoolStage?,primaryBandOverride:PrimaryGradeBand?,overrideReason:String,hapticsEnabled:Boolean,expectedVersion:Long):RemoteResult<RemoteExperienceProfile>{updatedStage=stageOverride;updatedHaptics=hapticsEnabled;return RemoteResult.Ok(RemoteExperienceProfile(birthDate.toString(),"PRIMARY",stageOverride?.name,stageOverride?.name?:"PRIMARY",primaryBandOverride=primaryBandOverride?.name,effectivePrimaryBand=primaryBandOverride?.name,overrideReason=overrideReason,hapticsEnabled=hapticsEnabled,version=expectedVersion+1))}
        override suspend fun educationSources(base:String,token:String,family:String)=RemoteResult.Ok(emptyList<RemoteEducationSource>())
        override suspend fun createEducationSource(base:String,token:String,family:String,title:String,sourceUrl:String,stages:List<SchoolStage>,usageNote:String,key:String):RemoteResult<RemoteEducationSource>{resourceCalls += "create";return RemoteResult.Ok(RemoteEducationSource("source-1",title,sourceUrl,stages.map{it.name},usageNote,"DRAFT","NEVER","",null,emptyList()))}
        override suspend fun educationSourceAction(base:String,token:String,family:String,source:String,action:String,key:String):RemoteResult<RemoteEducationSource>{resourceCalls += action;return RemoteResult.Ok(RemoteEducationSource(source,"公益课堂","https://learn.example.org",listOf("PRIMARY"),"免费浏览","DRAFT","READY","",null,emptyList()))}
        override suspend fun childEducationCatalog(base:String,token:String,family:String,child:String):RemoteResult<List<RemoteChildEducationSource>>{resourceCalls += "child-catalog";return RemoteResult.Ok(emptyList())}
        override suspend fun learningAttempt(base:String,token:String,family:String,child:String,assignment:String,activity:String,response:String,playedSeconds:Int?,durationSeconds:Int?,key:String):RemoteResult<RemoteLearningAssignment>{learningKey=key;learningToken=token;return RemoteResult.Ok(learningAssignment())}
        override suspend fun createTeachingCourse(base:String,token:String,family:String,draft:RemoteTeachingCourseDraft,key:String):RemoteResult<RemoteTeachingVersion>{teachingCalls += "create:$token";teachingDraft=draft;return RemoteResult.Ok(version("DRAFT"))}
        override suspend fun publishTeachingVersion(base:String,token:String,family:String,version:String,key:String):RemoteResult<RemoteTeachingVersion>{teachingCalls += "publish:$token";return RemoteResult.Ok(version("PUBLISHED"))}
        override suspend fun teachingVersion(base:String,token:String,family:String,version:String):RemoteResult<RemoteTeachingVersion>{teachingCalls += "detail:$token";return RemoteResult.Ok(version("PUBLISHED"))}
        override suspend fun assignTeachingLesson(base:String,token:String,family:String,child:String,version:String,lesson:String,key:String):RemoteResult<RemoteLearningAssignment>{teachingCalls += "assign:$token";return RemoteResult.Ok(learningAssignment())}
        override suspend fun submit(base:String,childToken:String,family:String,child:String,task:String,key:String)=RemoteResult.Ok(Unit)
        override suspend fun review(base:String,parentToken:String,family:String,completion:String,key:String)=RemoteResult.Ok(Unit)
        override suspend fun usage(base:String,childToken:String,family:String,child:String,key:String,occurredAt:Instant):RemoteResult<Unit>{usageKeys += key;return RemoteResult.Ok(Unit)}
        private fun version(status:String)=RemoteTeachingVersion("course","version","自然课","PRIMARY","FAMILY",1,status,listOf("lesson"))
        private fun learningAssignment()=RemoteLearningAssignment("assignment","自然课","家庭学习夹","找叶子","去户外观察","PRIMARY","FAMILY","ASSIGNED",0,emptyList(),"")
    }
    companion object {
        const val FAMILY="11111111-1111-1111-1111-111111111111"
        const val PARENT="22222222-2222-2222-2222-222222222222"
        const val CHILD="33333333-3333-3333-3333-333333333333"
    }
}
