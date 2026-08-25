package com.familygrowth.android.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

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

    private class FakeTransport : FamilyApiTransport {
        var expire = false
        override suspend fun login(base:String,family:String,parent:String,pin:String)=RemoteResult.Ok("parent-token")
        override suspend fun childSession(base:String,parentToken:String,child:String)=RemoteResult.Ok("child-token")
        override suspend fun snapshot(base:String,token:String,family:String,child:String):RemoteResult<RemoteSnapshot> = if(expire) RemoteResult.Unauthorized else RemoteResult.Ok(RemoteSnapshot(family,child,"小树",BigDecimal("12.00"),30,emptyList(),0,0))
        override suspend fun submit(base:String,childToken:String,family:String,child:String,task:String,key:String)=RemoteResult.Ok(Unit)
        override suspend fun review(base:String,parentToken:String,family:String,completion:String,key:String)=RemoteResult.Ok(Unit)
    }
    companion object {
        const val FAMILY="11111111-1111-1111-1111-111111111111"
        const val PARENT="22222222-2222-2222-2222-222222222222"
        const val CHILD="33333333-3333-3333-3333-333333333333"
    }
}
