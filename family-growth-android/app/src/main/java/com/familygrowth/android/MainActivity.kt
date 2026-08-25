package com.familygrowth.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.familygrowth.android.ui.FamilyGrowthApp
import com.familygrowth.android.ui.FamilyGrowthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FamilyGrowthTheme { FamilyGrowthApp() } }
    }
}
