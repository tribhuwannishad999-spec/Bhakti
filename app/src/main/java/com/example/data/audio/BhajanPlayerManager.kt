package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.data.model.BhajanTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class BhajanPlayerState(
    val currentTrack: BhajanTrack? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val volume: Float = 0.85f,
    val playbackModeNote: String = "",
    val errorMessage: String? = null
)

/**
 * Bhakti Music Player Engine — strictly NO autoplay.
 * Plays legally authorized owner-uploaded audio URLs via MediaPlayer, or recites
 * traditional devotional path/lyrics via Hindi TTS when no audio URL is attached yet.
 */
class BhajanPlayerManager(context: Context) {
    private val appContext = context.applicationContext
    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var playlist: List<BhajanTrack> = emptyList()
    private var currentIndex: Int = -1

    private val _state = MutableStateFlow(BhajanPlayerState())
    val state: StateFlow<BhajanPlayerState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale("hi", "IN"))
                tts?.setSpeechRate(0.85f)
                ttsReady = true
            }
        }
    }

    fun setPlaylist(tracks: List<BhajanTrack>) {
        playlist = tracks
        val curId = _state.value.currentTrack?.id
        if (curId != null) {
            currentIndex = tracks.indexOfFirst { it.id == curId }
        }
    }

    fun playTrack(track: BhajanTrack, tracks: List<BhajanTrack> = playlist) {
        playlist = tracks
        currentIndex = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        stopInternal()

        val validUrl = track.audioUrl?.takeIf {
            it.startsWith("https://") || it.startsWith("http://")
        }

        if (validUrl != null) {
            _state.value = _state.value.copy(
                currentTrack = track,
                isLoading = true,
                isPlaying = false,
                errorMessage = null,
                playbackModeNote = "ऑडियो स्ट्रीम लोड हो रही है..."
            )
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setVolume(_state.value.volume, _state.value.volume)
                    setDataSource(validUrl)
                    setOnPreparedListener { mp ->
                        mp.start()
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isPlaying = true,
                            playbackModeNote = "अधिकृत ऑडियो चल रहा है"
                        )
                    }
                    setOnCompletionListener {
                        _state.value = _state.value.copy(isPlaying = false)
                    }
                    setOnErrorListener { _, _, _ ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isPlaying = false,
                            errorMessage = "ऑडियो फ़ाइल लोड नहीं हो सकी। कृपया URL जांचें।"
                        )
                        true
                    }
                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Bhajan audio failed", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = e.localizedMessage ?: "Playback error"
                )
            }
        } else {
            // Transparently recite lyrics via Hindi TTS when owner hasn't attached a recording yet
            val textToRecite = track.lyrics?.takeIf { it.isNotBlank() } ?: track.title
            if (ttsReady) {
                tts?.speak(
                    textToRecite.take(600),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "bhajan_${track.id}"
                )
                _state.value = _state.value.copy(
                    currentTrack = track,
                    isLoading = false,
                    isPlaying = true,
                    errorMessage = null,
                    playbackModeNote = "पाठ वाचन (Hindi TTS) — ओनर डैशबोर्ड से अधिकृत MP3 जोड़ा जा सकता है"
                )
            } else {
                _state.value = _state.value.copy(
                    currentTrack = track,
                    isLoading = false,
                    isPlaying = false,
                    errorMessage = "कोई ऑडियो फ़ाइल अपलोड नहीं है और TTS उपलब्ध नहीं है।"
                )
            }
        }
    }

    fun togglePlayPause() {
        val current = _state.value.currentTrack ?: return
        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(isPlaying = false)
            } else {
                mp.start()
                _state.value = _state.value.copy(isPlaying = true)
            }
        } else if (_state.value.isPlaying) {
            tts?.stop()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            playTrack(current)
        }
    }

    fun next() {
        if (playlist.isEmpty()) return
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % playlist.size
        playTrack(playlist[nextIndex])
    }

    fun previous() {
        if (playlist.isEmpty()) return
        val prevIndex = if (currentIndex <= 0) playlist.lastIndex else currentIndex - 1
        playTrack(playlist[prevIndex])
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = clamped)
        mediaPlayer?.setVolume(clamped, clamped)
    }

    private fun stopInternal() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.reset()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        stopInternal()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
        }
    }

    private companion object {
        const val TAG = "BhajanPlayerManager"
    }
}
