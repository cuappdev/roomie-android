package com.example.roomie.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.MemberStatus
import com.example.roomie.data.RoomieState
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.InviteCodeCard
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RoommateRow
import com.example.roomie.ui.components.ShareInviteButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.BrownOutline
import com.example.roomie.ui.theme.GlassLight
import com.example.roomie.ui.theme.GlassOnWhite
import com.example.roomie.ui.theme.IdentityButton
import com.example.roomie.ui.theme.IdentityTint
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TanOutline
import com.example.roomie.ui.theme.Danger
import com.example.roomie.ui.theme.RoomieType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MeViewModel(private val repo: FakeRoomieRepository = FakeRoomieRepository) : ViewModel() {
    val state: StateFlow<RoomieState> = repo.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.state.value)
    fun renameHouse(name: String) = repo.renameHouse(name)
    fun signOut() = repo.signOut()
    fun leaveHouse() = repo.leaveHouse()
}

@Composable
fun MeScreen(onChangePic: () -> Unit, onSignedOut: () -> Unit, onLeftHouse: () -> Unit, vm: MeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val session = state.session ?: return
    val house = state.house ?: return
    var renaming by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Background)) {
        RoomieHeader("Your Space", background = Background, titleStyle = RoomieType.greeting, height = 76.dp)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Identity
            val identityShape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, identityShape, ambientColor = ShadowWarm.copy(alpha = 0.13f), spotColor = ShadowWarm.copy(alpha = 0.13f))
                    .clip(identityShape)
                    .background(IdentityTint)
                    .border(1.dp, TanOutline, identityShape)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuddyAvatar(session.buddy, 64.dp, contentDescription = "Your buddy: ${session.buddy.label}")
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        listOf(session.firstName, session.lastName).filter { it.isNotBlank() }.joinToString(" "),
                        style = RoomieType.cardTitle.copy(lineHeight = 25.sp),
                        maxLines = 1,
                    )
                    Text("${session.netId} · Your profile", style = RoomieType.choreMeta, maxLines = 1)
                }
                OutlineButton(
                    Modifier.size(40.dp),
                    fill = IdentityButton,
                    onClickLabel = "Change pic",
                    onClick = onChangePic,
                ) {
                    Icon(painterResource(R.drawable.ic_pen), "Change pic", Modifier.size(20.dp), tint = Color.Unspecified)
                }
            }

            // Household
            val houseShape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, houseShape, ambientColor = ShadowWarm.copy(alpha = 0.08f), spotColor = ShadowWarm.copy(alpha = 0.08f))
                    .clip(houseShape)
                    .background(Surface)
                    .border(1.dp, Border, houseShape)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(house.name, style = RoomieType.cardTitle.copy(color = BrandBrown), maxLines = 1, modifier = Modifier.semantics { heading() })
                        val count = house.members.size
                        Text("$count roommate${if (count == 1) "" else "s"} · Your household", style = RoomieType.choreMeta)
                    }
                    OutlineButton(
                        Modifier.size(width = 60.dp, height = 36.dp),
                        fill = GlassOnWhite,
                        onClickLabel = "Rename house",
                        onClick = { renaming = true },
                    ) {
                        Text("Edit", style = RoomieType.smallLink)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    house.members.forEach { m ->
                        RoommateRow(m, isMe = m.id == session.userId, nameStyle = RoomieType.listTitle.copy(fontSize = 16.sp, lineHeight = 20.sp)) {
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

            // Account actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountButton("Sign Out", BrandBrown, GlassLight, Modifier.weight(1f)) { vm.signOut(); onSignedOut() }
                AccountButton("Leave House", Danger, Background, Modifier.weight(1f)) { confirmLeave = true }
            }
        }
    }

    if (renaming) {
        var name by remember { mutableStateOf(house.name) }
        AlertDialog(
            onDismissRequest = { renaming = false },
            containerColor = Background,
            title = { Text("Rename house", style = RoomieType.sheetTitle) },
            text = { RoomieTextField(name, { name = it }, placeholder = "House name", contentDescription = "House name") },
            confirmButton = { TextAction("Save", { vm.renameHouse(name); renaming = false }, enabled = name.isNotBlank()) },
            dismissButton = { TextAction("Cancel", { renaming = false }) },
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = Background,
            title = { Text("Leave ${house.name}?", style = RoomieType.sheetTitle) },
            text = { Text("You'll lose access to the house's notes, chores and shopping list. A roommate can invite you back.", style = RoomieType.body) },
            confirmButton = { TextAction("Leave", { vm.leaveHouse(); confirmLeave = false; onLeftHouse() }, style = RoomieType.linkButton.copy(color = Danger)) },
            dismissButton = { TextAction("Stay", { confirmLeave = false }) },
        )
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
            .shadow(2.dp, shape, ambientColor = ShadowWarm.copy(alpha = 0.08f), spotColor = ShadowWarm.copy(alpha = 0.08f))
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
