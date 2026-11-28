package com.example.roomie.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.MemberStatus
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.InviteCodeCard
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RoommateRow
import com.example.roomie.ui.components.ShareInviteButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.components.displayName
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.BrownOutline
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.GlassLight
import com.example.roomie.ui.theme.GlassOnWhite
import com.example.roomie.ui.theme.IdentityButton
import com.example.roomie.ui.theme.IdentityTint
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TanOutline

@Composable
fun MeScreen(
    onChangePic: () -> Unit,
    onSignedOut: () -> Unit,
    onLeftHouse: () -> Unit,
    vm: MeViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    MeContent(
        ui = ui ?: return,
        onChangePic = onChangePic,
        onRename = vm::renameHouse,
        onSignOut = {
            vm.signOut()
            onSignedOut()
        },
        onLeave = {
            vm.leaveHouse()
            onLeftHouse()
        },
    )
}

@Composable
private fun MeContent(
    ui: MeUiState,
    onChangePic: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
    onLeave: () -> Unit = {},
) {
    var renaming by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val house = ui.house

    // The whole page scrolls, header included; only the nav bar stays put.
    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        RoomieHeader(
            stringResource(R.string.me_title),
            background = Background,
            titleStyle = RoomieType.greeting,
            height = 76.dp,
        )
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IdentityCard(ui, onChangePic)
            HouseholdCard(ui) { renaming = true }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountButton(
                    stringResource(R.string.me_sign_out),
                    BrandBrown,
                    GlassLight,
                    Modifier.weight(1f),
                    onSignOut,
                )
                AccountButton(stringResource(R.string.me_leave_house), Danger, Background, Modifier.weight(1f)) {
                    confirmLeave =
                        true
                }
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(house.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            containerColor = Background,
            title = { Text(stringResource(R.string.me_rename_house), style = RoomieType.sheetTitle) },
            text = {
                RoomieTextField(
                    name,
                    { name = it },
                    placeholder = stringResource(R.string.me_house_name),
                    contentDescription = stringResource(R.string.me_house_name),
                )
            },
            confirmButton = {
                TextAction(
                    stringResource(R.string.save),
                    {
                        onRename(name)
                        renaming = false
                    },
                    enabled = name.isNotBlank(),
                )
            },
            dismissButton = { TextAction(stringResource(R.string.cancel), { renaming = false }) },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = Background,
            title = { Text(stringResource(R.string.me_leave_title, house.name), style = RoomieType.sheetTitle) },
            text = { Text(stringResource(R.string.me_leave_body), style = RoomieType.body) },
            confirmButton = {
                TextAction(
                    stringResource(R.string.me_leave),
                    {
                        confirmLeave = false
                        onLeave()
                    },
                    style = RoomieType.linkButton.copy(color = Danger),
                )
            },
            dismissButton = { TextAction(stringResource(R.string.me_stay), { confirmLeave = false }) },
        )
    }
}

@Composable
private fun IdentityCard(ui: MeUiState, onChangePic: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(
                6.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.13f),
                spotColor = ShadowWarm.copy(alpha = 0.13f),
            )
            .clip(shape)
            .background(IdentityTint)
            .border(1.dp, TanOutline, shape)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BuddyAvatar(
            ui.buddy,
            64.dp,
            contentDescription = stringResource(R.string.cd_your_buddy, ui.buddy.displayName()),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(ui.fullName, style = RoomieType.cardTitle.copy(lineHeight = 25.sp), maxLines = 1)
            Text(stringResource(R.string.me_profile, ui.netId), style = RoomieType.choreMeta, maxLines = 1)
        }
        val changePic = stringResource(R.string.cd_me_change_pic)
        OutlineButton(Modifier.size(40.dp), fill = IdentityButton, onClickLabel = changePic, onClick = onChangePic) {
            Icon(painterResource(R.drawable.ic_pen), changePic, Modifier.size(20.dp), tint = Color.Unspecified)
        }
    }
}

@Composable
private fun HouseholdCard(ui: MeUiState, onRename: () -> Unit) {
    val house = ui.house
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                5.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.08f),
                spotColor = ShadowWarm.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(Surface)
            .border(1.dp, Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    house.name,
                    style = RoomieType.cardTitle.copy(color = BrandBrown),
                    maxLines = 1,
                    modifier = Modifier.semantics { heading() },
                )
                val count = house.members.size
                Text(pluralStringResource(R.plurals.me_household, count, count), style = RoomieType.choreMeta)
            }
            OutlineButton(
                Modifier.size(width = 60.dp, height = 36.dp),
                fill = GlassOnWhite,
                onClickLabel = stringResource(R.string.me_rename_house),
                onClick = onRename,
            ) {
                Text(stringResource(R.string.me_edit), style = RoomieType.smallLink)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            house.members.forEach { m ->
                RoommateRow(
                    m,
                    isMe = m.id == ui.meId,
                    nameStyle = RoomieType.listTitle.copy(fontSize = 16.sp, lineHeight = 20.sp),
                ) {
                    when (m.status) {
                        MemberStatus.Owner -> MemberStatusPill(m.status, ownerStyle = false)
                        MemberStatus.InviteSent -> MemberStatusPill(m.status)
                        MemberStatus.Joined -> Unit
                    }
                }
            }
        }
        InviteCodeCard(house.inviteCode, corner = 12.dp)
        ShareInviteButton(house.name, house.inviteCode, height = 40.dp)
    }
}

/** Small rounded glass button with a translucent brown outline (pen and Edit on the profile cards). */
@Composable
private fun OutlineButton(
    modifier: Modifier,
    fill: Color,
    onClickLabel: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .shadow(
                2.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.08f),
                spotColor = ShadowWarm.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(fill)
            .border(1.dp, BrownOutline, shape)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun AccountButton(text: String, color: Color, fill: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .height(42.dp)
            .clip(shape)
            .background(fill)
            .border(1.dp, Border, shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = RoomieType.glassButton.copy(color = color))
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun MeContentPreview() = RoomiePreview {
    MeContent(meUiState(FakeRoomieRepository.demoState())!!)
}
