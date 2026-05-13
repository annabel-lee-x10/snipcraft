package dev.a10101100.snipcraft.core.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val SnipDarkColorScheme = darkColorScheme(
    primary = SnipPrimary,
    onPrimary = SnipOnPrimary,
    secondary = SnipSecondary,
    background = SnipBackground,
    surface = SnipSurface,
    onBackground = SnipOnBackground,
    error = SnipError,
)

private val SnipLightColorScheme = lightColorScheme(
    primary = SnipPrimaryLight,
    background = SnipBackgroundLight,
    onBackground = SnipOnBackgroundLight,
)

@Composable
fun SnipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SnipDarkColorScheme
        else -> SnipLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SnipTypography,
        content = content,
    )
}
