package com.yourcompany.musicsocial.core.domain.repository

import com.yourcompany.musicsocial.core.domain.model.Page
import com.yourcompany.musicsocial.core.domain.model.PageRequest
import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.core.domain.result.AppResult

interface UserRepository {
    suspend fun getProfile(userId: String): AppResult<UserProfile>

    suspend fun getProfileByUsername(username: String): AppResult<UserProfile>

    /** Updates the signed-in user's own profile. RLS rejects updates to anyone else's row. */
    suspend fun updateMyProfile(update: ProfileUpdate): AppResult<UserProfile>

    suspend fun searchProfiles(query: String, page: PageRequest): AppResult<Page<UserProfile>>

    suspend fun follow(userId: String): AppResult<Unit>

    suspend fun unfollow(userId: String): AppResult<Unit>
}
