package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.integrations.AstroLogicsAdapter
import com.example.data.integrations.NavamshaPanchangAdapter
import com.example.data.integrations.SpiritualNewsAdapter
import com.example.data.model.BhajanTrack
import com.example.data.model.ChatMessage
import com.example.data.model.DonationRecord
import com.example.data.model.DonationStatus
import com.example.data.model.FavoriteItem
import com.example.data.model.FestivalEntry
import com.example.data.model.KathaVideo
import com.example.data.model.MantraAudioMapping
import com.example.data.model.PalmistryAnalysis
import com.example.data.model.PanchangResult
import com.example.data.model.SpiritualNewsArticle
import com.example.data.model.TempleEntry
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.AiDevotionalRepository
import com.example.data.repository.BhaktiRepository
import com.example.data.repository.DevotionalSeedCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BhaktiViewModel(
    private val repository: BhaktiRepository,
    private val aiRepository: AiDevotionalRepository,
    private val currentUserId: String,
    private val currentUserName: String,
    val isOwner: Boolean
) : ViewModel() {

    private val panchangAdapter = NavamshaPanchangAdapter()
    private val astroLogicsAdapter = AstroLogicsAdapter()
    private val newsAdapter = SpiritualNewsAdapter()

    private fun <T> Flow<T>.asUiState(tag: String, fallbackError: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, fallbackError, rootError)
                emit(UiState.Error(rootError.message ?: fallbackError))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).asUiState(TAG, "Failed to load profile")

    val favoritesState: StateFlow<UiState<List<FavoriteItem>>> =
        repository.observeFavorites(currentUserId).asUiState(TAG, "Failed to load favorites")

    val userDonationsState: StateFlow<UiState<List<DonationRecord>>> =
        repository.observeUserDonations(currentUserId).asUiState(TAG, "Failed to load donations")

    val ownerAllDonationsState: StateFlow<UiState<List<DonationRecord>>> =
        if (isOwner) {
            repository.observeAllDonationsForOwner().asUiState(TAG, "Failed to load owner donations")
        } else {
            MutableStateFlow<UiState<List<DonationRecord>>>(UiState.Success(emptyList())).asStateFlow()
        }

    val kathasState: StateFlow<UiState<List<KathaVideo>>> =
        repository.observePublishedKathas()
            .map { remote ->
                if (remote.isEmpty()) DevotionalSeedCatalog.starterKathas else remote + DevotionalSeedCatalog.starterKathas
            }
            .asUiState(TAG, "Failed to load kathas")

    val templesState: StateFlow<UiState<List<TempleEntry>>> =
        repository.observePublishedTemples()
            .map { remote -> remote + DevotionalSeedCatalog.referenceTemples }
            .asUiState(TAG, "Failed to load temples")

    val festivalsState: StateFlow<UiState<List<FestivalEntry>>> =
        repository.observePublishedFestivals()
            .map { remote -> remote + DevotionalSeedCatalog.referenceFestivals }
            .asUiState(TAG, "Failed to load festivals")

    val bhajansState: StateFlow<UiState<List<BhajanTrack>>> =
        repository.observePublishedBhajans()
            .map { remote -> remote + DevotionalSeedCatalog.referenceBhajans }
            .asUiState(TAG, "Failed to load bhajans")

    val mantraAudioMappingsState: StateFlow<UiState<List<MantraAudioMapping>>> =
        repository.observePublishedMantraAudio().asUiState(TAG, "Failed to load mantra audio mappings")

    // Language ("hi" default, switchable to "en")
    private val _language = MutableStateFlow("hi")
    val language: StateFlow<String> = _language.asStateFlow()

    // Action feedback banner
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    // Palmistry Vision AI state
    private val _palmistryState = MutableStateFlow<UiState<PalmistryAnalysis>>(UiState.Idle)
    val palmistryState: StateFlow<UiState<PalmistryAnalysis>> = _palmistryState.asStateFlow()

    // AI Devotional Assistant state
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                role = "assistant",
                content = "🙏 प्रणाम! मैं भक्ति दर्शन AI सहायक हूँ। आप मंत्रों के अर्थ, व्रत-त्योहार, पूजा विधि या आध्यात्मिक प्रश्नों के बारे में हिंदी या English में पूछ सकते हैं।",
                timestampLabel = nowLabel()
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    // Panchang state
    private val _selectedCity = MutableStateFlow("Varanasi")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _panchangState = MutableStateFlow<UiState<PanchangResult>>(UiState.Idle)
    val panchangState: StateFlow<UiState<PanchangResult>> = _panchangState.asStateFlow()

    // Rashifal live adapter state
    private val _horoscopeState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val horoscopeState: StateFlow<UiState<String>> = _horoscopeState.asStateFlow()

    // Spiritual News state
    private val _newsState = MutableStateFlow<UiState<List<SpiritualNewsArticle>>>(UiState.Idle)
    val newsState: StateFlow<UiState<List<SpiritualNewsArticle>>> = _newsState.asStateFlow()

    // Last submitted donation receipt for immediate display
    private val _lastReceipt = MutableStateFlow<DonationRecord?>(null)
    val lastReceipt: StateFlow<DonationRecord?> = _lastReceipt.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserName)
        }
        viewModelScope.launch {
            userProfileState.collect { state ->
                if (state is UiState.Success && state.data != null) {
                    _language.value = state.data.language
                }
            }
        }
        refreshPanchang("Varanasi")
        refreshSpiritualNews()
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun toggleLanguage() {
        val next = if (_language.value == "hi") "en" else "hi"
        _language.value = next
        viewModelScope.launch {
            repository.updatePreferredLanguage(next)
        }
    }

    fun syncBubbleGameStats(punyaScore: Long, totalChants: Long, sadhanaLevel: Int, bestScore: Long) {
        viewModelScope.launch {
            repository.saveGameProgress(
                punyaScore = punyaScore,
                totalChants = totalChants,
                sadhanaLevel = sadhanaLevel.toLong(),
                bestScore = bestScore
            )
        }
    }

    fun toggleFavorite(itemId: String, itemType: String, title: String, currentlyFavorited: Boolean) {
        viewModelScope.launch {
            val res = repository.toggleFavorite(itemId, itemType, title, currentlyFavorited)
            res.onFailure { _actionMessage.value = it.localizedMessage ?: "Favorite update failed" }
        }
    }

    // ==========================================
    // DONATIONS
    // ==========================================

    fun submitDonation(
        donorName: String,
        donorPhone: String?,
        donorEmail: String?,
        amountInr: Long,
        purpose: String,
        paymentApp: String,
        transactionRef: String
    ) {
        viewModelScope.launch {
            val result = repository.submitPendingDonation(
                donorName = donorName,
                donorPhone = donorPhone,
                donorEmail = donorEmail,
                amountInr = amountInr,
                purpose = purpose,
                paymentApp = paymentApp,
                transactionRef = transactionRef
            )
            result.onSuccess { record ->
                _lastReceipt.value = record
                _actionMessage.value = "दान संदर्भ दर्ज किया गया (स्थिति: सत्यापन लंबित)।"
            }.onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "दान दर्ज करने में त्रुटि।"
            }
        }
    }

    fun clearLastReceipt() {
        _lastReceipt.value = null
    }

    fun ownerVerifyDonation(donationId: String, status: DonationStatus, note: String) {
        viewModelScope.launch {
            val res = repository.ownerUpdateDonationStatus(donationId, status, note)
            _actionMessage.value = if (res.isSuccess) {
                "दान स्थिति अपडेट हुई: ${status.labelHi}"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "स्थिति अपडेट विफल"
            }
        }
    }

    // ==========================================
    // AI PALMISTRY (GEMINI VISION VIA FIREBASE AI LOGIC)
    // ==========================================

    fun analyzePalm(bitmap: Bitmap, handSide: String) {
        viewModelScope.launch {
            _palmistryState.value = UiState.Loading
            val res = aiRepository.analyzePalmPhoto(bitmap, handSide)
            _palmistryState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "हस्तरेखा विश्लेषण विफल रहा।") }
            )
        }
    }

    fun resetPalmistry() {
        _palmistryState.value = UiState.Idle
    }

    // ==========================================
    // AI DEVOTIONAL ASSISTANT (STREAMING GEMINI VIA FIREBASE AI LOGIC)
    // ==========================================

    fun sendDevotionalQuestion(question: String) {
        val clean = question.trim()
        if (clean.isBlank() || _streamingReply.value != null) return
        val userMsg = ChatMessage(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            content = clean,
            timestampLabel = nowLabel()
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _chatError.value = null
        _streamingReply.value = ""

        viewModelScope.launch {
            try {
                var finalReply = ""
                aiRepository.streamDevotionalResponse(clean, _language.value).collect { partial ->
                    finalReply = partial
                    _streamingReply.value = partial
                }
                _streamingReply.value = null
                if (finalReply.isNotBlank()) {
                    _chatMessages.value = _chatMessages.value + ChatMessage(
                        id = "a_${System.currentTimeMillis()}",
                        role = "assistant",
                        content = finalReply,
                        timestampLabel = nowLabel()
                    )
                }
            } catch (e: TimeoutCancellationException) {
                _streamingReply.value = null
                _chatError.value = "उत्तर प्राप्त करने में समय सीमा (60s) समाप्त हुई। कृपया पुनः प्रयास करें।"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI assistant streaming failed", e)
                _streamingReply.value = null
                _chatError.value = e.localizedMessage ?: "AI सहायक से उत्तर प्राप्त नहीं हो सका।"
            }
        }
    }

    // ==========================================
    // PANCHANG, RASHIFAL & NEWS ADAPTERS
    // ==========================================

    fun refreshPanchang(city: String = _selectedCity.value) {
        _selectedCity.value = city
        viewModelScope.launch {
            _panchangState.value = UiState.Loading
            val result = panchangAdapter.fetchPanchang(city)
            _panchangState.value = UiState.Success(result)
        }
    }

    fun fetchHoroscope(signEn: String, period: String) {
        viewModelScope.launch {
            _horoscopeState.value = UiState.Loading
            val res = astroLogicsAdapter.fetchHoroscope(signEn, period)
            _horoscopeState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "AstroLogics API प्रतीक्षारत है।") }
            )
        }
    }

    fun refreshSpiritualNews() {
        viewModelScope.launch {
            _newsState.value = UiState.Loading
            val res = newsAdapter.fetchSpiritualNews()
            _newsState.value = res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.localizedMessage ?: "News API क्रेडेंशियल प्रतीक्षारत हैं।") }
            )
        }
    }

    // ==========================================
    // OWNER CONTENT MANAGEMENT
    // ==========================================

    fun ownerAddKatha(
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        duration: String,
        speaker: String?
    ) {
        viewModelScope.launch {
            val res = repository.ownerAddKatha(title, description, category, videoUrl, thumbnailUrl, duration, speaker)
            _actionMessage.value = if (res.isSuccess) "नई कथा सफलतापूर्वक प्रकाशित हुई!" else res.exceptionOrNull()?.localizedMessage ?: "त्रुटि"
        }
    }

    fun ownerAddTemple(
        name: String,
        location: String,
        deity: String,
        history: String,
        darshanTimings: String,
        imageUrl: String?,
        videoUrl: String?
    ) {
        viewModelScope.launch {
            val res = repository.ownerAddTemple(name, location, deity, history, darshanTimings, imageUrl, videoUrl)
            _actionMessage.value = if (res.isSuccess) "नया मंदिर विवरण सहेजा गया!" else res.exceptionOrNull()?.localizedMessage ?: "त्रुटि"
        }
    }

    fun ownerAddFestival(
        name: String,
        dateLabel: String,
        significance: String,
        pujaVidhi: String,
        mantra: String,
        kathaSummary: String?,
        relatedVideoUrl: String?
    ) {
        viewModelScope.launch {
            val res = repository.ownerAddFestival(name, dateLabel, significance, pujaVidhi, mantra, kathaSummary, relatedVideoUrl)
            _actionMessage.value = if (res.isSuccess) "नया पर्व जोड़ा गया!" else res.exceptionOrNull()?.localizedMessage ?: "त्रुटि"
        }
    }

    fun ownerAddBhajan(
        title: String,
        artist: String,
        category: String,
        audioUrl: String?,
        lyrics: String?,
        durationLabel: String
    ) {
        viewModelScope.launch {
            val res = repository.ownerAddBhajan(title, artist, category, audioUrl, lyrics, durationLabel)
            _actionMessage.value = if (res.isSuccess) "नया भजन जोड़ा गया!" else res.exceptionOrNull()?.localizedMessage ?: "त्रुटि"
        }
    }

    fun ownerSaveMantraAudio(mantraId: String, mantraText: String, audioUrl: String?, voiceLabel: String) {
        viewModelScope.launch {
            val res = repository.ownerSaveMantraAudio(mantraId, mantraText, audioUrl, voiceLabel)
            _actionMessage.value = if (res.isSuccess) "मंत्र ऑडियो मैपिंग सहेजी गई!" else res.exceptionOrNull()?.localizedMessage ?: "त्रुटि"
        }
    }

    fun ownerDeleteContent(collection: String, docId: String) {
        viewModelScope.launch {
            val res = repository.ownerDeleteDocument(collection, docId)
            _actionMessage.value = if (res.isSuccess) "सामग्री हटाई गई।" else res.exceptionOrNull()?.localizedMessage ?: "हटाने में त्रुटि"
        }
    }

    private fun nowLabel(): String = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date())

    private companion object {
        const val TAG = "BhaktiViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }
}
