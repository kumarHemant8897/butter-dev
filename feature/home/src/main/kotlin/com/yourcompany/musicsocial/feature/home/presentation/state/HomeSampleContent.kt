package com.yourcompany.musicsocial.feature.home.presentation.state

import com.yourcompany.musicsocial.core.sample.SampleData

/** TESTING ONLY: Home feed built from SampleData. Replace with the feed repository. */
internal fun sampleHomeContent() = HomeContent(
    recentlyPlayed = SampleData.recentlyPlayed,
    recommendedAlbums = SampleData.recommendedAlbums,
    pickedForYou = SampleData.softLanding,
    trending = SampleData.trending.firstOrNull(),
    friendsListening = SampleData.friendsListening,
    sharedWithYou = SharedWithYou(
        senderName = SampleData.eli.displayName.substringBefore(' '),
        song = SampleData.nightDrive,
        quote = "this feels like our kind of night",
        minutesAgo = 2,
    ),
    madeForYou = SampleData.midnightMix,
    isSample = true,
)
