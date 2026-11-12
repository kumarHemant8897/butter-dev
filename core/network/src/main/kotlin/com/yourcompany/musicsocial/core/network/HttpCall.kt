package com.yourcompany.musicsocial.core.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs an OkHttp call without blocking a thread. Cancelling the coroutine cancels the HTTP request,
 * so a search that is replaced by a newer one stops using the network straight away.
 * The caller must close the returned [Response] (for example with `use`).
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(
        object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isCancelled) response.close() else continuation.resume(response)
            }
        },
    )
}
