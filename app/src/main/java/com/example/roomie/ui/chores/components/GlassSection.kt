package com.example.roomie.ui.chores.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.ui.theme.Background
import com.example.roomie.ui.theme.BrandBrown
import com.example.roomie.ui.theme.RoomiePreview
import com.example.roomie.ui.theme.RoomieType
import com.example.roomie.ui.theme.SectionBorder
import com.example.roomie.ui.theme.SectionEnd
import com.example.roomie.ui.theme.SectionStart
import com.example.roomie.ui.theme.ShadowWarm

/** Translucent form section with a brown title ("Chore Details", "Assignment", "People & Schedule"). */
@Composable
fun GlassSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(
                4.dp,
                shape,
                ambientColor = ShadowWarm.copy(alpha = 0.08f),
                spotColor = ShadowWarm.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(Background)
            .background(Brush.horizontalGradient(listOf(SectionStart, SectionEnd)))
            .border(1.dp, SectionBorder, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = RoomieType.sectionTitle.copy(color = BrandBrown, lineHeight = 20.sp))
        content()
    }
}

/** 12sp label above a chores form control. */
@Composable
fun ChoreField(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = RoomieType.onboardingLabel)
        content()
    }
}

@Preview
@Composable
private fun GlassSectionPreview() = RoomiePreview(padding = 16.dp) {
    GlassSection("Chore Details") {
        ChoreField("Chore Name") { Text("Take out recycling", style = RoomieType.choreMeta) }
    }
}
