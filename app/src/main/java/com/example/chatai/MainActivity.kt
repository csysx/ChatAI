package com.example.chatai

import android.os.Build
import android.os.Bundle
import com.example.chatai.ui.theme.AIChatAppTheme
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.activity.ComponentActivity
import com.example.chatai.ui.MainNavigation
import com.example.chatai.viewmodel.ViewModelFactory
import dagger.hilt.android.AndroidEntryPoint


// MainActivity.kt

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = ViewModelFactory(applicationContext)
//        val viewModel = ViewModelProvider(this, factory)[ChatViewModel::class.java]

        setContent {
            AIChatAppTheme {
                MainNavigation(factory)
            }
        }
    }
}
