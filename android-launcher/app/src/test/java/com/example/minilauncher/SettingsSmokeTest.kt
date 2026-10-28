package com.example.minilauncher

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression guard: opening settings must not crash, and the theme section must be there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsOpensWithoutCrashing() {
        compose.onNodeWithContentDescription("Ayarlar").performClick()
        compose.onNodeWithText("Tema").assertIsDisplayed()
    }
}
