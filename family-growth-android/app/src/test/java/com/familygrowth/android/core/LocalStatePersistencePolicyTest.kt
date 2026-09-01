package com.familygrowth.android.core

import org.junit.Assert.*
import org.junit.Test

class LocalStatePersistencePolicyTest {
    @Test fun legacyDeletionIsLastAndRequiresVerifiedEncryptedWriteAndReceipt(){
        assertEquals(RecoverableMigrationPolicy.Step.DELETE_LEGACY,RecoverableMigrationPolicy.orderedSteps.last())
        assertFalse(RecoverableMigrationPolicy.mayDeleteLegacy(setOf(RecoverableMigrationPolicy.Step.READ_LEGACY,RecoverableMigrationPolicy.Step.WRITE_ENCRYPTED)))
        assertFalse(RecoverableMigrationPolicy.mayDeleteLegacy(RecoverableMigrationPolicy.orderedSteps.dropLast(2).toSet()))
        assertTrue(RecoverableMigrationPolicy.mayDeleteLegacy(RecoverableMigrationPolicy.orderedSteps.dropLast(1).toSet()))
    }

    @Test fun syncAndConflictRowsDoNotHaveTokenOrPinColumns(){
        val names=FamilySyncCursorEntity::class.java.declaredFields.map{it.name}.toSet()+SyncConflictEntity::class.java.declaredFields.map{it.name}
        assertFalse(names.any{it.contains("token",true)||it.contains("pin",true)||it.contains("password",true)})
        assertTrue("encryptedKnownDigests" in names)
        assertTrue("encryptedServerFacts" in names)
    }
}
