package com.example.kanjoos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kanjoos.ui.MainScreen
import com.example.kanjoos.ui.theme.KanjoosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanjoosTheme(darkTheme = true) {
                MainScreen()
            }
        }
    }
}
