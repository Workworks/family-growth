package com.familygrowth.android.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import com.familygrowth.android.core.SchoolStage

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
        val result = repository.updateExperience(LocalDate.of(2013, 8, 26), SchoolStage.JUNIOR_MIDDLE, "实际入学阶段", false, 0)
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

    private class FakeTransport : FamilyApiTransport {
        var expire = false
        val usageKeys = mutableListOf<String>()
        var updatedStage: SchoolStage? = null
        var updatedHaptics = true
        val resourceCalls = mutableListOf<String>()
        override suspend fun login(base:String,family:String,parent:String,pin:String)=RemoteResult.Ok("parent-token")
        override suspend fun childSession(base:String,parentToken:String,child:String)=RemoteResult.Ok("child-token")
        override suspend fun snapshot(base:String,token:String,family:String,child:String):RemoteResult<RemoteSnapshot> = if(expire) RemoteResult.Unauthorized else RemoteResult.Ok(RemoteSnapshot(family,child,"小树",BigDecimal("12.00"),30,emptyList(),0,0))
        override suspend fun experience(base:String,token:String,family:String,child:String):RemoteResult<RemoteExperienceProfile> = if(expire) RemoteResult.Unauthorized else RemoteResult.Ok(RemoteExperienceProfile("2022-08-26","KINDERGARTEN",null,"KINDERGARTEN","",true,0))
        override suspend fun updateExperience(base:String,token:String,family:String,child:String,birthDate:LocalDate,stageOverride:SchoolStage?,overrideReason:String,hapticsEnabled:Boolean,expectedVersion:Long):RemoteResult<RemoteExperienceProfile>{updatedStage=stageOverride;updatedHaptics=hapticsEnabled;return RemoteResult.Ok(RemoteExperienceProfile(birthDate.toString(),"PRIMARY",stageOverride?.name,stageOverride?.name?:"PRIMARY",overrideReason,hapticsEnabled,expectedVersion+1))}
        override suspend fun educationSources(base:String,token:String,family:String)=RemoteResult.Ok(emptyList<RemoteEducationSource>())
        override suspend fun createEducationSource(base:String,token:String,family:String,title:String,sourceUrl:String,stages:List<SchoolStage>,usageNote:String,key:String):RemoteResult<RemoteEducationSource>{resourceCalls += "create";return RemoteResult.Ok(RemoteEducationSource("source-1",title,sourceUrl,stages.map{it.name},usageNote,"DRAFT","NEVER","",null,emptyList()))}
        override suspend fun educationSourceAction(base:String,token:String,family:String,source:String,action:String,key:String):RemoteResult<RemoteEducationSource>{resourceCalls += action;return RemoteResult.Ok(RemoteEducationSource(source,"公益课堂","https://learn.example.org",listOf("PRIMARY"),"免费浏览","DRAFT","READY","",null,emptyList()))}
        override suspend fun childEducationCatalog(base:String,token:String,family:String,child:String):RemoteResult<List<RemoteChildEducationSource>>{resourceCalls += "child-catalog";return RemoteResult.Ok(emptyList())}
        override suspend fun submit(base:String,childToken:String,family:String,child:String,task:String,key:String)=RemoteResult.Ok(Unit)
        override suspend fun review(base:String,parentToken:String,family:String,completion:String,key:String)=RemoteResult.Ok(Unit)
        override suspend fun usage(base:String,childToken:String,family:String,child:String,key:String,occurredAt:Instant):RemoteResult<Unit>{usageKeys += key;return RemoteResult.Ok(Unit)}
    }
    companion object {
        const val FAMILY="11111111-1111-1111-1111-111111111111"
        const val PARENT="22222222-2222-2222-2222-222222222222"
        const val CHILD="33333333-3333-3333-3333-333333333333"
    }
}
