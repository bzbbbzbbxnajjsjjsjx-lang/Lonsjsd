package com.example.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Haptic feedback that respects the user's setting. */
class Haptics(private val perform: (HapticFeedbackType) -> Unit, private val enabled: Boolean) {
    fun tick() = fire(HapticFeedbackType.ContextClick)
    fun toggle(on: Boolean) = fire(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
    fun confirm() = fire(HapticFeedbackType.Confirm)
    fun reject() = fire(HapticFeedbackType.Reject)
    fun long() = fire(HapticFeedbackType.LongPress)
    fun segment() = fire(HapticFeedbackType.SegmentTick)
    private fun fire(type: HapticFeedbackType) { if (enabled) perform(type) }
}

@Composable
fun rememberHaptics(): Haptics {
    val h = LocalHapticFeedback.current
    return Haptics(h::performHapticFeedback, LocalHapticsEnabled.current)
}
