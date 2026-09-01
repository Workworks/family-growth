package com.familygrowth.android.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

data class LocalSyncState(val clientId:String,val cursor:Long,val knownDigests:Map<String,String>)

class ReliableSyncStateStore(context:Context){
    private val dao=LocalStateDatabase.get(context).stateDao();private val cipher=KeystoreStateCipher()
    suspend fun load(family:String,child:String):LocalSyncState=withContext(Dispatchers.IO){
        val identity=dao.clientIdentity()?:LocalClientIdentityEntity(clientId=UUID.randomUUID().toString(),createdAt=System.currentTimeMillis()).also{dao.putClientIdentity(it)}
        val cursor=dao.cursor(scope(family,child));val known=cursor?.let{decode(cipher.decrypt(it.encryptedKnownDigests))}?:emptyMap()
        LocalSyncState(identity.clientId,cursor?.cursor?:0,known)
    }
    suspend fun apply(family:String,child:String,cursor:Long,changed:Map<String,String>,tombstones:List<String>){withContext(Dispatchers.IO){
        val current=load(family,child);require(cursor>=current.cursor){"同步游标不能倒退"};val next=current.knownDigests.toMutableMap().apply{putAll(changed);tombstones.forEach(::remove)}
        dao.putCursor(FamilySyncCursorEntity(scope(family,child),cursor,cipher.encrypt(encode(next)),System.currentTimeMillis()))
    }}
    suspend fun recordConflict(family:String,child:String,action:String,key:String,message:String,serverFacts:String){withContext(Dispatchers.IO){dao.putConflict(SyncConflictEntity(UUID.randomUUID().toString(),family,child,action,key,message.take(240),cipher.encrypt(serverFacts.toByteArray()),"OPEN",System.currentTimeMillis(),null))}}
    suspend fun conflicts(family:String,child:String)=withContext(Dispatchers.IO){dao.conflicts(family,child)}
    private fun scope(f:String,c:String)="$f:$c"
    private fun encode(values:Map<String,String>)=JSONObject(values).toString().toByteArray()
    private fun decode(bytes:ByteArray):Map<String,String>{val json=JSONObject(String(bytes));return json.keys().asSequence().associateWith(json::getString)}
}
