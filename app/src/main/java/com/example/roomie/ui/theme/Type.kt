package com.example.roomie.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.roomie.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Hand-written feel for sticky notes (typography/sticky-board in Figma).
val Poppins = FontFamily(Font(R.font.poppins_regular, FontWeight.Normal))

private fun inter(size: Float, weight: FontWeight, color: androidx.compose.ui.graphics.Color = TextPrimary) =
    TextStyle(fontFamily = Inter, fontSize = size.sp, fontWeight = weight, color = color)

/** Named text styles used across Roomie screens, taken from the Figma frames. */
object RoomieType {
    val screenTitle = inter(26f, FontWeight.Bold, BrandBrown).copy(lineHeight = 1.2.em)
    val welcomeTitle = inter(32f, FontWeight.Bold, BrandBrown).copy(lineHeight = 36.8.sp)
    val onboardingTitle = inter(30f, FontWeight.Bold, BrandBrown).copy(lineHeight = 34.5.sp, letterSpacing = (-0.3).sp)
    val onboardingSubtitle = inter(20f, FontWeight.Medium, TextSecondary).copy(lineHeight = 23.sp, letterSpacing = (-0.3).sp)
    val sheetTitle = inter(22f, FontWeight.Bold, BrandBrown)
    val sectionTitle = inter(16f, FontWeight.SemiBold).copy(lineHeight = 20.8.sp)
    val fieldLabel = inter(13f, FontWeight.SemiBold, TextSecondary)
    val input = inter(16f, FontWeight.Normal)
    val body = inter(14f, FontWeight.Normal)
    val bodySecondary = inter(13f, FontWeight.Normal, TextSecondary)
    val caption = inter(12.5f, FontWeight.Normal, TextSecondary)
    val primaryButton = inter(16f, FontWeight.SemiBold, Surface)
    val linkButton = inter(15f, FontWeight.SemiBold, BrandBrown)
    val pillButton = inter(13f, FontWeight.SemiBold, BrandBrown)
    val pill = inter(12f, FontWeight.SemiBold)
    val listTitle = inter(15f, FontWeight.Medium)
    val rowTitle = inter(13f, FontWeight.Medium)
    val rowMeta = inter(12f, FontWeight.Normal, TextSecondary)
    val inviteCode = inter(19f, FontWeight.Bold).copy(letterSpacing = 1.14.sp)
    val dueChip = inter(10.5f, FontWeight.SemiBold)
    val titleEmpty = inter(20f, FontWeight.Bold)
}

/** Material slots map to Inter so dialogs and pickers match the rest of the app. */
val Typography = Typography(
    displaySmall = inter(32f, FontWeight.Bold),
    headlineMedium = inter(26f, FontWeight.Bold),
    headlineSmall = inter(22f, FontWeight.Bold),
    titleLarge = inter(20f, FontWeight.Bold),
    titleMedium = inter(16f, FontWeight.SemiBold),
    titleSmall = inter(14f, FontWeight.SemiBold),
    bodyLarge = inter(16f, FontWeight.Normal).copy(lineHeight = 22.4.sp),
    bodyMedium = inter(14f, FontWeight.Normal).copy(lineHeight = 20.sp),
    bodySmall = inter(12f, FontWeight.Normal),
    labelLarge = inter(14f, FontWeight.SemiBold),
    labelMedium = inter(12f, FontWeight.Medium),
    labelSmall = inter(11f, FontWeight.Medium),
)
