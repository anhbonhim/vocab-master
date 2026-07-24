package com.nhimz.vocabmaster.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nhimz.vocabmaster.ui.theme.ErrorRed
import com.nhimz.vocabmaster.ui.theme.SuccessGreen

/**
 * Duolingo-style 3D Card (per 03-UI-SPEC.md).
 *
 * A stateless content card with a subtle bottom shadow that gives the raised/3D
 * Duolingo impression. The shadow shrinks to 0dp while pressed, simulating the
 * button pushing into the surface.
 *
 * Color choices follow UI-SPEC:
 *  - default: theme surface with a 2dp elevation
 *  - selected: blue tint background
 *  - correct: green tint background
 *  - incorrect: red tint background
 *  - disabled: 50% alpha
 */
@Composable
fun Duo3DCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    state: Duo3DCardState = Duo3DCardState.Default,
    cornerRadius: Dp = 16.dp,
    shadowElevation: Dp = 4.dp,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val resolvedBg = backgroundColor ?: when (state) {
        Duo3DCardState.Default -> MaterialTheme.colorScheme.surface
        Duo3DCardState.Selected -> MaterialTheme.colorScheme.secondaryContainer
        Duo3DCardState.Correct -> SuccessGreen.copy(alpha = 0.15f)
        Duo3DCardState.Incorrect -> ErrorRed.copy(alpha = 0.15f)
        Duo3DCardState.Disabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }

    val resolvedBorder = borderColor ?: when (state) {
        Duo3DCardState.Default -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        Duo3DCardState.Selected -> MaterialTheme.colorScheme.secondary
        Duo3DCardState.Correct -> SuccessGreen
        Duo3DCardState.Incorrect -> ErrorRed
        Duo3DCardState.Disabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
    }

    val effectiveElevation = if (isPressed && onClick != null) 0.dp else shadowElevation

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = state != Duo3DCardState.Disabled,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .shadow(
                elevation = effectiveElevation,
                shape = RoundedCornerShape(cornerRadius)
            )
            .then(clickableModifier),
        shape = RoundedCornerShape(cornerRadius),
        color = resolvedBg,
        border = BorderStroke(2.dp, resolvedBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Visual state for [Duo3DCard]. Stateless — the caller decides which state to
 * render based on UI events. No side effects, easy to preview/test.
 */
enum class Duo3DCardState {
    Default,
    Selected,
    Correct,
    Incorrect,
    Disabled
}

/**
 * A horizontal row of [Duo3DCard]s. Convenience helper for layouts like a
 * 2-column settings grid.
 */
@Composable
fun Duo3DCardRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 12.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        content = content
    )
}
