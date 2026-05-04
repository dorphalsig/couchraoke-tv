package com.couchraoke.tv.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 30_000)
    fun themeAppliesIteration1Palette() {
        var primary: Color? = null
        var secondary: Color? = null
        var tertiary: Color? = null
        var background: Color? = null
        var surface: Color? = null

        composeRule.setContent {
            CouchraokeTheme {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                tertiary = MaterialTheme.colorScheme.tertiary
                background = MaterialTheme.colorScheme.background
                surface = MaterialTheme.colorScheme.surface
            }
        }

        composeRule.runOnIdle {
            assertEquals(TextPrimary, primary)
            assertEquals(TextSecondary, secondary)
            assertEquals(Player1Accent, tertiary)
            assertEquals(AppBackground, background)
            assertEquals(SurfacePrimary, surface)
        }
    }

    @Test(timeout = 30_000)
    fun typographyUsesConfiguredBodyLargeStyle() {
        assertEquals(24.sp, Typography.bodyLarge.fontSize)
    }
}
