package com.yourcompany.musicsocial.core.domain.system

import kotlinx.coroutines.flow.Flow

/** Reports whether the device currently has a validated internet connection. */
interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}
