package com.example.roomie.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Placeholder
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.theme.TextSecondary

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = RoomieType.fieldLabel)
}

/**
 * Bordered input from the design: white fill, 1dp tan border (2dp brown when focused),
 * 10dp corners, 14dp padding. Pass [height] = null for a multi-line text area.
 */
@Composable
fun RoomieTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    height: Dp? = 50.dp,
    minHeight: Dp = 50.dp,
    corner: Dp = 10.dp,
    background: Color = Surface,
    textStyle: TextStyle = RoomieType.input,
    suffix: String? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    contentDescription: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(corner)
    val borderColor = when {
        isError -> Danger
        focused -> BrandBrown
        else -> Border
    }
    val singleLine = height != null
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier.height(minHeight))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        textStyle = textStyle.copy(color = TextPrimary),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        cursorBrush = SolidColor(BrandBrown),
        decorationBox = { inner ->
            Row(
                Modifier
                    .clip(shape)
                    .background(background)
                    .border(if (focused || isError) 2.dp else 1.dp, borderColor, shape)
                    .padding(horizontal = 14.dp, vertical = if (singleLine) 0.dp else 12.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle.copy(color = Placeholder))
                    inner()
                }
                if (suffix != null) Text(suffix, style = RoomieType.body.copy(color = TextSecondary))
            }
        },
    )
}

/** Label above a field, matching the 6dp label gap used throughout the design. */
@Composable
fun LabeledField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        content()
    }
}

/** Calendar button that opens a date picker ("Tue, Nov 17  >"). */
@Composable
fun DateButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_calendar), null, Modifier.size(20.dp), tint = Color.Unspecified)
        Text(text, style = RoomieType.input, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_chevron_right_18), null, Modifier.size(18.dp), tint = Color.Unspecified)
    }
}

@Composable
fun ErrorText(text: String?, modifier: Modifier = Modifier) {
    if (text != null) Text(text, modifier = modifier, style = RoomieType.caption.copy(color = Danger, fontSize = 13.sp))
}

@Composable
fun IconTextRow(@DrawableRes icon: Int, text: String, modifier: Modifier = Modifier, style: TextStyle = RoomieType.body) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), null, Modifier.size(16.dp), tint = Color.Unspecified)
        Text(text, style = style)
    }
}
