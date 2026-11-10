package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.profile.presentation.state.AccountStatus
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(account: AccountStatus) = ProfileUiState(
        account = account,
        profile = SampleData.me,
        topArtists = SampleData.myTopArtists,
        favoriteSongs = SampleData.myFavoriteSongs,
        publicMixes = SampleData.myPublicMixes,
        lastPlayed = SampleData.myLastPlayed,
        isSample = true,
    )

    @Test
    fun showsTheOwnProfileWithEditAction() {
        composeRule.setContent { ButterTheme { ProfileScreen(state(AccountStatus.SignedOut), PaddingValues(), {}) } }

        composeRule.onNodeWithText("Hemant Kumar").assertIsDisplayed()
        composeRule.onNodeWithText("@hemantkumar").assertIsDisplayed()
        composeRule.onNodeWithText("Edit profile").assertIsDisplayed()
    }

    @Test
    fun tappingTheHeartReportsTheSong() {
        var toggled: String? = null
        composeRule.setContent { ButterTheme { ProfileScreen(state(AccountStatus.SignedOut), PaddingValues(), { toggled = it }) } }

        composeRule.onNodeWithContentDescription("Remove Night Drive from favorites").performClick()

        assertEquals(SampleData.nightDrive.id, toggled)
    }

    @Test
    fun backendUnavailable_explainsHowToConfigure() {
        composeRule.setContent { ButterTheme { ProfileScreen(state(AccountStatus.BackendUnavailable), PaddingValues(), {}) } }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Backend not connected"))
        composeRule.onNodeWithText("Backend not connected").assertIsDisplayed()
    }
}
