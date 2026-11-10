package com.yourcompany.musicsocial.core.domain.result

/**
 * Every failure the app knows how to explain to a user.
 *
 * The data layer translates SDK/transport exceptions (Supabase, OkHttp, Room) into these types, so
 * the domain and UI layers never see vendor-specific exceptions. Keep this list small: add a case
 * only when the UI needs to react to it differently.
 */
sealed interface AppError {

    sealed interface Network : AppError {
        /** No connectivity, DNS failure or connection refused. */
        data object Unavailable : Network

        /** The request started but did not complete in time. */
        data object Timeout : Network
    }

    sealed interface Auth : AppError {
        data object InvalidCredentials : Auth

        /** The refresh token is no longer valid; the user must sign in again. */
        data object SessionExpired : Auth

        /** Signed in, but Row Level Security or the API refused the operation. */
        data object Forbidden : Auth

        /** The operation needs a signed-in user and there is none. */
        data object NotSignedIn : Auth
    }

    /** The backend answered with a 5xx or an unexpected status. */
    data class Server(val statusCode: Int?) : AppError

    /** A single requested item does not exist (or is not visible to this user). */
    data object NotFound : AppError

    /** A uniqueness rule was violated, for example a username that is already taken. */
    data object Conflict : AppError

    /** Input rejected before it reached the network. [field] and [rule] are stable codes, not UI text. */
    data class Validation(val field: String, val rule: String) : AppError

    /** SUPABASE_URL / SUPABASE_ANON_KEY were not supplied at build time. */
    data object BackendNotConfigured : AppError

    /** Anything we did not anticipate. Details are logged by the data layer, never shown to users. */
    data object Unknown : AppError
}
