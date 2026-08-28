package com.familygrowth.android.core

import org.junit.Assert.*
import org.junit.Test

class SmartEduOfficialSourceTest {
    @Test fun verifiedTextbookSelectionUsesProvidedOfficialDeepLink() {
        val selection = OfficialLearningSelection(SchoolStage.PRIMARY, "一年级", "语文", "统编版", "上册")
        assertTrue(SmartEduOfficialSource.hasVerifiedDeepLink(selection))
        assertEquals(SmartEduOfficialSource.VERIFIED_PRIMARY_ONE_CHINESE_URL, SmartEduOfficialSource.launchUrl(selection))
    }

    @Test fun unknownCombinationFallsBackToOfficialSelectorWithoutGuessingIds() {
        val selection = OfficialLearningSelection(SchoolStage.PRIMARY, "二年级", "数学", "人教版", "下册")
        assertFalse(SmartEduOfficialSource.hasVerifiedDeepLink(selection))
        assertEquals(SmartEduOfficialSource.BASE_URL, SmartEduOfficialSource.launchUrl(selection))
    }

    @Test fun navigationAllowsOnlyHttpsOfficialCourseArea() {
        assertTrue(SmartEduOfficialSource.isAllowedNavigation(SmartEduOfficialSource.VERIFIED_PRIMARY_ONE_CHINESE_URL))
        assertTrue(SmartEduOfficialSource.isAllowedNavigation("https://basic.smartedu.cn/resource-detail?id=1"))
        assertFalse(SmartEduOfficialSource.isAllowedNavigation("http://basic.smartedu.cn/syncClassroom"))
        assertFalse(SmartEduOfficialSource.isAllowedNavigation("https://evil.example/syncClassroom"))
        assertFalse(SmartEduOfficialSource.isAllowedNavigation("https://basic.smartedu.cn/recommend"))
    }
}
