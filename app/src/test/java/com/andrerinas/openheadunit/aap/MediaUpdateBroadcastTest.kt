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

class MediaUpdateBroadcastTest {
    private val context = mock<Context>().also { whenever(it.applicationContext).thenReturn(it) }
    private val sent = mutableListOf<List<*>>()

    private fun run(block: (AapMediaBroadcaster) -> Unit) =
        mockConstruction(MediaUpdateIntent::class.java) { _, c -> sent.add(c.arguments()) }.use { block(AapMediaBroadcaster(context)) }

    private fun meta(song: String, artist: String? = null) = MediaPlayback.MediaMetaData.newBuilder().setSong(song)
        .also { b -> artist?.let { b.setArtist(it) } }.buildPartial()

    private fun status(state: MediaPlayback.MediaPlaybackStatus.State?, seconds: Int) = MediaPlayback.MediaPlaybackStatus.newBuilder()
        .setPlaybackSeconds(seconds).also { b -> state?.let { b.setState(it) } }.buildPartial()

    @Test fun metadataAndPlayingChangesBroadcastWithTheNavigationPermission() = run { b ->
        b.onMetadata(meta("Song", "Artist"))
        b.onStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 1))
        assertEquals(listOf(listOf("Song", "Artist", "", false), listOf("Song", "Artist", "", true)), sent)
        verify(context, times(2)).sendBroadcast(any(), eq(NavigationUpdateIntent.BROADCAST_PERMISSION))
    }

    @Test fun unsetFieldsReadAsEmptyAndPositionOnlyUpdatesAreNotSent() = run { b ->
        b.onMetadata(meta("Song"))
        b.onStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 1))
        b.onStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 2))
        b.onStatus(status(null, 3))
        b.onMetadata(meta("Song"))
        assertEquals(listOf(listOf("Song", "", "", false), listOf("Song", "", "", true)), sent)
    }

    @Test fun sessionEndSendsOneEmptyNotPlayingBroadcast() = run { b ->
        b.onMetadata(meta("Song", "Artist"))
        b.onStatus(status(MediaPlayback.MediaPlaybackStatus.State.PLAYING, 1))
        b.onSessionEnd()
        b.onSessionEnd()
        assertEquals(listOf("", "", "", false), sent.last())
        assertEquals(3, sent.size)
    }
}
