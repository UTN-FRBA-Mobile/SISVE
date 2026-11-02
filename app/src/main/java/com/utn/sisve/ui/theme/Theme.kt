package com.utn.sisve.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Red80,
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    inversePrimary = Red40,
    surfaceTint = Red80,

    secondary = RedGrey80,
    onSecondary = Color(0xFF442929),
    secondaryContainer = Color(0xFF5D3F3F),
    onSecondaryContainer = Color(0xFFFFDAD9),

    tertiary = Orange80,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDEB0)
)

private val LightColorScheme = lightColorScheme(
    primary = Red40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    inversePrimary = Red80,
    surfaceTint = Red40,

    secondary = RedGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD9),
    onSecondaryContainer = Color(0xFF2C1515),

    tertiary = Orange40,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFDEB0),
    onTertiaryContainer = Color(0xFF2B1700)
)

@Composable
fun SisveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
