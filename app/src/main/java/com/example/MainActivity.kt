package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.SmartCardApp
import com.example.ui.SmartCardViewModel
import com.example.ui.SmartCardViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get repository from Application
        val app = application as SmartCardApplication
        val repository = app.repository

        // Initialize ViewModel using Factory
        val viewModel: SmartCardViewModel by viewModels {
            SmartCardViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.CharcoalBg
                ) {
                    SmartCardApp(viewModel = viewModel)
                }
            }
        }
    }
}
