package com.yourcompany.musicsocial.feature.auth.presentation.viewmodel

import com.yourcompany.musicsocial.core.domain.model.SignUpOutcome
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.usecase.UsernameRule
import com.yourcompany.musicsocial.core.domain.usecase.ValidateDisplayNameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateEmailUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidatePasswordUseCase
import com.yourcompany.musicsocial.core.domain.usecase.ValidateUsernameUseCase
import com.yourcompany.musicsocial.core.domain.usecase.Validated
import com.yourcompany.musicsocial.core.testing.MainDispatcherRule
import com.yourcompany.musicsocial.core.testing.fake.FakeAuthRepository
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthMode
import com.yourcompany.musicsocial.feature.auth.presentation.state.AuthNotice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val viewModel = AuthViewModel(
        auth,
        ValidateEmailUseCase(),
        ValidatePasswordUseCase(),
        ValidateUsernameUseCase(),
        ValidateDisplayNameUseCase(),
    )

    private fun fillSignIn(email: String = "hemant@example.com", password: String = "secret-pass") {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun `sign-in with invalid fields shows errors and never calls the backend`() {
        viewModel.onEmailChange("not-an-email")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(Validated.Rule.INVALID_FORMAT, state.emailError)
        assertEquals(Validated.Rule.BLANK, state.passwordError)
        assertEquals(emptyList<Pair<String, String>>(), auth.signInAttempts)
    }

    @Test
    fun `sign-in trims the email and sends the password untouched`() {
        fillSignIn(email = "  hemant@example.com ", password = " pass word ")

        viewModel.onSubmit()

        assertEquals(listOf("hemant@example.com" to " pass word "), auth.signInAttempts)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `wrong credentials surface the backend error and keep the form`() {
        auth.signInResult = AppResult.Failure(AppError.Auth.InvalidCredentials)
        fillSignIn()

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(AppError.Auth.InvalidCredentials, state.error)
        assertEquals("hemant@example.com", state.email)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `editing a field clears the previous error`() {
        auth.signInResult = AppResult.Failure(AppError.Network.Unavailable)
        fillSignIn()
        viewModel.onSubmit()

        viewModel.onPasswordChange("another")

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `sign-up validates every field, including the username rules`() {
        viewModel.onModeChange(AuthMode.SIGN_UP)
        fillSignIn(password = "short")
        viewModel.onUsernameChange("Bad Name!")
        viewModel.onDisplayNameChange("   ")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(Validated.Rule.TOO_SHORT, state.passwordError)
        assertEquals(UsernameRule.INVALID_CHARACTERS, state.usernameError)
        assertEquals(Validated.Rule.BLANK, state.displayNameError)
        assertEquals(emptyList<List<String>>(), auth.signUpAttempts)
    }

    @Test
    fun `sign-up sends normalised values`() {
        viewModel.onModeChange(AuthMode.SIGN_UP)
        fillSignIn()
        viewModel.onUsernameChange("  HemantKumar ")
        viewModel.onDisplayNameChange("  Hemant   Kumar ")

        viewModel.onSubmit()

        assertEquals(
            listOf(listOf("hemant@example.com", "secret-pass", "hemantkumar", "Hemant Kumar")),
            auth.signUpAttempts,
        )
    }

    @Test
    fun `when email confirmation is required the user is sent back to sign in with a notice`() {
        auth.signUpResult = AppResult.Success(SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED)
        viewModel.onModeChange(AuthMode.SIGN_UP)
        fillSignIn()
        viewModel.onUsernameChange("hemantkumar")
        viewModel.onDisplayNameChange("Hemant Kumar")

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(AuthMode.SIGN_IN, state.mode)
        assertEquals(AuthNotice.CONFIRM_EMAIL, state.notice)
        assertEquals("hemant@example.com", state.email)
        assertEquals("", state.password)
    }

    @Test
    fun `switching mode keeps the email but drops everything else`() {
        fillSignIn()
        viewModel.onModeChange(AuthMode.SIGN_UP)

        val state = viewModel.uiState.value
        assertEquals("hemant@example.com", state.email)
        assertEquals("", state.password)
        assertEquals(AuthMode.SIGN_UP, state.mode)
    }
}
