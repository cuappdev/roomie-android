package com.example.roomie.ui.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.data.FakeRoomieRepository
import com.example.roomie.ui.chores.components.BackLink
import com.example.roomie.ui.chores.components.BrownHeaderCard
import com.example.roomie.ui.chores.components.SubPage
import com.example.roomie.ui.chores.components.TanButton
import com.example.roomie.ui.chores.components.peopleLabel
import com.example.roomie.ui.theme.Border
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.ShadowWarm
import com.example.roomie.ui.theme.SuccessText
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.util.monthDayLabel
import java.time.LocalDate

/** "Manage Rotations": every rotation in the house with who's next. */
@Composable
fun ManageRotationsScreen(
    onBack: () -> Unit,
    onOpenRotation: (String) -> Unit,
    vm: ManageRotationsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    ManageRotationsContent(ui, onBack, onOpenRotation)
}

@Composable
private fun ManageRotationsContent(
    ui: ManageRotationsUiState,
    onBack: () -> Unit = {},
    onOpenRotation: (String) -> Unit = {},
) {
    SubPage(spacing = 12) {
        BackLink(stringResource(R.string.chores_back_to_chores), onBack)
        BrownHeaderCard(
            stringResource(R.string.chores_manage_rotations),
            stringResource(R.string.manage_rotations_subtitle),
        )
        if (ui.rotations.isEmpty()) {
            Text(stringResource(R.string.manage_rotations_empty), style = RoomieType.choreMeta)
        }
        ui.rotations.forEach { RotationCard(it) { onOpenRotation(it.id) } }
    }
}

@Composable
private fun RotationCard(rotation: RotationSummary, onOpen: () -> Unit) {
    val separator = stringResource(R.string.names_separator)
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                5.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.11f),
                spotColor = ShadowWarm.copy(alpha = 0.11f),
            )
            .clip(shape)
            .background(Surface)
            .border(0.7.dp, Border, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(rotation.title, style = RoomieType.sectionTitle.copy(lineHeight = 20.sp))
        Text(
            stringResource(
                R.string.manage_rotations_members,
                rotation.memberNames.joinToString(separator),
                peopleLabel(rotation.peoplePerWeek),
            ),
            style = RoomieType.choreMeta,
        )
        Text(
            stringResource(
                R.string.manage_rotations_next,
                rotation.nextNames.joinToString(separator),
                rotation.nextDue.monthDayLabel(),
            ),
            style = RoomieType.onboardingLabel.copy(color = SuccessText),
        )
        TanButton(stringResource(R.string.manage_rotations_view_edit), onOpen)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ManageRotationsPreview() = RoomiePreview {
    ManageRotationsContent(manageRotationsUiState(FakeRoomieRepository.demoState(), LocalDate.now()))
}

@Preview(widthDp = 390, heightDp = 400)
@Composable
private fun ManageRotationsEmptyPreview() = RoomiePreview {
    ManageRotationsContent(ManageRotationsUiState(emptyList()))
}
