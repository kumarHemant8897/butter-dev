package com.yourcompany.musicsocial.core.common.coroutines

import javax.inject.Qualifier

/** Dispatcher for blocking IO (disk, Room without suspend APIs, file uploads). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** Dispatcher for CPU-bound work (mapping large lists, parsing). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/**
 * Process-wide scope for work that must outlive a screen (e.g. flushing a message outbox).
 * Uses a SupervisorJob so one failing child does not cancel the others.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
