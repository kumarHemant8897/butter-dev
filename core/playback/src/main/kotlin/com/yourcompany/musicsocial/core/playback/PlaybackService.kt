package com.yourcompany.musicsocial.core.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Keeps playback alive when the app is in the background and publishes it to the system: the media
 * notification, the lock screen, headphones and Bluetooth buttons.
 *
 * The player itself belongs to [ExoPlaybackController] (one player for the whole app). The session
 * wraps it, so screens and system controls always act on the same queue. Media3 moves this service
 * to the foreground while music plays and back when it stops.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject internal lateinit var controller: ExoPlaybackController

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val builder = MediaSession.Builder(this, controller.sessionPlayer())
        // Tapping the notification opens the app where the user left it.
        packageManager.getLaunchIntentForPackage(packageName)?.let { launch ->
            launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            builder.setSessionActivity(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        }
        session = builder.build().also {
            // The app starts this service directly instead of connecting a MediaController, so the
            // session must be added here; that is what makes Media3 post the notification and keep
            // the service in the foreground while playing.
            addSession(it)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away while paused ends the service; while playing, music continues. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        // Release the session only. The player is the app-wide one and stays with the controller.
        session?.release()
        session = null
        controller.onServiceDestroyed()
        super.onDestroy()
    }
}
