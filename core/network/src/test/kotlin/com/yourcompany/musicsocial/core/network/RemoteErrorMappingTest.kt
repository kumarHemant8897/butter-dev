package com.yourcompany.musicsocial.core.network

import com.yourcompany.musicsocial.core.domain.result.AppError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class RemoteErrorMappingTest {

    @Test
    fun `connectivity failures map to network errors`() {
        assertEquals(AppError.Network.Unavailable, UnknownHostException("db.example.supabase.co").toAppError())
        assertEquals(AppError.Network.Unavailable, IOException("connection reset").toAppError())
        assertEquals(AppError.Network.Timeout, SocketTimeoutException().toAppError())
    }

    @Test
    fun `http status codes map to the matching app error`() {
        assertEquals(AppError.Auth.SessionExpired, 401.toAppError())
        assertEquals(AppError.Auth.Forbidden, 403.toAppError())
        assertEquals(AppError.NotFound, 404.toAppError())
        assertEquals(AppError.Conflict, 409.toAppError())
        assertEquals(AppError.Server(503), 503.toAppError())
    }

    @Test
    fun `unexpected exceptions do not leak as specific errors`() {
        assertEquals(AppError.Unknown, IllegalStateException("boom").toAppError())
    }

    @Test
    fun `config is only usable with both url and key, and never prints the key`() {
        val key = "sb_publishable_secretvalue"
        val configured = BackendConfig("https://abc.supabase.co", key, enableHttpLogging = false)

        assertEquals(true, configured.isConfigured)
        assertEquals(false, BackendConfig("", key, false).isConfigured)
        assertEquals(false, BackendConfig("https://abc.supabase.co", " ", false).isConfigured)
        assertEquals(false, configured.toString().contains(key))
    }
}
