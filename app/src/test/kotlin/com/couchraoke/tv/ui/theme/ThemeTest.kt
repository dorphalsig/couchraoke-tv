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
    fun themeAppliesLightPalette() {
        var primary: Color? = null
        var secondary: Color? = null
        var tertiary: Color? = null

        composeRule.setContent {
            CouchraokeTheme(isInDarkTheme = false) {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                tertiary = MaterialTheme.colorScheme.tertiary
            }
        }

        composeRule.runOnIdle {
            assertEquals(Purple40, primary)
            assertEquals(PurpleGrey40, secondary)
            assertEquals(Pink40, tertiary)
        }
    }

    @Test(timeout = 30_000)
    fun themeAppliesDarkPalette() {
        var primary: Color? = null
        var secondary: Color? = null
        var tertiary: Color? = null

        composeRule.setContent {
            CouchraokeTheme(isInDarkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                secondary = MaterialTheme.colorScheme.secondary
                tertiary = MaterialTheme.colorScheme.tertiary
            }
        }

        composeRule.runOnIdle {
            assertEquals(Purple80, primary)
            assertEquals(PurpleGrey80, secondary)
            assertEquals(Pink80, tertiary)
        }
    }

    @Test(timeout = 30_000)
    fun typographyUsesConfiguredBodyLargeStyle() {
        assertEquals(16.sp, Typography.bodyLarge.fontSize)
        assertEquals(24.sp, Typography.bodyLarge.lineHeight)
        assertEquals(0.5.sp, Typography.bodyLarge.letterSpacing)
    }
}
