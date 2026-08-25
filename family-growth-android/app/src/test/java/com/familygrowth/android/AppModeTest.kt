package com.familygrowth.android
import com.familygrowth.android.core.AppMode
import org.junit.Assert.assertEquals
import org.junit.Test
class AppModeTest { @Test fun exposesParentAndChildModes(){ assertEquals(listOf("CHILD","PARENT"),AppMode.entries.map{it.name}) } }
