package com.yourcompany.musicsocial.data.user.repository

import com.yourcompany.musicsocial.core.domain.model.FollowStatus
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.Relationship
import com.yourcompany.musicsocial.core.domain.result.AppError
import com.yourcompany.musicsocial.core.domain.result.AppResult
import com.yourcompany.musicsocial.core.domain.result.getOrNull
import com.yourcompany.musicsocial.data.user.datasource.FollowEdgeDto
import com.yourcompany.musicsocial.data.user.datasource.IncomingRequestDto
import com.yourcompany.musicsocial.data.user.datasource.ProfileDto
import com.yourcompany.musicsocial.data.user.datasource.SupabaseUserRemoteDataSource
import com.yourcompany.musicsocial.data.user.datasource.UserRemoteDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UserRepositoryImplTest {

    private val remote = mockk<UserRemoteDataSource> {
        every { currentUserId() } returns "me"
        coEvery { getBlockedIds() } returns AppResult.Success(emptySet())
    }

    @Test
    fun `someone I blocked is reported as blocked and left out of search`() = runTest {
        coEvery { remote.getBlockedIds() } returns AppResult.Success(setOf("id-spam"))
        coEvery { remote.searchProfiles("sa", null, 20) } returns AppResult.Success(listOf(dto("spam").copy(id = "id-spam"), dto("sam")))

        assertEquals(Relationship(FollowStatus.NONE, followsMe = false, blockedByMe = true), repository.getRelationship("id-spam").getOrNull())
        assertEquals(listOf("sam"), repository.searchProfiles("sa", PageRequest(limit = 20)).getOrNull()!!.items.map { it.username })
    }

    @Test
    fun `report trims details and refuses overly long ones before the network`() = runTest {
        coEvery { remote.insertReport("u2", "spam", "links") } returns AppResult.Success(Unit)

        assertEquals(AppResult.Success(Unit), repository.report("u2", com.yourcompany.musicsocial.core.domain.model.ReportReason.SPAM, "  links "))
        assertEquals(
            AppResult.Failure(AppError.Validation("details", "too_long")),
            repository.report("u2", com.yourcompany.musicsocial.core.domain.model.ReportReason.OTHER, "x".repeat(1001)),
        )
        coVerify(exactly = 1) { remote.insertReport(any(), any(), any()) }
    }
    private val repository = UserRepositoryImpl(remote)

    private fun dto(username: String, bio: String? = "hi") =
        ProfileDto(id = "id-$username", username = username, displayName = username.uppercase(), bio = bio)

    @Test
    fun `asking again while a request is waiting counts as success`() = runTest {
        coEvery { remote.insertFollowRequest("u2") } returns AppResult.Failure(AppError.Conflict)

        assertEquals(AppResult.Success(Unit), repository.requestFollow("u2"))
    }

    @Test
    fun `other request failures are passed through`() = runTest {
        coEvery { remote.insertFollowRequest("u2") } returns AppResult.Failure(AppError.Network.Unavailable)

        assertEquals(AppResult.Failure(AppError.Network.Unavailable), repository.requestFollow("u2"))
    }

    @Test
    fun `relationship reads follows in both directions and a waiting request`() = runTest {
        coEvery { remote.getFollowsBetween("u2") } returns AppResult.Success(listOf(FollowEdgeDto("me", "u2")))
        assertEquals(Relationship(FollowStatus.FOLLOWING, followsMe = false), repository.getRelationship("u2").getOrNull())

        coEvery { remote.getFollowsBetween("u3") } returns AppResult.Success(listOf(FollowEdgeDto("u3", "me")))
        coEvery { remote.isRequestPending("u3") } returns AppResult.Success(true)
        val toU3 = repository.getRelationship("u3").getOrNull()!!
        assertEquals(Relationship(FollowStatus.REQUESTED, followsMe = true), toU3)
        // They follow me, so we may chat even though my request to them is still waiting.
        assertTrue(toU3.canMessage)
        assertFalse(toU3.canSeeLibrary)
    }

    @Test
    fun `incoming requests carry the requester and when they asked`() = runTest {
        coEvery { remote.getIncomingRequests() } returns AppResult.Success(
            listOf(IncomingRequestDto(createdAt = "2026-11-11T10:00:00.123456+00:00", requester = dto("luna"))),
        )

        val request = repository.getIncomingRequests().getOrNull()!!.single()

        assertEquals("luna", request.requester.username)
        assertEquals(Instant.parse("2026-11-11T10:00:00.123456Z"), request.requestedAt)
    }

    @Test
    fun `people search leaves out the signed-in user`() = runTest {
        coEvery { remote.searchProfiles("lu", null, 20) } returns AppResult.Success(listOf(dto("luna"), dto("me").copy(id = "me")))

        assertEquals(listOf("luna"), repository.searchProfiles("lu", PageRequest(limit = 20)).getOrNull()!!.items.map { it.username })
    }

    @Test
    fun `blank bio becomes null so the UI shows no empty line`() = runTest {
        coEvery { remote.getProfile("id-a") } returns AppResult.Success(dto("a", bio = "   "))

        assertNull(repository.getProfile("id-a").getOrNull()!!.bio)
    }

    @Test
    fun `search ignores queries shorter than two characters`() = runTest {
        val page = repository.searchProfiles(" a ", PageRequest()).getOrNull()!!

        assertEquals(emptyList<Any>(), page.items)
        assertNull(page.nextCursor)
        coVerify(exactly = 0) { remote.searchProfiles(any(), any(), any()) }
    }

    @Test
    fun `a full page returns the last username as the next cursor, a short page ends the list`() = runTest {
        coEvery { remote.searchProfiles("jo", null, 2) } returns AppResult.Success(listOf(dto("joe"), dto("john")))
        coEvery { remote.searchProfiles("jo", "john", 2) } returns AppResult.Success(listOf(dto("jon")))

        val first = repository.searchProfiles("jo", PageRequest(limit = 2)).getOrNull()!!
        val second = repository.searchProfiles("jo", PageRequest(limit = 2, cursor = first.nextCursor)).getOrNull()!!

        assertEquals("john", first.nextCursor)
        assertEquals(listOf("jon"), second.items.map { it.username })
        assertNull(second.nextCursor)
    }

    @Test
    fun `filter input cannot inject filter syntax and wildcards match literally`() {
        val sanitize = SupabaseUserRemoteDataSource.Companion::sanitizeForFilter

        assertEquals("abc", sanitize("a,b)c"))
        assertEquals("a.b\\_c-d' e", sanitize("  a.b_c-d' e "))
        // '%' is dropped entirely, so it can never act as a wildcard; '_' is kept but escaped.
        assertEquals("100", sanitize("100%"))
        assertEquals("\\_x", sanitize("_x"))
        assertEquals("", sanitize("(),\"*"))
    }
}
