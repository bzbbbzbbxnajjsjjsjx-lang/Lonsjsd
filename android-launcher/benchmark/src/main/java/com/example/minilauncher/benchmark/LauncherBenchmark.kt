package com.example.minilauncher.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.example.minilauncher"

/**
 * Run from Android Studio on a connected device (Samsung A56) or from the command line:
 * ./gradlew :benchmark:connectedBenchmarkAndroidTest
 * Results are written under benchmark/build/outputs/connected_android_test_additional_output.
 */
@RunWith(AndroidJUnit4::class)
class LauncherBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /** Time from a cold process start until the launcher's first frame. */
    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }

    /** Dropped frames while the keyboard opens and closes from the search bar. */
    @Test
    fun keyboardOpenFrames() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        setupBlock = {
            pressHome()
            startActivityAndWait()
        },
    ) {
        // The search bar sits at the bottom of the screen. The offset may need tuning per device.
        device.click(device.displayWidth / 2, (device.displayHeight * 0.92f).toInt())
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }
}
