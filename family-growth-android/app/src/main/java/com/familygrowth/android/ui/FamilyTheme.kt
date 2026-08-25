package com.familygrowth.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

object GrowthColors {
    val Emerald = Color(0xFF087A57)
    val EmeraldDark = Color(0xFF075D45)
    val Mint = Color(0xFFE8F7F0)
    val Amber = Color(0xFFF2A640)
    val Canvas = Color(0xFFF4F7F5)
    val Ink = Color(0xFF10231C)
    val Slate = Color(0xFF607069)
    val Border = Color(0xFFDDE6E1)
    val Coral = Color(0xFFD95757)
}

object ChildColors {
    val Paper = Color(0xFFF7F8F3)
    val Ink = Color(0xFF24332D)
    val Moss = Color(0xFF3D6B57)
    val Mist = Color(0xFFDDE7DF)
    val Sun = Color(0xFFE7B84B)
    val Coral = Color(0xFFC96B5A)
}

private val LightColors = lightColorScheme(
    primary = GrowthColors.Emerald,
    onPrimary = Color.White,
    primaryContainer = GrowthColors.Mint,
    onPrimaryContainer = GrowthColors.EmeraldDark,
    secondary = GrowthColors.Amber,
    onSecondary = GrowthColors.Ink,
    background = GrowthColors.Canvas,
    onBackground = GrowthColors.Ink,
    surface = Color.White,
    onSurface = GrowthColors.Ink,
    surfaceVariant = Color(0xFFEDF2EF),
    onSurfaceVariant = GrowthColors.Slate,
    outline = GrowthColors.Border,
    error = GrowthColors.Coral,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF63D5AB),
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF09543F),
    secondary = Color(0xFFFFC56D),
    background = Color(0xFF101713),
    surface = Color(0xFF17201B),
    surfaceVariant = Color(0xFF223029),
    outline = Color(0xFF3C4C44),
    error = Color(0xFFFFB4AB),
)

private val GrowthTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun FamilyGrowthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = GrowthTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(34.dp),
        ),
        content = content,
    )
}
