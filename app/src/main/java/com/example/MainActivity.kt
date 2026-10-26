package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.screens.MainScreen
import com.example.ui.theme.FocusFlowTheme
import com.example.ui.viewmodel.ProductivityViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ProductivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FocusFlowTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
