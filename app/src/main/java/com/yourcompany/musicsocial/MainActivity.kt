package com.yourcompany.musicsocial

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourcompany.musicsocial.core.datastore.AppPreferencesDataSource
import com.yourcompany.musicsocial.core.datastore.ThemeMode
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferences: AppPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(darkTheme = true)
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by preferences.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
            }

            // Keep system bar icon colours readable when the theme changes.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(darkTheme)
                onDispose {}
            }

            ButterTheme(darkTheme = darkTheme) {
                ButterApp()
            }
        }
    }

    private fun enableEdgeToEdge(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
