package com.familygrowth.android.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.*
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Entity(tableName="local_state_snapshot")
data class LocalStateSnapshotEntity(@PrimaryKey val scope:String="device",val schemaVersion:Int,val encryptedPayload:ByteArray,val updatedAt:Long)

@Entity(tableName="local_migration_receipt")
data class LocalMigrationReceiptEntity(@PrimaryKey val migrationId:String,val sourceDigest:String,val completedAt:Long)

@Entity(tableName="family_sync_cursor")
data class FamilySyncCursorEntity(@PrimaryKey val scope:String,val cursor:Long,val encryptedKnownDigests:ByteArray,val updatedAt:Long)

@Entity(tableName="local_client_identity")
data class LocalClientIdentityEntity(@PrimaryKey val scope:String="device",val clientId:String,val createdAt:Long)

@Entity(tableName="durable_command",indices=[Index(value=["familyId","childId","idempotencyKey"],unique=true)])
data class DurableCommandEntity(@PrimaryKey val id:String,val familyId:String,val childId:String,val actionType:String,val idempotencyKey:String,val encryptedPayload:ByteArray,val state:String,val attempts:Int,val createdAt:Long,val updatedAt:Long)

@Entity(tableName="sync_conflict",indices=[Index(value=["familyId","childId","status"])])
data class SyncConflictEntity(@PrimaryKey val id:String,val familyId:String,val childId:String,val actionType:String,val idempotencyKey:String,val message:String,val encryptedServerFacts:ByteArray,val status:String,val createdAt:Long,val resolvedAt:Long?)

@Dao interface LocalStateDao {
    @Query("SELECT * FROM local_state_snapshot WHERE scope='device'") suspend fun snapshot():LocalStateSnapshotEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putSnapshot(value:LocalStateSnapshotEntity)
    @Query("SELECT * FROM local_migration_receipt WHERE migrationId=:id") suspend fun receipt(id:String):LocalMigrationReceiptEntity?
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun putReceipt(value:LocalMigrationReceiptEntity)
    @Query("SELECT * FROM family_sync_cursor WHERE scope=:scope") suspend fun cursor(scope:String):FamilySyncCursorEntity?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putCursor(value:FamilySyncCursorEntity)
    @Query("SELECT * FROM local_client_identity WHERE scope='device'") suspend fun clientIdentity():LocalClientIdentityEntity?
    @Insert(onConflict=OnConflictStrategy.ABORT) suspend fun putClientIdentity(value:LocalClientIdentityEntity)
    @Query("SELECT * FROM sync_conflict WHERE familyId=:family AND childId=:child AND status='OPEN' ORDER BY createdAt") suspend fun conflicts(family:String,child:String):List<SyncConflictEntity>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun putConflict(value:SyncConflictEntity)
    @Query("UPDATE sync_conflict SET status='RESOLVED',resolvedAt=:at WHERE id=:id AND status='OPEN'") suspend fun resolveConflict(id:String,at:Long):Int
}

@Database(entities=[LocalStateSnapshotEntity::class,LocalMigrationReceiptEntity::class,FamilySyncCursorEntity::class,LocalClientIdentityEntity::class,DurableCommandEntity::class,SyncConflictEntity::class],version=1,exportSchema=true)
abstract class LocalStateDatabase:RoomDatabase(){abstract fun stateDao():LocalStateDao
    companion object{@Volatile private var instance:LocalStateDatabase?=null
        fun get(context:Context):LocalStateDatabase=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,LocalStateDatabase::class.java,"family_growth_state.db").build().also{instance=it}}
    }
}

interface StateCipher { fun encrypt(plain:ByteArray):ByteArray;fun decrypt(blob:ByteArray):ByteArray }

class KeystoreStateCipher(private val alias:String="family_growth_room_state_key_v1"):StateCipher{
    override fun encrypt(plain:ByteArray):ByteArray{val cipher=Cipher.getInstance(TRANSFORMATION);cipher.init(Cipher.ENCRYPT_MODE,key());val encrypted=cipher.doFinal(plain);return ByteBuffer.allocate(1+1+cipher.iv.size+encrypted.size).put(FORMAT).put(cipher.iv.size.toByte()).put(cipher.iv).put(encrypted).array()}
    override fun decrypt(blob:ByteArray):ByteArray{require(blob.size>14&&blob[0]==FORMAT){"本机状态密文格式无效"};val ivSize=blob[1].toInt() and 0xff;require(ivSize==12&&blob.size>2+ivSize){"本机状态 IV 无效"};val iv=blob.copyOfRange(2,2+ivSize);val encrypted=blob.copyOfRange(2+ivSize,blob.size);return Cipher.getInstance(TRANSFORMATION).apply{init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,iv))}.doFinal(encrypted)}
    private fun key():SecretKey{val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)};(store.getKey(alias,null) as? SecretKey)?.let{return it};return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())}.generateKey()}
    private companion object{const val FORMAT:Byte=1;const val TRANSFORMATION="AES/GCM/NoPadding"}
}

object RecoverableMigrationPolicy{
    enum class Step{READ_LEGACY,WRITE_ENCRYPTED,VERIFY_DECRYPT,WRITE_RECEIPT,DELETE_LEGACY}
    val orderedSteps=Step.entries
    fun mayDeleteLegacy(completed:Set<Step>)=completed.containsAll(orderedSteps.dropLast(1))
}
