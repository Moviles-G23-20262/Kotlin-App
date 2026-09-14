package com.campusswap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.data.AppViewModel
import com.campusswap.app.navigation.CampusSwapApp
import com.campusswap.app.ui.theme.CampusSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel()
            CampusSwapTheme(darkTheme = appViewModel.isDarkTheme) {
                CampusSwapApp(appViewModel = appViewModel)
            }
        }
    }
}
