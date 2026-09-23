package com.vika.quest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vika.quest.ui.QuestApp
import com.vika.quest.ui.theme.QuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as QuestApplication).container
        setContent {
            QuestTheme {
                QuestApp(container = container)
            }
        }
    }
}
