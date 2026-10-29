package com.example.minilauncher

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Stronger tactile feedback than Compose's built-in haptics, which are often too light on Samsung devices.
 * Uses the vibrator directly: a predefined heavy effect on Android 10 and newer, a short pulse before that.
 */
object Haptics {

    /** Set from the settings; when false no vibration plays. */
    @Volatile
    var enabled: Boolean = true

    /** A firm click, used when a switch changes. */
    fun tap(context: Context) = play(context, VibrationEffect.EFFECT_HEAVY_CLICK, 40)

    /** A double click, used when an action completes (favourite, hide, copy). */
    fun confirm(context: Context) = play(context, VibrationEffect.EFFECT_DOUBLE_CLICK, 60)

    private fun play(context: Context, predefined: Int, fallbackMs: Long) {
        if (!enabled) return
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(predefined)
        } else {
            VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}
