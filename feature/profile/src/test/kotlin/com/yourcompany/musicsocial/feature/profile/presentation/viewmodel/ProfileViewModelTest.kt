package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import app.cash.turbine.test
import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.feature.profile.presentation.state.AccountStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `shows loading while the session is restored, then the signed-out state`() = runTest {
        val auth = FakeAuthRepository(initial = AuthState.Initializing)
        val viewModel = ProfileViewModel(auth)

        viewModel.uiState.test {
            assertEquals(AccountStatus.Loading, awaitItem().account)

            auth.emit(AuthState.SignedOut)
            assertEquals(AccountStatus.SignedOut, awaitItem().account)
        }
    }

    @Test
    fun `a build without backend credentials reports the backend as unavailable`() = runTest {
        val viewModel = ProfileViewModel(FakeAuthRepository(initial = AuthState.Unavailable))

        viewModel.uiState.test {
            assertEquals(AccountStatus.BackendUnavailable, awaitItem().account)
        }
    }

    @Test
    fun `signing in and out updates the account status`() = runTest {
        val auth = FakeAuthRepository(initial = AuthState.SignedOut)
        val viewModel = ProfileViewModel(auth)

        viewModel.uiState.test {
            assertEquals(AccountStatus.SignedOut, awaitItem().account)

            auth.emit(AuthState.SignedIn(userId = "user-1", email = null))
            assertEquals(AccountStatus.SignedIn("user-1"), awaitItem().account)

            auth.signOut()
            assertEquals(AccountStatus.SignedOut, awaitItem().account)
        }
    }

    @Test
    fun `un-hearting a favourite song toggles only that song`() = runTest {
        val viewModel = ProfileViewModel(FakeAuthRepository(initial = AuthState.SignedOut))

        viewModel.uiState.test {
            val songs = awaitItem().favoriteSongs.map { it.id }

            viewModel.onToggleLike(songs[0])
            val afterUnlike = awaitItem().unlikedSongIds
            assertTrue(songs[0] in afterUnlike)
            assertFalse(songs[1] in afterUnlike)

            viewModel.onToggleLike(songs[0])
            assertTrue(awaitItem().unlikedSongIds.isEmpty())
        }
    }

    @Test
    fun `profile tab shows Hemant Kumar's profile`() = runTest {
        val viewModel = ProfileViewModel(FakeAuthRepository(initial = AuthState.SignedOut))

        viewModel.uiState.test {
            assertEquals("Hemant Kumar", awaitItem().profile.displayName)
        }
    }
}
