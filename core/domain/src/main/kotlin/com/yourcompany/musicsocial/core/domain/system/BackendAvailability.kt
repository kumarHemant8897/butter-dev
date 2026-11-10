package com.yourcompany.musicsocial.core.domain.system

/**
 * Tells the UI whether this build was given backend credentials.
 * Used to show an honest "backend not configured" state instead of failing silently.
 */
interface BackendAvailability {
    val isConfigured: Boolean
}
