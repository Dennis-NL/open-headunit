package com.andrerinas.openheadunit.aap

import android.content.Context
import com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback
import com.andrerinas.openheadunit.contract.MediaUpdateIntent
import com.andrerinas.openheadunit.contract.NavigationUpdateIntent

class AapMediaBroadcaster(private val context: Context) {
    data class State(val song: String = "", val artist: String = "", val album: String = "", val playing: Boolean = false)

    private var current = State()
    private var sent: State? = null

    fun onMetadata(meta: MediaPlayback.MediaMetaData) =
        publish(current.copy(song = meta.song, artist = meta.artist, album = meta.album))

    fun onPlaybackStatus(status: MediaPlayback.MediaPlaybackStatus) {
        if (status.hasState()) publish(current.copy(playing = status.state == MediaPlayback.MediaPlaybackStatus.State.PLAYING))
    }

    fun onSessionEnded() {
        if (sent != null && sent != State()) publish(State())
        current = State()
    }

    private fun publish(state: State) {
        current = state
        if (state == sent) return
        sent = state
        context.applicationContext.sendBroadcast(
            MediaUpdateIntent(state.song, state.artist, state.album, state.playing),
            NavigationUpdateIntent.BROADCAST_PERMISSION
        )
    }
}
