package com.couchraoke.tv.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.couchraoke.tv.ui.theme.CouchraokeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InterruptionShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun rendersPauseCopyInReusableBlockingShell() {
        renderShell(title = "PAUSED", bodyLines = listOf("Playback is paused."))

        composeRule.onNodeWithTag("interruption-shell").assertIsDisplayed()
        composeRule.onNodeWithText("PAUSED").assertIsDisplayed()
        composeRule.onNodeWithText("Playback is paused.").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun rendersCountdownDisconnectCopyInReusableBlockingShell() {
        renderShell(
            title = "DISCONNECTED",
            bodyLines = listOf("Player 1 disconnected.", "Reconnect the phone or quit to Song List."),
        )

        composeRule.onNodeWithText("DISCONNECTED").assertIsDisplayed()
        composeRule.onNodeWithText("Player 1 disconnected.").assertIsDisplayed()
        composeRule.onNodeWithText("Reconnect the phone or quit to Song List.").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun rendersStartFailureAndPlaybackErrorCopyInReusableBlockingShell() {
        renderShell(
            title = "ERROR",
            bodyLines = listOf("This song can't be played.", "audio warning"),
        )

        composeRule.onNodeWithText("ERROR").assertIsDisplayed()
        composeRule.onNodeWithText("This song can't be played.").assertIsDisplayed()
        composeRule.onNodeWithText("audio warning").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun scrimAlwaysLocksBackgroundFocusWithoutExplicitBackground() {
        renderShell(title = "ERROR", bodyLines = listOf("This song can't be played."))

        assertTrue(composeRule.nodeWithTagExists("interruption-shell-scrim"))
        composeRule.onNodeWithTag("interruption-shell-scrim").assertIsDisplayed()
    }

    @Test(timeout = 30_000)
    fun rendersSelectPlayersNoPhoneCopyAndLocksBackgroundFocus() {
        composeRule.setContent {
            CouchraokeTheme {
                InterruptionShell(
                    title = "No phones connected",
                    bodyLines = listOf("Open Join and scan the QR code with the phone app."),
                    background = { backgroundModifier ->
                        Box(backgroundModifier.testTag("underlying-song-list"))
                    },
                )
            }
        }

        composeRule.onNodeWithText("No phones connected").assertIsDisplayed()
        composeRule.onNodeWithText("Open Join and scan the QR code with the phone app.").assertIsDisplayed()
        assertTrue(composeRule.nodeWithTagExists("underlying-song-list"))
        assertTrue(composeRule.nodeWithTagExists("interruption-background-focus-locked"))
    }

    @Test(timeout = 30_000)
    fun shellIsCopyReusableWithoutAddingSettingsSurface() {
        renderShell(title = "LIBRARY REFRESH FAILED", bodyLines = listOf("Try again from Song List."))

        composeRule.onNodeWithText("LIBRARY REFRESH FAILED").assertIsDisplayed()
        composeRule.onNodeWithText("Try again from Song List.").assertIsDisplayed()
        assertFalse(composeRule.nodeWithTextExists("Settings"))
    }

    private fun renderShell(title: String, bodyLines: List<String>) {
        composeRule.setContent {
            CouchraokeTheme {
                InterruptionShell(title = title, bodyLines = bodyLines)
            }
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.nodeWithTextExists(
        text: String,
    ): Boolean = try {
        onNodeWithText(text).fetchSemanticsNode()
        true
    } catch (expected: AssertionError) {
        false
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.nodeWithTagExists(
        tag: String,
    ): Boolean = try {
        onNodeWithTag(tag).fetchSemanticsNode()
        true
    } catch (expected: AssertionError) {
        false
    }
}
