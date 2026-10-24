package com.example.roomie.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.ButtonBorder
import com.example.roomie.ui.theme.ButtonGradientEnd
import com.example.roomie.ui.theme.ButtonGradientMid
import com.example.roomie.ui.theme.ButtonGradientStart
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.GlassButtonBorder
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface

/** The brand brown gradient used by primary buttons and the rotation header cards. */
val BrandGradient = Brush.horizontalGradient(
    0f to ButtonGradientStart,
    0.375f to ButtonGradientMid,
    0.908f to ButtonGradientEnd,
)

/** Brown gradient fill with a half-alpha border and soft drop shadow (onboarding refresh). */
private fun Modifier.gradientFill(shape: RoundedCornerShape): Modifier = this
    .shadow(3.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.1f), spotColor = ShadowWarm.copy(alpha = 0.1f))
    .clip(shape)
    .background(BrandGradient)
    .border(1.dp, ButtonBorder, shape)

/** Full-width brown call to action ("Next", "Sign in", "Post note"). [gradient] uses the refreshed style. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    gradient: Boolean = false,
    corner: Dp = 12.dp,
    @DrawableRes icon: Int? = null,
    textStyle: TextStyle = RoomieType.primaryButton,
) {
    val shape = RoundedCornerShape(corner)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (gradient) Modifier.gradientFill(shape) else Modifier.clip(shape).background(BrandBrown))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = textStyle)
    }
}

/** White outlined button ("Share invite link", "Sign out", "Delete"). [glass] uses the refreshed warm glass style. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    contentColor: Color = BrandBrown,
    glass: Boolean = false,
    height: Dp = 46.dp,
    textStyle: TextStyle = if (glass) RoomieType.glassButton else RoomieType.linkButton,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (glass) Modifier.glassSurface(shape, borderColor = GlassButtonBorder)
                else Modifier.clip(shape).background(Surface).border(BorderStroke(1.dp, Border), shape),
            )
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(18.dp), tint = Color.Unspecified)
        Text(text, style = textStyle.copy(color = contentColor))
    }
}

/** Small outlined pill with a plus ("Add note", "Add chore"). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = R.drawable.ic_plus_small,
    height: Dp = 34.dp,
) {
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier
            .height(height)
            .clip(shape)
            .border(1.dp, BrandBrown, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(14.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.pillButton)
    }
}

/** Plain brown text action ("Skip for now", "Cancel", "Edit"). */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = RoomieType.linkButton,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = style.copy(color = if (enabled) style.color else style.color.copy(alpha = 0.5f)))
    }
}

/** Brown "Add" button that sits next to an input. */
@Composable
fun InlineAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Add",
    height: Dp = 46.dp,
    corner: Dp = 8.dp,
    enabled: Boolean = true,
    gradient: Boolean = false,
    textStyle: TextStyle = RoomieType.primaryButton.copy(fontSize = 15.sp),
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (gradient) Modifier.gradientFill(shape) else Modifier.clip(shape).background(BrandBrown))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = textStyle)
    }
}

/** Square icon button with a 44dp touch target. */
@Composable
fun IconAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    tint: Color = Color.Unspecified,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, Modifier.size(iconSize), tint = tint)
    }
}

/** Red outlined variant used for destructive actions. */
@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null) =
    SecondaryButton(text, onClick, modifier, icon, contentColor = Danger)
