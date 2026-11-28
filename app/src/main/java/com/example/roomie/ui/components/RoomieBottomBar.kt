package com.example.roomie.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Tan

enum class RoomieTab(@StringRes val label: Int, val icon: Int, val activeIcon: Int, val iconSize: Dp = 24.dp) {
    Home(R.string.tab_home, R.drawable.ic_tab_home, R.drawable.ic_tab_home_active),
    Chores(R.string.tab_chores, R.drawable.ic_tab_chores, R.drawable.ic_tab_chores_active),
    Notes(R.string.tab_notes, R.drawable.ic_tab_notes, R.drawable.ic_tab_notes_active),
    Shopping(R.string.tab_shopping, R.drawable.ic_tab_cart, R.drawable.ic_tab_cart_active),
    Profile(R.string.tab_profile, R.drawable.ic_tab_profile, R.drawable.ic_tab_profile_active, 20.dp),
}

private val NavGlass = Brush.verticalGradient(listOf(Color(0x29FFFCF7), Color(0x1CF0E0C7)))

/**
 * "Roomie / Navigation": five icon-only tabs on warm glass. The active tab gets a soft tan pill and
 * a darker, heavier icon. Each tab keeps an accessible name.
 */
@Composable
fun RoomieBottomBar(selected: RoomieTab?, onSelect: (RoomieTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .shadow(
                7.dp,
                RectangleShape,
                clip = false,
                ambientColor = ShadowWarm.copy(alpha = 0.1f),
                spotColor = ShadowWarm.copy(alpha = 0.1f),
            )
            .background(Background)
            .background(NavGlass)
            .drawBehind {
                drawLine(Color.White.copy(alpha = 0.85f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .navigationBarsPadding()
            .padding(start = 11.dp, end = 11.dp, top = 6.dp, bottom = 8.dp),
    ) {
        RoomieTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(shape)
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                Tan.copy(alpha = 0.17f),
                            ).border(WhiteBorderWidth, Color.White.copy(alpha = 0.8f), shape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(role = Role.Tab, onClickLabel = stringResource(tab.label)) { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (isSelected) tab.activeIcon else tab.icon),
                    contentDescription = stringResource(tab.label),
                    modifier = Modifier.size(tab.iconSize),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}
