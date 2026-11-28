package com.example.roomie.ui.onboarding.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.R
import com.example.roomie.ui.components.IconAction
import com.example.roomie.ui.components.PrimaryButton
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.Divider
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType

private const val STEPS = 4

/** Back arrow, 4-step progress, title and a bottom-pinned primary button. */
@Composable
internal fun OnboardingScaffold(
    step: Int?,
    title: String,
    subtitle: String?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    titleStyle: TextStyle = RoomieType.onboardingTitle,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 7.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().height(44.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Box(Modifier.width(34.dp)) {
                    IconAction(R.drawable.ic_back, stringResource(R.string.back), onBack, Modifier.offset(x = (-10).dp))
                }
            }
            if (step != null) StepProgress(step, Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = titleStyle, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = RoomieType.onboardingSubtitle)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
        bottom()
    }
}

@Composable
private fun StepProgress(step: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.cd_onboarding_step, step, STEPS)
    Row(
        modifier.semantics {
            contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(step.toFloat(), 0f..STEPS.toFloat(), STEPS)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(STEPS) { i ->
            Box(
                Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(
                    if (i <
                        step
                    ) {
                        BrandBrown
                    } else {
                        Divider
                    },
                ),
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 500)
@Composable
private fun OnboardingScaffoldPreview() = RoomiePreview {
    OnboardingScaffold(
        step = 2,
        title = "Hi Leah,",
        subtitle = "How are you setting up?",
        onBack = {},
        bottom = { PrimaryButton(stringResource(R.string.next), {}, gradient = true) },
    ) {}
}
