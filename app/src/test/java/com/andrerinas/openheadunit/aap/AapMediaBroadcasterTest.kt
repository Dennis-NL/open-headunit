package com.andrerinas.openheadunit.aap

import android.content.Context
import com.andrerinas.openheadunit.aap.protocol.proto.MediaPlayback
import com.andrerinas.openheadunit.contract.MediaUpdateIntent
import com.andrerinas.openheadunit.contract.NavigationUpdateIntent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mockConstruction
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AapMediaBroadcasterTest {
    private val context = mock<Context>().also { whenever(it.applicationContext).thenReturn(it) }

    private fun meta(song: String? = null, artist: String? = null, album: String? = null) =
        MediaPlayback.MediaMetaData.newBuilder().apply {
            song?.let { setSong(it) }
            artist?.let { setArtist(it) }
            album?.let { setAlbum(it) }
        }.build()

    private fun status(state: MediaPlayback.MediaPlaybackStatus.State? = null, seconds: Int? = null) =
        MediaPlayback.MediaPlaybackStatus.newBuilder().apply {
            state?.let { setState(it) }
            seconds?.let { setPlaybackSeconds(it) }
        }.build()

    private fun record(block: (AapMediaBroadcaster) -> Unit): List<List<Any?>> {
        val sent = mutableListOf<List<Any?>>()
        mockConstruction(MediaUpdateIntent::class.java) { _, ctx -> sent.add(ctx.arguments()) }.use {
            block(AapMediaBroadcaster(context))
        }
        return sent
    }

    @Test fun metadataAndPlayStateAreBroadcastWithPermission() {
        val sent = record {
            it.onMetadata(meta("Song", "Artist", "Album"))
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING))
        }
        assertEquals(listOf(listOf("Song", "Artist", "Album", false), listOf("Song", "Artist", "Album", true)), sent)
        verify(context, times(2)).sendBroadcast(any<android.content.Intent>(), eq(NavigationUpdateIntent.BROADCAST_PERMISSION))
    }

    @Test fun positionOnlyAndRepeatedUpdatesAreNotRebroadcast() {
        val sent = record {
            it.onMetadata(meta("Song", "Artist", "Album"))
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 1))
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 2))
            it.onPlaybackStatus(status(seconds = 3))
            it.onMetadata(meta("Song", "Artist", "Album"))
        }
        assertEquals(2, sent.size)
    }

    @Test fun unsetFieldsReadAsEmptyAndUnsetStateKeepsPlayState() {
        val sent = record {
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING))
            it.onMetadata(meta(song = "Song"))
            it.onPlaybackStatus(status())
        }
        assertEquals(listOf(listOf("", "", "", true), listOf("Song", "", "", true)), sent)
    }

    @Test fun pausedStateIsBroadcastAsNotPlaying() {
        val sent = record {
            it.onMetadata(meta("Song", "Artist", "Album"))
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING))
            it.onPlaybackStatus(status(MediaPlayback.MediaPlaybackStatus.State.PAUSED))
        }
        assertEquals(false, sent.last()[3])
    }

    @Test fun sessionEndSendsOneEmptyBroadcast() {
        val sent = record {
            it.onSessionEnded()
            it.onMetadata(meta("Song", "Artist", "Album"))
            it.onSessionEnded()
            it.onSessionEnded()
        }
        assertEquals(listOf(listOf("Song", "Artist", "Album", false), listOf("", "", "", false)), sent)
    }
}
