package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.Roommate
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TextPrimary
import com.example.roomie.ui.util.monthDayLabel
import java.time.LocalDate

/** "Rotation Preview": who has the chore for each of the listed weeks. */
@Composable
fun RotationPreview(weeks: List<Pair<LocalDate, List<Roommate>>>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                4.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.1f),
                spotColor = ShadowWarm.copy(alpha = 0.1f),
            )
            .clip(shape)
            .background(Surface)
            .border(0.7.dp, Border, shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.rotation_preview), style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.rotation_preview_week_of),
                style = RoomieType.choreMeta,
                modifier = Modifier.width(62.dp),
            )
            Text(stringResource(R.string.rotation_preview_assigned_to), style = RoomieType.choreMeta)
        }
        weeks.forEach { (week, people) ->
            Row(
                Modifier.heightIn(min = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    week.monthDayLabel(),
                    style = RoomieType.choreMeta.copy(color = TextPrimary, fontWeight = FontWeight.Medium),
                    modifier = Modifier.width(62.dp),
                )
                people.forEach { p ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BuddyAvatar(p.buddy, 20.dp)
                        Text(p.firstName, style = RoomieType.choreMeta)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun RotationPreviewPreview() = RoomiePreview(padding = 16.dp) {
    val members = FakeRoomieRepository.demoState().house!!.members
    val week = LocalDate.now()
    RotationPreview((0L..2L).map { week.plusWeeks(it) to listOf(members[it.toInt() % members.size]) })
}
