package com.yourcompany.musicsocial.feature.profile.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.AuthState
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.ValidateBioUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateDisplayNameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateUsernameUseCase
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.core.testing.fake.FakeUserRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun profile(id: String, username: String, bio: String? = null) = UserProfile(
        id = id, username = username, displayName = username.replaceFirstChar { it.uppercase() },
        bio = bio, avatarUrl = null, followerCount = 0, followingCount = 0,
    )

    private val users = FakeUserRepository(
        meId = "me",
        initialProfiles = listOf(profile("me", "hemant", bio = "old bio"), profile("other", "taken")),
    )
    private val auth = FakeAuthRepository(AuthState.SignedIn("me", null))

    /** Pretends every picked photo becomes these bytes, or fails when [bytes] is null. */
    private class FakeImageReader(var bytes: ByteArray? = byteArrayOf(1, 2, 3)) :
        com.yourcompany.musicsocial.feature.profile.presentation.avatar.AvatarImageReader {
        override suspend fun read(uri: String): ByteArray? = bytes
    }

    private val images = FakeImageReader()

    private fun viewModel() = EditProfileViewModel(
        auth, users, ValidateUsernameUseCase(), ValidateDisplayNameUseCase(), ValidateBioUseCase(), images,
    )

    @Test
    fun `a picked photo is uploaded and shown at once`() {
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://media/picked/1")

        val state = viewModel.uiState.value
        assertEquals(1, users.uploadedAvatars.size)
        assertEquals("https://storage.example/avatars/me/avatar-1.jpg", state.avatarUrl)
        assertEquals("https://storage.example/avatars/me/avatar-1.jpg", users.profiles["me"]!!.avatarUrl)
        assertFalse(state.isUploadingPhoto)
    }

    @Test
    fun `an unreadable photo shows an error and uploads nothing`() {
        images.bytes = null
        val viewModel = viewModel()

        viewModel.onPhotoPicked("content://media/picked/broken")

        assertTrue(users.uploadedAvatars.isEmpty())
        assertTrue(viewModel.uiState.value.photoError is AppError.Validation)
    }

    @Test
    fun `the form is filled from the current profile`() {
        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("Hemant", state.displayName)
        assertEquals("hemant", state.username)
        assertEquals("old bio", state.bio)
    }

    @Test
    fun `a load failure keeps the form in the loading state with the error`() {
        users.failWith = AppError.Network.Unavailable

        val state = viewModel().uiState.value

        assertTrue(state.loadFailed)
        assertEquals(AppError.Network.Unavailable, state.error)
    }

    @Test
    fun `saving without changes does not call the backend`() {
        val viewModel = viewModel()

        viewModel.onSave()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(emptyList<ProfileUpdate>(), users.updates)
    }

    @Test
    fun `only changed fields are sent and the username is normalised`() {
        val viewModel = viewModel()
        viewModel.onUsernameChange("  Hemant.K ")
        viewModel.onBioChange("")

        viewModel.onSave()

        assertEquals(listOf(ProfileUpdate(username = "hemant.k", bio = "")), users.updates)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `invalid fields are reported without a request`() {
        val viewModel = viewModel()
        viewModel.onUsernameChange("a")
        viewModel.onBioChange("x".repeat(161))

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals(UsernameRule.TOO_SHORT, state.usernameError)
        assertEquals(com.yourcompany.musicsocial.core.domain.usecase.Validated.Rule.TOO_LONG, state.bioError)
        assertFalse(state.isSaved)
        assertEquals(emptyList<ProfileUpdate>(), users.updates)
    }

    @Test
    fun `a taken username is reported on the username field, not as a generic error`() {
        val viewModel = viewModel()
        viewModel.onUsernameChange("taken")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertTrue(state.usernameTaken)
        assertEquals(null, state.error)
        assertFalse(state.isSaved)
        assertFalse(state.isSaving)
    }
}
