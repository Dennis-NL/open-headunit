package com.andrerinas.openheadunit.aap

import android.content.Context
import com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback
import com.andrerinas.openheadunit.contract.MediaUpdateIntent
import com.andrerinas.openheadunit.contract.NavigationUpdateIntent

/** Sends [MediaUpdateIntent] only when song, artist, album or playing changes. */
class AapMediaBroadcaster(private val context: Context) {
    private data class Track(val song: String = "", val artist: String = "", val album: String = "", val playing: Boolean = false)

    private var current = Track()
    private var sent: Track? = null

    fun onMetadata(meta: MediaPlayback.MediaMetaData) = publish(current.copy(song = meta.song, artist = meta.artist, album = meta.album))

    fun onStatus(status: MediaPlayback.MediaPlaybackStatus) {
        if (status.hasState()) publish(current.copy(playing = status.state == MediaPlayback.MediaPlaybackStatus.State.PLAYING))
    }

    fun onSessionEnd() = publish(Track())

    private fun publish(t: Track) {
        current = t
        if (t == sent) return
        sent = t
        context.applicationContext.sendBroadcast(MediaUpdateIntent(t.song, t.artist, t.album, t.playing), NavigationUpdateIntent.BROADCAST_PERMISSION)
    }
}
