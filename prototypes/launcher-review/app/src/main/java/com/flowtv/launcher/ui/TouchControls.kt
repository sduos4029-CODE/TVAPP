package com.flowtv.launcher.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults

// TV Material 1.0 handles D-pad/Enter but does not attach pointer gesture handlers.
// Add pointer taps without introducing a second keyboard/focus target.
fun Modifier.touchClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val currentClick by rememberUpdatedState(onClick)
    pointerInput(enabled) {
        if (enabled) detectTapGestures(onTap = { currentClick() })
    }
}

@Composable
fun FlowButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
               content: @Composable RowScope.() -> Unit) {
    Button(onClick = onClick, modifier = modifier.touchClick(enabled, onClick), enabled = enabled,
        colors = ButtonDefaults.colors(focusedContainerColor = Mint, focusedContentColor = Navy),
        content = content)
}
