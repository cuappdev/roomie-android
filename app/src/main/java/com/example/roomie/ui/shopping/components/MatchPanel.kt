package com.example.roomie.ui.shopping.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.components.RoomieCard
import com.example.roomie.ui.shopping.MatchItem
import com.example.roomie.ui.shopping.shoppingUiState
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.PeachPill
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.util.agoLabel
import java.time.LocalDateTime

/**
 * While typing a new item: what's already on the list ("Make it ×3"), what was bought lately ("Add
 * again"), and a row to add it as new anyway.
 */
@Composable
internal fun MatchPanel(
    query: String,
    matches: List<MatchItem>,
    now: LocalDateTime,
    onMakeMore: (String) -> Unit,
    onAddAgain: (String) -> Unit,
    onAddNew: () -> Unit,
) {
    RoomieCard {
        matches.forEach { match ->
            val item = match.item
            when (match) {
                is MatchItem.OnList -> Row(
                    Modifier.fillMaxWidth().heightIn(
                        min = 60.dp,
                    ).background(PeachPill).padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = RoomieType.listTitle.copy(fontWeight = FontWeight.SemiBold))
                        Text(
                            stringResource(R.string.shopping_already_on_list, item.quantity),
                            style = RoomieType.caption.copy(color = Danger, fontWeight = FontWeight.SemiBold),
                        )
                    }
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface)
                            .border(1.dp, BrandBrown, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Button) { onMakeMore(item.id) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.shopping_make_it, item.quantity + 1),
                            style = RoomieType.pillButton.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                is MatchItem.RecentlyBought -> Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = RoomieType.listTitle)
                        Text(
                            stringResource(
                                R.string.shopping_bought_ago_by,
                                match.at.agoLabel(now),
                                match.by.label(capitalized = false),
                            ),
                            style = RoomieType.caption,
                        )
                    }
                    Box(
                        Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { onAddAgain(item.name) }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.shopping_add_again),
                            style = RoomieType.pillButton.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
            HorizontalDivider(color = Divider, thickness = 1.dp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onAddNew)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_plus), null, Modifier.size(18.dp), tint = Color.Unspecified)
            Text(
                stringResource(R.string.shopping_add_as_new, query),
                style = RoomieType.listTitle.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}

@Preview
@Composable
private fun MatchPanelPreview() = RoomiePreview(padding = 16.dp) {
    val state = FakeRoomieRepository.demoState()
    val now = LocalDateTime.now()
    // A one-letter query matches a bit of everything in the demo list.
    val ui = shoppingUiState(state, "a", null, null, now)!!
    MatchPanel("a", ui.matches.take(3), now, onMakeMore = {}, onAddAgain = {}, onAddNew = {})
}
