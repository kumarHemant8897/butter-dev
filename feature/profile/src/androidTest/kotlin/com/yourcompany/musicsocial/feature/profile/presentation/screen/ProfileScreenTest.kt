package com.yourcompany.musicsocial.feature.profile.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.musicsocial.core.designsystem.theme.ButterTheme
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.sample.SampleData
import com.yourcompany.musicsocial.feature.profile.presentation.state.ProfileUiState
import com.yourcompany.musicsocial.feature.profile.presentation.state.SampleSections
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val realProfile = UserProfile("u1", "ada", "Ada Lovelace", "first programmer", null, 12, 3)

    private val sampleState = ProfileUiState.Content(
        profile = SampleData.me,
        sample = SampleSections(SampleData.myTopArtists, SampleData.myFavoriteSongs, SampleData.myPublicMixes, SampleData.myLastPlayed),
    )

    private fun show(state: ProfileUiState, onEdit: () -> Unit = {}, onSignOut: () -> Unit = {}, onLike: (String) -> Unit = {}) {
        composeRule.setContent {
            ButterTheme {
                ProfileScreen(state, PaddingValues(), ProfileActions(onEditProfile = onEdit, onSignOut = onSignOut, onToggleLike = onLike))
            }
        }
    }

    @Test
    fun showsTheRealProfileAndItsActions() {
        var edited = false
        var signedOut = false
        show(ProfileUiState.Content(realProfile), onEdit = { edited = true }, onSignOut = { signedOut = true })

        composeRule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        composeRule.onNodeWithText("@ada").assertIsDisplayed()
        composeRule.onNodeWithText("Edit profile").performClick()
        composeRule.onNodeWithText("Sign out").performClick()

        assertEquals(true, edited)
        assertEquals(true, signedOut)
    }

    @Test
    fun aRealProfileShowsItsOwnLibraryNotSampleSongs() {
        show(ProfileUiState.Content(realProfile))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Saved songs"))
        composeRule.onNodeWithText("Liked songs").assertExists()
        composeRule.onNodeWithText("Night Drive").assertDoesNotExist()
    }

    @Test
    fun tappingTheHeartOnASampleSongReportsIt() {
        var toggled: String? = null
        show(sampleState, onLike = { toggled = it })

        composeRule.onNodeWithContentDescription("Remove Night Drive from favorites").performClick()

        assertEquals(SampleData.nightDrive.id, toggled)
    }

    @Test
    fun withoutABackendTheSampleProfileExplainsHowToConfigure() {
        show(sampleState)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Backend not connected"))
        composeRule.onNodeWithText("Backend not connected").assertIsDisplayed()
    }
}
