package com.example.minitetris

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/** Short vibrations for each game event. Uses predefined effects on Android 10 and newer. */
object Haptics {

    /** Picks the event from what changed between two states and vibrates for it. */
    fun forChange(context: Context, before: GameState, after: GameState) {
        when {
            after.over && !before.over -> play(context, VibrationEffect.EFFECT_HEAVY_CLICK, 120)
            after.board != before.board && after.lines > before.lines -> play(context, VibrationEffect.EFFECT_DOUBLE_CLICK, 80)
            after.board != before.board -> play(context, VibrationEffect.EFFECT_CLICK, 25)
            after.piece?.cells != before.piece?.cells -> play(context, VibrationEffect.EFFECT_TICK, 12)
            after.piece?.col != before.piece?.col -> play(context, VibrationEffect.EFFECT_TICK, 8)
        }
    }

    private fun play(context: Context, predefined: Int, fallbackMs: Long) {
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
