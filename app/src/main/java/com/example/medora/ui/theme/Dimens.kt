package com.example.medora.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// One place for every size in the app. Use these, not raw dp numbers.
// Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 (see Spacing in MedoraSpacing.kt).
object Dimens {
    // Touch targets (Material: at least 48dp).
    val touch = 48.dp
    val buttonHeight = 52.dp

    // Screen edges and gaps between blocks.
    val screenPadding = 16.dp
    val blockGap = 12.dp
    val sectionGap = 24.dp

    // Bars.
    val topBarHeight = 56.dp
    val topBarTop = 8.dp
    // Space the content needs under the top bar (bar + its top gap).
    val topBarSpace = 64.dp
    val bottomBarHeight = 64.dp
    val bottomBarSpace = 88.dp

    // Round button inside bars (back, settings).
    val roundButton = 44.dp

    // Icons.
    val iconS = 20.dp
    val iconM = 24.dp
    val iconL = 28.dp

    // Corners. All soft.
    val radiusCard = 24.dp
    val radiusField = 16.dp
}

object MedoraShapesSet {
    val card = RoundedCornerShape(Dimens.radiusCard)
    val field = RoundedCornerShape(Dimens.radiusField)
}
