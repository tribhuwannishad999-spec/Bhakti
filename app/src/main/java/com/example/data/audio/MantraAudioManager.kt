package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.MantraAudioMapping
import com.example.data.model.MantraItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState
    data object Loading : AudioPlaybackState
    data class Playing(val mantra: MantraItem, val sourceLabel: String) : AudioPlaybackState
    data class Error(val message: String) : AudioPlaybackState
}

/**
 * Dedicated Mantra Voice & Audio Management Module.
 * Guarantees 1-to-1 exact mantra-to-audio mapping, stops any previous speech/audio
 * before starting the next (zero overlap), supports owner-uploaded custom audio URLs
 * with verified Hindi TextToSpeech fallback, and never autoplays before user interaction.
 */
class MantraAudioManager(context: Context) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var hindiVoiceAvailable = false
    private var mediaPlayer: MediaPlayer? = null

    private val customMappings = mutableMapOf<String, MantraAudioMapping>()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _audioState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val audioState: StateFlow<AudioPlaybackState> = _audioState.asStateFlow()

    private val _ttsEngineStatus = MutableStateFlow("हिंदी उच्चारण इंजन तैयार हो रहा है...")
    val ttsEngineStatus: StateFlow<String> = _ttsEngineStatus.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val hindiLocale = Locale("hi", "IN")
                    val langResult = tts?.setLanguage(hindiLocale)
                    ttsReady = true
                    hindiVoiceAvailable = langResult != TextToSpeech.LANG_MISSING_DATA &&
                        langResult != TextToSpeech.LANG_NOT_SUPPORTED
                    tts?.setSpeechRate(0.88f)
                    tts?.setPitch(0.96f)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            _audioState.value = AudioPlaybackState.Idle
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _audioState.value = AudioPlaybackState.Error("मंत्र उच्चारण में त्रुटि हुई।")
                        }
                    })
                    _ttsEngineStatus.value = if (hindiVoiceAvailable) {
                        "Hindi TTS सक्रिय (कोई कस्टम रिकॉर्डिंग न होने पर उपयोग)"
                    } else {
                        "डिवाइस पर हिंदी TTS पैक उपलब्ध नहीं — कृपया Google Speech Services में हिंदी डाउनलोड करें"
                    }
                } else {
                    ttsReady = false
                    _ttsEngineStatus.value = "ऑडियो इंजन प्रारंभ नहीं हो सका"
                    _audioState.value = AudioPlaybackState.Error("TTS initialization failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
            _ttsEngineStatus.value = "TTS अनुपलब्ध: ${e.localizedMessage ?: "Error"}"
        }
    }

    fun updateCustomMappings(mappings: List<MantraAudioMapping>) {
        customMappings.clear()
        mappings.forEach { mapping ->
            if (mapping.id.isNotBlank()) {
                customMappings[mapping.id] = mapping
            }
        }
    }

    fun getEffectiveMappingDescription(mantra: MantraItem): String {
        val custom = customMappings[mantra.id]
        return if (custom != null && custom.customRecording && !custom.audioUrl.isNullOrBlank()) {
            "कस्टम रिकॉर्डिंग: ${custom.voiceLabel}"
        } else {
            "Hindi TTS Fallback (${mantra.mantraHindi})"
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        if (!enabled) {
            stopAll()
        }
    }

    fun toggleSound(): Boolean {
        val next = !_soundEnabled.value
        setSoundEnabled(next)
        return next
    }

    /**
     * Plays the exact mantra tapped by the user.
     * Strictly stops any previous TTS or MediaPlayer playback before starting the new mantra.
     */
    fun playExactMantra(mantra: MantraItem) {
        if (!_soundEnabled.value) return

        // Stop any currently playing audio immediately to prevent overlapping voices
        stopAll()

        val custom = customMappings[mantra.id]
        val customUrl = custom?.audioUrl?.takeIf {
            custom.customRecording && (it.startsWith("https://") || it.startsWith("http://"))
        }

        if (customUrl != null) {
            playCustomRecording(mantra, customUrl, custom.voiceLabel)
        } else {
            speakWithHindiTts(mantra)
        }
    }

    private fun playCustomRecording(mantra: MantraItem, url: String, label: String) {
        _audioState.value = AudioPlaybackState.Loading
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    _audioState.value = AudioPlaybackState.Playing(mantra, label)
                    mp.start()
                }
                setOnCompletionListener {
                    _audioState.value = AudioPlaybackState.Idle
                }
                setOnErrorListener { _, _, _ ->
                    Log.w(TAG, "Custom audio URL failed for ${mantra.id}, falling back to Hindi TTS")
                    releaseMediaPlayer()
                    speakWithHindiTts(mantra)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing custom recording for ${mantra.id}", e)
            speakWithHindiTts(mantra)
        }
    }

    private fun speakWithHindiTts(mantra: MantraItem) {
        val engine = tts
        if (!ttsReady || engine == null) {
            _audioState.value = AudioPlaybackState.Error("हिंदी उच्चारण इंजन तैयार नहीं है।")
            return
        }
        val exactText = mantra.speechTextHindi
        _audioState.value = AudioPlaybackState.Playing(
            mantra = mantra,
            sourceLabel = if (hindiVoiceAvailable) "Hindi TTS" else "Default TTS"
        )
        val result = engine.speak(
            exactText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "mantra_${mantra.id}_${System.currentTimeMillis()}"
        )
        if (result == TextToSpeech.ERROR) {
            _audioState.value = AudioPlaybackState.Error("मंत्र बोलने में त्रुटि: $exactText")
        }
    }

    fun stopAll() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        releaseMediaPlayer()
        _audioState.value = AudioPlaybackState.Idle
    }

    private fun releaseMediaPlayer() {
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
        stopAll()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
        }
    }

    private companion object {
        const val TAG = "MantraAudioManager"
    }
}
