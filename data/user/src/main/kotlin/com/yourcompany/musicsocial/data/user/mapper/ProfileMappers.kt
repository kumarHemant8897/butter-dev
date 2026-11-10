package com.yourcompany.musicsocial.data.user.mapper

import com.yourcompany.musicsocial.core.domain.model.ProfileUpdate
import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.data.user.datasource.ProfileDto
import com.yourcompany.musicsocial.data.user.datasource.ProfileUpdateDto

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
    followerCount = followerCount,
    followingCount = followingCount,
)

fun ProfileUpdate.toDto(): ProfileUpdateDto = ProfileUpdateDto(
    username = username,
    displayName = displayName,
    bio = bio,
    avatarUrl = avatarUrl,
)
