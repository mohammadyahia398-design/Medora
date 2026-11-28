package com.example.medora.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.medora.ui.icons.themed
import com.example.medora.ui.theme.Dimens

// Flat top bar: no capsule behind the title. The title is centered.
// Back (or nothing) on the start side, one optional round button on the end side.
@Composable
fun MedoraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    startIcon: ImageVector? = null,
    startDescription: String? = null,
    onStart: () -> Unit = {},
    endIcon: ImageVector? = null,
    endDescription: String? = null,
    onEnd: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Dimens.screenPadding, vertical = 0.dp)
            .padding(top = Dimens.topBarTop)
            .height(Dimens.topBarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(Dimens.touch), contentAlignment = Alignment.Center) {
            if (startIcon != null) {
                RoundIconButton(startIcon, startDescription, onStart)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.size(Dimens.touch), contentAlignment = Alignment.Center) {
            if (endIcon != null) {
                RoundIconButton(endIcon, endDescription, onEnd)
            }
        }
    }
}

// Soft round button for icons. White circle, no shadow.
@Composable
fun RoundIconButton(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Dimens.roundButton)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = icon.themed(),
            contentDescription = description,
            modifier = Modifier.size(Dimens.iconM),
        )
    }
}
