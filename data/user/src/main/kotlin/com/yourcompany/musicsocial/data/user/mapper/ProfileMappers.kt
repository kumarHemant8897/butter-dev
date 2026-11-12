package com.yourcompany.musicsocial.data.user.mapper

import com.yourcompany.musicsocial.core.domain.model.UserProfile
import com.yourcompany.musicsocial.data.user.datasource.ProfileDto

fun ProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    username = username,
    displayName = displayName,
    bio = bio?.takeIf { it.isNotBlank() },
    avatarUrl = avatarUrl,
    followerCount = followerCount,
    followingCount = followingCount,
)
