package com.example.roomie.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.roomie.R
import com.example.roomie.ui.components.ErrorText
import com.example.roomie.ui.components.LabeledField
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.components.RoomieTextField
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.TextSecondary
import com.example.roomie.ui.util.UiText
import com.example.roomie.ui.util.asString

@Composable
fun SignInScreen(onSignedIn: (hasHouse: Boolean) -> Unit, vm: SignInViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    SignInContent(
        ui = ui,
        onNetIdChange = vm::setNetId,
        onPasswordChange = vm::setPassword,
        onSubmit = { vm.signIn(onSignedIn) },
    )
}

@Composable
private fun SignInContent(
    ui: SignInUiState,
    onNetIdChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 43.dp, bottom = 20.dp),
    ) {
        Image(painterResource(R.drawable.ic_brand_mark), null, Modifier.size(64.dp))
        Spacer(Modifier.height(15.dp))
        Text(
            stringResource(R.string.sign_in_welcome),
            style = RoomieType.welcomeTitle,
            modifier = Modifier.semantics {
                heading()
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sign_in_subtitle), style = RoomieType.input.copy(color = TextSecondary))
        Spacer(Modifier.height(18.dp))
        LabeledField(stringResource(R.string.sign_in_net_id), labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = ui.netId,
                onValueChange = onNetIdChange,
                placeholder = stringResource(R.string.sign_in_net_id_placeholder),
                suffix = stringResource(R.string.sign_in_net_id_suffix),
                suffixStyle = RoomieType.input.copy(color = TextSecondary, lineHeight = 20.sp),
                isError = ui.error != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false,
                ),
                contentDescription = stringResource(R.string.cd_sign_in_net_id),
            )
        }
        Spacer(Modifier.height(16.dp))
        LabeledField(stringResource(R.string.sign_in_password), labelStyle = RoomieType.onboardingLabel) {
            RoomieTextField(
                value = ui.password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(R.string.sign_in_password),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                contentDescription = stringResource(R.string.sign_in_password),
            )
        }
        ErrorText(ui.error?.asString(), Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            stringResource(if (ui.busy) R.string.sign_in_busy else R.string.sign_in),
            onSubmit,
            enabled = !ui.busy,
            gradient = true,
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SignInPreview() = RoomiePreview {
    SignInContent(SignInUiState(netId = "jw2877"))
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SignInErrorPreview() = RoomiePreview {
    SignInContent(SignInUiState(netId = "jw", error = UiText.Res(R.string.sign_in_bad_net_id)))
}
