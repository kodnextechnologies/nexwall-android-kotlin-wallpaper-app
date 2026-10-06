package com.kodnex.nexwall.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodnex.nexwall.sample.ui.CategoriesScreen
import com.kodnex.nexwall.sample.ui.PreviewScreen
import com.kodnex.nexwall.sample.ui.Screen
import com.kodnex.nexwall.sample.ui.WallpaperViewModel
import com.kodnex.nexwall.sample.ui.WallpapersScreen

class MainActivity : ComponentActivity() {
    private val viewModel: WallpaperViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val backStack by viewModel.backStack.collectAsStateWithLifecycle()
                BackHandler(enabled = backStack.size > 1) { viewModel.back() }

                when (val screen = backStack.last()) {
                    Screen.Categories -> CategoriesScreen(viewModel)
                    is Screen.Wallpapers -> WallpapersScreen(viewModel, screen.category)
                    is Screen.Preview -> PreviewScreen(viewModel, screen.wallpaper)
                }
            }
        }
    }
}

@Composable
private fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
