package com.mohsenoid.certhunter.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mohsenoid.certhunter.ui.theme.CertHunterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CertHunterTheme {
                AppNavHost()
            }
        }
    }
}
