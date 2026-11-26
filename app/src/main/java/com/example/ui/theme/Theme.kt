package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LedgerSemanticColors(
  val baki: Color,
  val bakiContainer: Color,
  val onBakiContainer: Color,
  val joma: Color,
  val jomaContainer: Color,
  val onJomaContainer: Color,
  val whatsapp: Color,
)

private val LightLedgerColors =
  LedgerSemanticColors(
    baki = BakiRed,
    bakiContainer = BakiRedContainer,
    onBakiContainer = BakiOnContainer,
    joma = JomaGreen,
    jomaContainer = JomaGreenContainer,
    onJomaContainer = JomaOnContainer,
    whatsapp = WhatsAppGreen,
  )

private val DarkLedgerColors =
  LedgerSemanticColors(
    baki = DarkBakiRed,
    bakiContainer = DarkBakiContainer,
    onBakiContainer = DarkBakiOnContainer,
    joma = DarkJomaGreen,
    jomaContainer = DarkJomaContainer,
    onJomaContainer = DarkJomaOnContainer,
    whatsapp = WhatsAppBright,
  )

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkEmeraldOnContainer,
    secondary = DarkMarigoldSecondary,
    onSecondary = DarkMarigoldOnSecondary,
    secondaryContainer = DarkMarigoldContainer,
    onSecondaryContainer = DarkMarigoldOnContainer,
    tertiary = TealContainer,
    background = DarkBackground,
    onBackground = DarkInkText,
    surface = DarkSurface,
    onSurface = DarkInkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInkText,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = DarkBakiRed,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = MarigoldSecondary,
    onSecondary = MarigoldOnSecondary,
    secondaryContainer = MarigoldContainer,
    onSecondaryContainer = MarigoldOnContainer,
    tertiary = TealTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = TealOnContainer,
    background = IvoryBackground,
    onBackground = InkText,
    surface = WarmSurface,
    onSurface = InkText,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedInkText,
    outline = WarmOutline,
    outlineVariant = WarmOutline,
    error = BakiRed,
    errorContainer = BakiRedContainer,
    onErrorContainer = BakiOnContainer,
  )

private val TallyShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun TallyKhataTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val ledgerColors = if (darkTheme) DarkLedgerColors else LightLedgerColors

  CompositionLocalProvider(LocalLedgerColors provides ledgerColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = TallyShapes,
      content = content,
    )
  }
}
