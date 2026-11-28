package com.example.roomie.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.components.ErrorText
import com.example.roomie.ui.components.InviteCodeCard
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.RoommateRow
import com.example.roomie.ui.components.ShareInviteButton
import com.example.roomie.ui.onboarding.components.OnboardingScaffold
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.util.asString

@Composable
fun SetUpHouseScreen(onEnter: () -> Unit, onBack: () -> Unit, vm: SetUpHouseViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    SetUpHouseContent(
        ui = ui ?: return,
        onBack = onBack,
        onNameChange = vm::setName,
        onEnter = { vm.finish(onEnter) },
    )
}

@Composable
private fun SetUpHouseContent(
    ui: SetUpHouseUiState,
    onBack: () -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onEnter: () -> Unit = {},
) {
    OnboardingScaffold(
        step = 4,
        title = stringResource(R.string.set_up_house_title),
        subtitle = null,
        onBack = onBack,
        titleStyle = RoomieType.onboardingTitle.copy(letterSpacing = 0.sp),
        bottom = {
            PrimaryButton(
                stringResource(if (ui.busy) R.string.set_up_house_busy else R.string.set_up_house_enter),
                onEnter,
                enabled = ui.canFinish,
                gradient = true,
            )
        },
    ) {
        LabeledField(stringResource(R.string.set_up_house_name), labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = ui.name,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.set_up_house_name_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                contentDescription = stringResource(R.string.me_house_name),
            )
        }
        ErrorText(ui.error?.asString())
        if (ui.inviteCode.isNotBlank()) {
            InviteCodeCard(ui.inviteCode, glass = true)
            ShareInviteButton(ui.houseName, ui.inviteCode, glass = true)
        } else {
            // The server makes the code when the house is created, on the button below.
            Text(stringResource(R.string.set_up_house_code_later), style = RoomieType.bodySecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.set_up_house_roommates),
                style = RoomieType.sectionTitle,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ui.members.forEach { roommate ->
                RoommateRow(
                    roommate,
                    isMe = roommate.id == ui.meId,
                    nameStyle = RoomieType.listTitle.copy(fontSize = 16.sp, lineHeight = 20.sp),
                ) {
                    MemberStatusPill(roommate.status)
                }
            }
            if (!ui.hasRoommates) {
                Text(stringResource(R.string.set_up_house_no_roommates), style = RoomieType.bodySecondary)
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 900)
@Composable
private fun SetUpHousePreview() = RoomiePreview {
    SetUpHouseContent(setUpHouseUiState(FakeRoomieRepository.demoState(), null)!!)
}
