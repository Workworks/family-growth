package com.familygrowth.android.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class ProductionFamilyApiTest {
    @Test fun exchangePreviewKeepsFeeAndTargetForExplicitConfirmation() {
        val preview=remoteExchangePreview("preview-1","10.00","0.50","9.00","家庭内部教育账本")
        assertEquals("preview-1",preview.id)
        assertEquals(BigDecimal("10.00"),preview.sourceAmount)
        assertEquals(BigDecimal("0.50"),preview.fee)
        assertEquals(BigDecimal("9.00"),preview.targetAmount)
        assertEquals("家庭内部教育账本",preview.notice)
    }
}
