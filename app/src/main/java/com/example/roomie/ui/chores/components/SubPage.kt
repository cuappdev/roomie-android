package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType

/** Scrolling cream page for the chore sub-pages (details, rotations). */
@Composable
internal fun SubPage(spacing: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

@Preview(widthDp = 390, heightDp = 300)
@Composable
private fun SubPagePreview() = RoomiePreview {
    SubPage {
        BackLink("Back to chores", {})
        Text("Take out recycling", style = RoomieType.greeting)
    }
}
