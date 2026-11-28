package com.example.roomie.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.ui.components.ErrorText
import com.example.roomie.ui.components.FieldLabel
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.components.glassSurface
import com.example.roomie.ui.onboarding.components.OnboardingScaffold
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.Surface
import com.example.roomie.ui.theme.TabIcon
import com.example.roomie.ui.util.UiText
import com.example.roomie.ui.util.asString

@Composable
fun SetupChoiceScreen(
    onStartHouse: () -> Unit,
    onJoined: () -> Unit,
    onBack: (() -> Unit)?,
    vm: SetupChoiceViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    SetupChoiceContent(
        ui = ui ?: return,
        onBack = onBack,
        onChooseStart = vm::chooseStart,
        onChooseJoin = vm::chooseJoin,
        onCodeChange = vm::setCode,
        onNext = { vm.next(onStartHouse, onJoined) },
    )
}

@Composable
private fun SetupChoiceContent(
    ui: SetupChoiceUiState,
    onBack: (() -> Unit)? = null,
    onChooseStart: () -> Unit = {},
    onChooseJoin: () -> Unit = {},
    onCodeChange: (String) -> Unit = {},
    onNext: () -> Unit = {},
) {
    OnboardingScaffold(
        step = 2,
        title = stringResource(R.string.setup_title, ui.firstName),
        subtitle = stringResource(R.string.setup_subtitle),
        onBack = onBack,
        bottom = { PrimaryButton(stringResource(R.string.next), onNext, enabled = !ui.busy, gradient = true) },
    ) {
        ChoiceCard(
            selected = !ui.join,
            title = stringResource(R.string.setup_start_title),
            body = stringResource(R.string.setup_start_body),
            onSelect = onChooseStart,
        )
        ChoiceCard(
            selected = ui.join,
            title = stringResource(R.string.setup_join_title),
            body = stringResource(R.string.setup_join_body),
            onSelect = onChooseJoin,
        ) {
            Column(Modifier.padding(start = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLabel(stringResource(R.string.invite_code), style = RoomieType.onboardingLabel)
                RoomieTextField(
                    value = ui.code,
                    onValueChange = onCodeChange,
                    placeholder = stringResource(R.string.setup_code_placeholder),
                    background = Background,
                    textStyle = RoomieType.input.copy(letterSpacing = 0.96.sp),
                    isError = ui.error != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    contentDescription = stringResource(R.string.cd_setup_code),
                )
                ErrorText(ui.error?.asString())
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    title: String,
    body: String,
    onSelect: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(shape)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(22.dp)
                    .border(1.dp, if (selected) BrandBrown else TabIcon, CircleShape)
                    .background(Surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(13.2.dp).background(BrandBrown, CircleShape))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.3.dp)) {
                // Figma centers text in its line box without trimming; match that so padding reads the same.
                val figmaLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
                Text(
                    title,
                    style = RoomieType.sectionTitle.copy(
                        fontSize = 17.sp,
                        lineHeight = 21.sp,
                        lineHeightStyle = figmaLines,
                    ),
                )
                Text(
                    body,
                    style = RoomieType.onboardingBody.copy(
                        fontSize = 14.sp,
                        lineHeight = 19.6.sp,
                        lineHeightStyle = figmaLines,
                    ),
                )
            }
        }
        extra?.invoke()
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SetupChoicePreview() = RoomiePreview {
    SetupChoiceContent(SetupChoiceUiState("Leah"))
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SetupChoiceJoinPreview() = RoomiePreview {
    SetupChoiceContent(
        SetupChoiceUiState(
            "Leah",
            join = true,
            code = "LOFT-12",
            error = UiText.Raw("We couldn't find a house with that code."),
        ),
    )
}
