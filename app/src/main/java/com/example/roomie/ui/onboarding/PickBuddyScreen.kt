package com.example.roomie.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.Buddy
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.components.displayName
import com.example.roomie.ui.onboarding.components.OnboardingScaffold
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType

@Composable
fun PickBuddyScreen(
    editing: Boolean,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
    vm: PickBuddyViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    PickBuddyContent(
        ui = ui ?: return,
        editing = editing,
        onBack = onBack,
        onPick = vm::pick,
        onSave = {
            vm.save()
            onNext()
        },
        onSkip = {
            vm.skip()
            onNext()
        },
    )
}

@Composable
private fun PickBuddyContent(
    ui: PickBuddyUiState,
    editing: Boolean,
    onBack: (() -> Unit)? = null,
    onPick: (Buddy) -> Unit = {},
    onSave: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    OnboardingScaffold(
        step = if (editing) null else 1,
        title = stringResource(R.string.buddy_title),
        subtitle = stringResource(R.string.buddy_subtitle),
        onBack = onBack,
        titleStyle = RoomieType.onboardingTitle.copy(fontWeight = FontWeight.Bold, lineHeight = 32.sp),
        bottom = {
            PrimaryButton(stringResource(if (editing) R.string.save else R.string.next), onSave, gradient = true)
            if (!editing) {
                TextAction(
                    stringResource(R.string.buddy_skip),
                    onSkip,
                    Modifier.align(Alignment.CenterHorizontally).offset(y = (-6).dp),
                    style = RoomieType.linkButton.copy(fontSize = 16.sp, lineHeight = 20.sp),
                )
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BuddyAvatar(
                ui.picked,
                88.dp,
                contentDescription = stringResource(R.string.cd_your_buddy, ui.picked.displayName()),
            )
            Text(ui.firstName, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(Buddy.entries.take(3), Buddy.entries.drop(3)).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(19.dp, Alignment.CenterHorizontally),
                ) {
                    row.forEach { buddy -> BuddyOption(buddy, buddy == ui.picked) { onPick(buddy) } }
                }
            }
        }
    }
}

@Composable
private fun BuddyOption(buddy: Buddy, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .width(104.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(88.dp)) {
            Box(
                Modifier
                    .size(88.dp)
                    .border(2.dp, if (selected) BrandBrown else Color.Transparent, CircleShape)
                    .padding(6.dp),
            ) {
                BuddyAvatar(buddy, 76.dp)
            }
            // Sibling of the ring so the ring's border never paints over the check.
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-5).dp, y = (-5).dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(BrandBrown)
                        .border(2.dp, Background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_check_small),
                        null,
                        Modifier.size(14.dp),
                        tint = Color.Unspecified,
                    )
                }
            }
        }
        Text(
            buddy.displayName(),
            style = RoomieType.rowTitle.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PickBuddyPreview() = RoomiePreview {
    PickBuddyContent(PickBuddyUiState("Leah", Buddy.Plant), editing = false)
}
