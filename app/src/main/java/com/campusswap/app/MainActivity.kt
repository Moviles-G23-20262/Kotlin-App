package com.campusswap.app

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusswap.app.viewmodel.AppViewModel
import com.campusswap.app.views.navigation.CampusSwapApp
import com.campusswap.app.views.themes.CampusSwapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                .launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val appViewModel: AppViewModel = viewModel()
            LaunchedEffect(Unit) { appViewModel.restoreTheme((application as CampusSwapApplication).container.themeStore) }
            // Follows the phone's light/dark setting until the user picks one in the Seller Hub.
            val dark = appViewModel.darkThemeChoice ?: isSystemInDarkTheme()
            LaunchedEffect(dark) {
                val style = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            CampusSwapTheme(darkTheme = dark) {
                CampusSwapApp(appViewModel = appViewModel)
            }
        }
    }
}
