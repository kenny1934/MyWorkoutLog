package com.kennychiu.myworkoutlog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DkPrimary,
    onPrimary = DkOnPrimary,
    secondary = DkSecondary,
    onSecondary = DkOnSecondary,
    background = DkBackground,
    surface = DkSurface,
    onBackground = DkOnSurface,
    onSurface = DkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = LtPrimary,
    onPrimary = LtOnPrimary,
    secondary = LtSecondary,
    onSecondary = LtOnSecondary,
    background = LtBackground,
    surface = LtSurface,
    onBackground = LtOnSurface,
    onSurface = LtOnSurface
)

@Composable
fun MyWorkoutLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
