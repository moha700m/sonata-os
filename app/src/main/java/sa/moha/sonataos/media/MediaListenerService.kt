package sa.moha.sonataos.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NowPlaying(val title: String = "", val artist: String = "", val app: String = "", val playing: Boolean = false, val art: Bitmap? = null, val active: Boolean = false)

/** Required so Android grants us getActiveSessions(); we don't read notification content. */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() { MediaHub.attach(this) }
}

object MediaHub {
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state
    private var controller: MediaController? = null
    private val cb = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
    }

    fun hasAccess(ctx: Context): Boolean =
        Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")?.contains(ctx.packageName) == true

    fun attach(ctx: Context) {
        if (!hasAccess(ctx)) return
        val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val comp = ComponentName(ctx, MediaListenerService::class.java)
        runCatching {
            msm.addOnActiveSessionsChangedListener({ list -> bind(list?.firstOrNull()) }, comp)
            bind(msm.getActiveSessions(comp).firstOrNull())
        }
    }

    private fun bind(c: MediaController?) {
        controller?.unregisterCallback(cb)
        controller = c
        c?.registerCallback(cb)
        publish()
    }

    private fun publish() {
        val c = controller ?: run { _state.value = NowPlaying(); return }
        val m = c.metadata
        _state.value = NowPlaying(
            title = m?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = m?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            app = c.packageName,
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            art = m?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            active = true,
        )
    }

    fun playPause() { val c = controller ?: return; if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play() }
    fun next() = controller?.transportControls?.skipToNext()
    fun prev() = controller?.transportControls?.skipToPrevious()
    fun pause() = controller?.transportControls?.pause()
    fun play() = controller?.transportControls?.play()
}
