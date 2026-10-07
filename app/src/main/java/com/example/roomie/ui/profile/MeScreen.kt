package com.example.roomie.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.data.MemberStatus
import com.example.roomie.data.RoomieState
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.DangerButton
import com.example.roomie.ui.components.InviteCodeCard
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.RoomieCard
import com.example.roomie.ui.components.RoomieHeader
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RoommateRow
import com.example.roomie.ui.components.SecondaryButton
import com.example.roomie.ui.components.ShareInviteButton
import com.example.roomie.ui.components.TextAction
import com.example.roomie.ui.theme.Background
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
        RoomieHeader("Me")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RoomieCard {
                Row(
                    Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BuddyAvatar(session.buddy, 56.dp, contentDescription = "Your buddy: ${session.buddy.label}")
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(listOf(session.firstName, session.lastName).filter { it.isNotBlank() }.joinToString(" "), style = RoomieType.sectionTitle.copy(fontSize = 18.sp))
                        Text(session.netId, style = RoomieType.bodySecondary)
                    }
                    TextAction("Change pic", onChangePic)
                }
            }
            RoomieCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(house.name, style = RoomieType.sectionTitle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = com.example.roomie.ui.theme.BrandBrown), modifier = Modifier.semantics { heading() })
                            val count = house.members.size
                            Text("$count roommate${if (count == 1) "" else "s"}", style = RoomieType.bodySecondary)
                        }
                        TextAction("Edit", { renaming = true })
                    }
                    house.members.forEach { m ->
                        RoommateRow(m, isMe = m.id == session.userId) {
                            when (m.status) {
                                MemberStatus.Owner -> MemberStatusPill(m.status, ownerStyle = false)
                                MemberStatus.InviteSent -> MemberStatusPill(m.status)
                                MemberStatus.Joined -> Unit
                            }
                        }
                    }
                    InviteCodeCard(house.inviteCode)
                    ShareInviteButton(house.name, house.inviteCode)
                }
            }
            SecondaryButton("Sign out", { vm.signOut(); onSignedOut() })
            DangerButton("Leave house", { confirmLeave = true })
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
