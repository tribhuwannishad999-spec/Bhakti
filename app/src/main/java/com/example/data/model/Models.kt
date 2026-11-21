package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

// ==========================================
// 1. MANTRA BUBBLE GAME CATALOG (EXACT 10 MANTRAS)
// ==========================================
enum class MantraItem(
    val id: String,
    val mantraHindi: String,
    val transliteration: String,
    val deityHindi: String,
    val deityEnglish: String,
    val speechTextHindi: String,
    val bubblePrimaryHex: Long,
    val bubbleSecondaryHex: Long,
    val punyaPoints: Int
) {
    OM_NAMAH_SHIVAYA(
        id = "om_namah_shivaya",
        mantraHindi = "ॐ नमः शिवाय",
        transliteration = "Om Namah Shivaya",
        deityHindi = "भगवान शिव",
        deityEnglish = "Lord Shiva",
        speechTextHindi = "ॐ नमः शिवाय",
        bubblePrimaryHex = 0xFF6A1B9A,
        bubbleSecondaryHex = 0xFF311B92,
        punyaPoints = 10
    ),
    JAI_SHRI_RAM(
        id = "jai_shri_ram",
        mantraHindi = "जय श्रीराम",
        transliteration = "Jai Shri Ram",
        deityHindi = "प्रभु श्रीराम",
        deityEnglish = "Lord Shri Ram",
        speechTextHindi = "जय श्रीराम",
        bubblePrimaryHex = 0xFFE65100,
        bubbleSecondaryHex = 0xFFBF360C,
        punyaPoints = 10
    ),
    JAI_SHRI_KRISHNA(
        id = "jai_shri_krishna",
        mantraHindi = "जय श्रीकृष्ण",
        transliteration = "Jai Shri Krishna",
        deityHindi = "भगवान श्रीकृष्ण",
        deityEnglish = "Lord Shri Krishna",
        speechTextHindi = "जय श्रीकृष्ण",
        bubblePrimaryHex = 0xFF1565C0,
        bubbleSecondaryHex = 0xFF0D47A1,
        punyaPoints = 10
    ),
    OM_HANUMATE_NAMAH(
        id = "om_hanumate_namah",
        mantraHindi = "ॐ हनुमते नमः",
        transliteration = "Om Hanumate Namah",
        deityHindi = "श्री हनुमान जी",
        deityEnglish = "Lord Hanuman",
        speechTextHindi = "ॐ हनुमते नमः",
        bubblePrimaryHex = 0xFFD84315,
        bubbleSecondaryHex = 0xFF8E1B08,
        punyaPoints = 10
    ),
    OM_GAM_GANAPATAYE(
        id = "om_gam_ganapataye",
        mantraHindi = "ॐ गं गणपतये नमः",
        transliteration = "Om Gam Ganapataye Namah",
        deityHindi = "श्री गणेश जी",
        deityEnglish = "Lord Ganesha",
        speechTextHindi = "ॐ गं गणपतये नमः",
        bubblePrimaryHex = 0xFFC62828,
        bubbleSecondaryHex = 0xFF7F0000,
        punyaPoints = 10
    ),
    JAI_MATA_DI(
        id = "jai_mata_di",
        mantraHindi = "जय माता दी",
        transliteration = "Jai Mata Di",
        deityHindi = "माँ दुर्गा",
        deityEnglish = "Maa Durga",
        speechTextHindi = "जय माता दी",
        bubblePrimaryHex = 0xFFAD1457,
        bubbleSecondaryHex = 0xFF6A0735,
        punyaPoints = 10
    ),
    OM_NAMO_NARAYANAYA(
        id = "om_namo_narayanaya",
        mantraHindi = "ॐ नमो नारायणाय",
        transliteration = "Om Namo Narayanaya",
        deityHindi = "भगवान विष्णु",
        deityEnglish = "Lord Narayana",
        speechTextHindi = "ॐ नमो नारायणाय",
        bubblePrimaryHex = 0xFF00695C,
        bubbleSecondaryHex = 0xFF004D40,
        punyaPoints = 10
    ),
    OM_SURYAYA_NAMAH(
        id = "om_suryaya_namah",
        mantraHindi = "ॐ सूर्याय नमः",
        transliteration = "Om Suryaya Namah",
        deityHindi = "सूर्य देव",
        deityEnglish = "Surya Dev",
        speechTextHindi = "ॐ सूर्याय नमः",
        bubblePrimaryHex = 0xFFEF6C00,
        bubbleSecondaryHex = 0xFFB24A00,
        punyaPoints = 10
    ),
    OM_SHANAISHCHARAYA(
        id = "om_shanaishcharaya",
        mantraHindi = "ॐ शनैश्चराय नमः",
        transliteration = "Om Shanaishcharaya Namah",
        deityHindi = "शनि देव",
        deityEnglish = "Shani Dev",
        speechTextHindi = "ॐ शनैश्चराय नमः",
        bubblePrimaryHex = 0xFF283593,
        bubbleSecondaryHex = 0xFF1A237E,
        punyaPoints = 10
    ),
    OM_NAMO_BHAGAVATE_VASUDEVAYA(
        id = "om_namo_bhagavate_vasudevaya",
        mantraHindi = "ॐ नमो भगवते वासुदेवाय",
        transliteration = "Om Namo Bhagavate Vasudevaya",
        deityHindi = "श्री वासुदेव",
        deityEnglish = "Lord Vasudeva",
        speechTextHindi = "ॐ नमो भगवते वासुदेवाय",
        bubblePrimaryHex = 0xFFB8860B,
        bubbleSecondaryHex = 0xFF6D4C00,
        punyaPoints = 15
    );

    companion object {
        fun fromId(id: String): MantraItem = entries.firstOrNull { it.id == id } ?: OM_NAMAH_SHIVAYA
    }
}

