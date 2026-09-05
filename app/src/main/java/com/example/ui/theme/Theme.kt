package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = IndigoPrimary,
  onPrimary = IndigoOnPrimary,
  primaryContainer = IndigoPrimaryContainer,
  onPrimaryContainer = IndigoOnPrimaryContainer,
  secondary = CyanSecondary,
  onSecondary = CyanOnSecondary,
  secondaryContainer = CyanSecondaryContainer,
  onSecondaryContainer = CyanOnSecondaryContainer,
  tertiary = AmberTertiary,
  onTertiary = AmberOnTertiary,
  tertiaryContainer = AmberTertiaryContainer,
  onTertiaryContainer = AmberOnTertiaryContainer,
  background = NeutralLightBackground,
  onBackground = NeutralLightOnSurface,
  surface = NeutralLightSurface,
  onSurface = NeutralLightOnSurface,
  surfaceVariant = NeutralLightSurfaceVariant,
  onSurfaceVariant = NeutralLightOnSurface,
  outline = NeutralLightOutline,
  error = RoseError,
  errorContainer = RoseErrorContainer
)

private val DarkColorScheme = darkColorScheme(
  primary = IndigoPrimaryDark,
  onPrimary = IndigoOnPrimaryDark,
  primaryContainer = IndigoPrimaryContainerDark,
  onPrimaryContainer = IndigoOnPrimaryContainerDark,
  secondary = CyanSecondaryDark,
  onSecondary = CyanOnSecondaryDark,
  secondaryContainer = CyanSecondaryContainerDark,
  onSecondaryContainer = CyanOnSecondaryContainerDark,
  tertiary = AmberTertiaryDark,
  onTertiary = AmberOnTertiaryDark,
  tertiaryContainer = AmberTertiaryContainerDark,
  onTertiaryContainer = AmberOnTertiaryContainerDark,
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurface,
  outline = DarkOutline,
  error = RoseError,
  errorContainer = RoseErrorContainer
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our brand colors for cohesive coach aesthetic
  content: @Composable () -> Unit,
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
