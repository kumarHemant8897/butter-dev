package com.yourcompany.musicsocial.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.musicsocial.core.designsystem.icon.AppIcons
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val items = listOf(
        BottomNavItem("Home", AppIcons.Home),
        BottomNavItem("Chats", AppIcons.Chats),
    )

    @Test
    fun selectingAnItem_reportsItsIndex_andMovesSelection() {
        var lastSelected = -1
        composeRule.setContent {
            var selected by remember { mutableIntStateOf(0) }
            ButterTheme {
                BottomNavigationBar(
                    items = items,
                    selectedIndex = selected,
                    onItemSelected = {
                        lastSelected = it
                        selected = it
                    },
                )
            }
        }

        composeRule.onNodeWithText("Home").assertIsSelected()
        composeRule.onNodeWithText("Chats").assertIsNotSelected().performClick()

        assertEquals(1, lastSelected)
        composeRule.onNodeWithText("Chats").assertIsSelected()
        composeRule.onNodeWithText("Home").assertIsNotSelected()
    }
}
