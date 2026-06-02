package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TerminalMainScreen
import com.example.ui.TerminalViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val terminalViewModel: TerminalViewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.TerminalBackground
                ) {
                    TerminalMainScreen(viewModel = terminalViewModel)
                }
            }
        }
    }
}
