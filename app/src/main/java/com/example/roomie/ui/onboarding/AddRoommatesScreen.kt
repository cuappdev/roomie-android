package com.example.roomie.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.components.BuddyAvatar
import com.example.roomie.ui.components.ErrorText
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.InlineAddButton
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.MemberStatusPill
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.glassSurface
import com.example.roomie.ui.onboarding.components.OnboardingScaffold
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.util.asString

@Composable
fun AddRoommatesScreen(onNext: () -> Unit, onBack: () -> Unit, vm: AddRoommatesViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    AddRoommatesContent(
        ui = ui ?: return,
        onBack = onBack,
        onNext = onNext,
        onNetIdChange = vm::setNetId,
        onInvite = vm::invite,
        onRemove = vm::remove,
    )
}

@Composable
private fun AddRoommatesContent(
    ui: AddRoommatesUiState,
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    onNetIdChange: (String) -> Unit = {},
    onInvite: () -> Unit = {},
    onRemove: (String) -> Unit = {},
) {
    OnboardingScaffold(
        step = 3,
        title = stringResource(R.string.add_roommates_title),
        subtitle = stringResource(R.string.add_roommates_subtitle),
        onBack = onBack,
        bottom = { PrimaryButton(stringResource(R.string.next), onNext, gradient = true) },
    ) {
        ui.roommates.forEach { roommate ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(RoundedCornerShape(14.dp))
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BuddyAvatar(roommate.buddy, 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(roommate.firstName, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
                    Text(roommate.netId, style = RoomieType.onboardingNote)
                }
                MemberStatusPill(roommate.status, pendingLabel = stringResource(R.string.status_invite_sent))
                IconAction(
                    R.drawable.ic_close,
                    stringResource(R.string.cd_add_roommates_remove, roommate.firstName),
                    { onRemove(roommate.id) },
                    iconSize = 18.dp,
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            LabeledField(
                stringResource(R.string.add_roommates_net_id),
                Modifier.weight(1f),
                labelStyle = RoomieType.onboardingLabel,
            ) {
                RoomieTextField(
                    value = ui.netId,
                    onValueChange = onNetIdChange,
                    placeholder = stringResource(R.string.add_roommates_net_id_placeholder),
                    isError = ui.error != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onInvite() }),
                    contentDescription = stringResource(R.string.add_roommates_net_id),
                )
            }
            InlineAddButton(
                onInvite,
                height = 50.dp,
                corner = 12.dp,
                enabled = ui.netId.isNotBlank() && !ui.busy,
                gradient = true,
                textStyle = RoomieType.primaryButton.copy(lineHeight = 20.sp),
            )
        }
        ErrorText(ui.error?.asString())
        Text(stringResource(R.string.add_roommates_note), style = RoomieType.onboardingNote)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AddRoommatesPreview() = RoomiePreview {
    AddRoommatesContent(AddRoommatesUiState(roommatesOf(FakeRoomieRepository.demoState())!!.take(2), netId = "ab123"))
}
