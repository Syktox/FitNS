package com.syktox.fitns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.syktox.fitns.core.design.FitNsTheme
import com.syktox.fitns.navigation.FitNsApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitNsTheme {
                FitNsApp()
            }
        }
    }
}