// ==========================================
// 2. FIRESTORE DATA MODELS (ALL WITH @Keep, @IgnoreExtraProperties, @DocumentId, DEFAULTS)
// ==========================================

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "भक्त",
    val language: String = "hi",
    val punyaScore: Long = 0L,
    val totalChants: Long = 0L,
    val sadhanaLevel: Long = 1L,
    val bestScore: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteItem(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val itemType: String = "bhajan",
    val title: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class DonationStatus(val labelHi: String, val labelEn: String) {
    PENDING_VERIFICATION("सत्यापन लंबित", "Pending Verification"),
    VERIFIED_SUCCESS("सत्यापित एवं सफल", "Verified Success"),
    FAILED("असफल", "Failed"),
    REFUNDED("रिफंड किया गया", "Refunded");

    companion object {
        fun fromRaw(raw: String): DonationStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_VERIFICATION
    }
}

@Keep
@IgnoreExtraProperties
data class DonationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val donorName: String = "",
    val donorPhone: String? = null,
    val donorEmail: String? = null,
    val amountInr: Long = 0L,
    val purpose: String = "",
    val paymentApp: String = "UPI",
    val transactionRef: String = "",
    val status: String = DonationStatus.PENDING_VERIFICATION.name,
    val receiptNumber: String = "",
    val ownerNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KathaVideo(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "शिव कथा",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val duration: String = "15:00",
    val speaker: String? = null,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TempleEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val location: String = "",
    val deity: String = "",
    val history: String = "",
    val darshanTimings: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FestivalEntry(
    @DocumentId val id: String = "",
    val name: String = "",
    val dateLabel: String = "",
    val significance: String = "",
    val pujaVidhi: String = "",
    val mantra: String = "",
    val kathaSummary: String? = null,
    val relatedVideoUrl: String? = null,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BhajanTrack(
    @DocumentId val id: String = "",
    val title: String = "",
    val artist: String = "",
    val category: String = "Shiva",
    val audioUrl: String? = null,
    val lyrics: String? = null,
    val durationLabel: String = "04:30",
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MantraAudioMapping(
    @DocumentId val id: String = "",
    val mantraText: String = "",
    val audioUrl: String? = null,
    val voiceLabel: String = "Hindi TTS (Device Verified)",
    val customRecording: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// ==========================================
// 3. DOMAIN MODELS (PALMISTRY, PANCHANG, RASHIFAL, VRAT, NEWS, AI CHAT)
// ==========================================

data class VratInfo(
    val id: String,
    val titleHi: String,
    val titleEn: String,
    val frequencyHi: String,
    val deityHi: String,
    val significanceHi: String,
    val vidhiHi: String,
    val mantra: String
)

data class ZodiacSignInfo(
    val id: String,
    val nameHi: String,
    val nameEn: String,
    val symbol: String,
    val elementHi: String,
    val rulingPlanetHi: String
)

data class PalmistryAnalysis(
    val isClearPalm: Boolean,
    val qualityFeedback: String,
    val handAnalyzed: String,
    val lifeLineHi: String,
    val lifeLineEn: String,
    val headLineHi: String,
    val headLineEn: String,
    val heartLineHi: String,
    val heartLineEn: String,
    val fateLineHi: String,
    val fateLineEn: String,
    val sunLineHi: String,
    val sunLineEn: String,
    val majorMountsHi: String,
    val majorMountsEn: String,
    val overallSummaryHi: String,
    val overallSummaryEn: String
)

data class PanchangResult(
    val city: String,
    val dateLabel: String,
    val lastUpdatedIst: String,
    val isStale: Boolean,
    val isLiveApiData: Boolean,
    val statusMessage: String,
    val tithi: String = "—",
    val paksha: String = "—",
    val nakshatra: String = "—",
    val yoga: String = "—",
    val karana: String = "—",
    val sunrise: String = "—",
    val sunset: String = "—",
    val moonrise: String = "—",
    val moonset: String = "—",
    val rahuKaal: String = "—",
    val gulikaKaal: String = "—",
    val abhijitMuhurat: String = "—"
)

data class SpiritualNewsArticle(
    val title: String,
    val description: String,
    val sourceName: String,
    val publishedAt: String,
    val url: String
)

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestampLabel: String
)

data class ServiceReadiness(
    val name: String,
    val status: String, // "Active", "Awaiting Credentials", "Device Supported"
    val detailHi: String,
    val detailEn: String,
    val isConfigured: Boolean
)

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
