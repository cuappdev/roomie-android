package com.example.roomie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface

/**
 * Tab header: brown title bottom-aligned with an optional trailing action. Defaults are the white
 * header; Home and Me pass the cream refresh look.
 */
@Composable
fun RoomieHeader(
    title: String,
    modifier: Modifier = Modifier,
    background: Color = Surface,
    titleStyle: TextStyle = RoomieType.screenTitle,
    height: Dp = 73.dp,
    endPadding: Dp = 10.dp,
    bottomPadding: Dp = 12.dp,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding()
            .height(height)
            .padding(start = 20.dp, end = endPadding, bottom = bottomPadding),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(title, style = titleStyle, modifier = Modifier.weight(1f).semantics { heading() })
        trailing()
    }
}

@Composable
fun SettingsAction(onClick: () -> Unit) {
    IconAction(R.drawable.ic_settings, stringResource(R.string.settings), onClick, Modifier.padding(bottom = 0.dp))
}
