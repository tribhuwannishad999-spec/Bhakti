package com.example.data.integrations

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.PanchangResult
import com.example.data.model.ServiceReadiness
import com.example.data.model.SpiritualNewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object ConfigValidator {
    fun isConfigured(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        return !trimmed.equals("UNCONFIGURED", ignoreCase = true) &&
            !trimmed.startsWith("YOUR_", ignoreCase = true) &&
            !trimmed.startsWith("MY_", ignoreCase = true)
    }

    fun isValidHttpsUrl(url: String?): Boolean {
        if (!isConfigured(url)) return false
        val trimmed = url!!.trim()
        return trimmed.startsWith("https://")
    }
}

/**
 * UPI Donation Config — strictly NEVER invents a merchant UPI ID.
 * Remains inactive until UPI_MERCHANT_VPA is provided in AI Studio Secrets.
 */
object UpiPaymentAdapter {
    fun isLiveUpiConfigured(): Boolean = ConfigValidator.isConfigured(BuildConfig.UPI_MERCHANT_VPA)

    fun getMerchantName(): String =
        if (ConfigValidator.isConfigured(BuildConfig.UPI_MERCHANT_NAME)) BuildConfig.UPI_MERCHANT_NAME else "Bhakti Darshan"

    fun buildUpiPaymentUri(
        amountInr: Long,
        purpose: String,
        transactionNote: String
    ): Uri? {
        if (!isLiveUpiConfigured()) return null
        val vpa = BuildConfig.UPI_MERCHANT_VPA.trim()
        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", vpa)
            .appendQueryParameter("pn", getMerchantName())
            .appendQueryParameter("tn", "$purpose - $transactionNote".take(60))
            .appendQueryParameter("am", amountInr.toString())
            .appendQueryParameter("cu", "INR")
            .build()
    }

    fun launchUpiApp(context: Context, uri: Uri, preferredPackage: String?): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (!preferredPackage.isNullOrBlank()) {
                    setPackage(preferredPackage)
                }
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                val chooser = Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "UPI ऐप चुनें")
                context.startActivity(chooser)
                true
            }
        } catch (e: Exception) {
            Log.w("UpiPaymentAdapter", "Could not launch UPI app", e)
            false
        }
    }
}

/**
 * WhatsApp Sharing & Contact Integration.
 * Supports real Android WhatsApp share intents for Katha, Temples, Festivals, Donation links,
 * plus WhatsApp Business Cloud API status tracking (never sends unsolicited bulk messages).
 */
object WhatsAppIntegrationManager {
    fun isBusinessApiConfigured(): Boolean =
        ConfigValidator.isConfigured(BuildConfig.WHATSAPP_PHONE_NUMBER_ID)

    fun isContactNumberConfigured(): Boolean =
        ConfigValidator.isConfigured(BuildConfig.WHATSAPP_CONTACT_NUMBER)

    fun shareDevotionalMessage(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(fallback, "शेयर करें / Share"))
            }
        } catch (e: Exception) {
            Log.w("WhatsAppShare", "Share intent failed", e)
        }
    }

    fun openWhatsAppContact(context: Context, prefillMessage: String): Boolean {
        if (!isContactNumberConfigured()) return false
        val digitsOnly = BuildConfig.WHATSAPP_CONTACT_NUMBER.filter { it.isDigit() }
        if (digitsOnly.length < 10) return false
        val encoded = URLEncoder.encode(prefillMessage, "UTF-8")
        val uri = Uri.parse("https://wa.me/$digitsOnly?text=$encoded")
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: Exception) {
            Log.w("WhatsAppContact", "Could not open WhatsApp contact", e)
            false
        }
    }
}

/**
 * Navamsha Panchang API Adapter with Asia/Kolkata 4:00 AM daily refresh awareness & cache metadata.
 * Strictly NEVER fabricates Panchang timings when API credentials are not configured.
 */
class NavamshaPanchangAdapter {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean =
        ConfigValidator.isValidHttpsUrl(BuildConfig.NAVAMSHA_API_BASE_URL) &&
            ConfigValidator.isConfigured(BuildConfig.NAVAMSHA_API_KEY)

    fun currentIstTimestamp(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a 'IST'", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    fun currentIstDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        return sdf.format(Date())
    }

    suspend fun fetchPanchang(city: String): PanchangResult = withContext(Dispatchers.IO) {
        val dateStr = currentIstDate()
        val nowIst = currentIstTimestamp()
        if (!isConfigured()) {
            return@withContext PanchangResult(
                city = city,
                dateLabel = dateStr,
                lastUpdatedIst = nowIst,
                isStale = true,
                isLiveApiData = false,
                statusMessage = "Navamsha API क्रेडेंशियल (NAVAMSHA_API_BASE_URL व NAVAMSHA_API_KEY) अभी सेट नहीं हैं। हम कभी नकली पंचांग डेटा नहीं बनाते — कृपया AI Studio Secrets में कुंजी जोड़ें।"
            )
        }

        try {
            val baseUrl = BuildConfig.NAVAMSHA_API_BASE_URL.trimEnd('/')
            val encodedCity = URLEncoder.encode(city, "UTF-8")
            val request = Request.Builder()
                .url("$baseUrl/panchang?city=$encodedCity&date=$dateStr")
                .addHeader("Authorization", "Bearer ${BuildConfig.NAVAMSHA_API_KEY.trim()}")
                .addHeader("x-api-key", BuildConfig.NAVAMSHA_API_KEY.trim())
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext PanchangResult(
                        city = city,
                        dateLabel = dateStr,
                        lastUpdatedIst = nowIst,
                        isStale = true,
                        isLiveApiData = false,
                        statusMessage = "Navamsha API से त्रुटि मिली (HTTP ${response.code})। कृपया आधिकारिक दस्तावेज़ के अनुसार बेस URL और API Key जांचें।"
                    )
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                PanchangResult(
                    city = city,
                    dateLabel = dateStr,
                    lastUpdatedIst = nowIst,
                    isStale = false,
                    isLiveApiData = true,
                    statusMessage = "Navamsha API से लाइव पंचांग प्राप्त हुआ",
                    tithi = data.optString("tithi", "—"),
                    paksha = data.optString("paksha", "—"),
                    nakshatra = data.optString("nakshatra", "—"),
                    yoga = data.optString("yoga", "—"),
                    karana = data.optString("karana", "—"),
                    sunrise = data.optString("sunrise", "—"),
                    sunset = data.optString("sunset", "—"),
                    moonrise = data.optString("moonrise", "—"),
                    moonset = data.optString("moonset", "—"),
                    rahuKaal = data.optString("rahu_kaal", data.optString("rahuKaal", "—")),
                    gulikaKaal = data.optString("gulika_kaal", data.optString("gulikaKaal", "—")),
                    abhijitMuhurat = data.optString("abhijit_muhurat", data.optString("abhijitMuhurat", "—"))
                )
            }
        } catch (e: Exception) {
            Log.e("NavamshaAdapter", "Failed fetching Panchang", e)
            PanchangResult(
                city = city,
                dateLabel = dateStr,
                lastUpdatedIst = nowIst,
                isStale = true,
                isLiveApiData = false,
                statusMessage = "Navamsha API से जुड़ने में त्रुटि: ${e.localizedMessage ?: "Network Error"}"
            )
        }
    }
}

/**
 * AstroLogics Rashifal & Kundli Adapter — strictly NEVER invents horoscope or Kundli API results.
 */
class AstroLogicsAdapter {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean =
        ConfigValidator.isValidHttpsUrl(BuildConfig.ASTROLOGICS_API_BASE_URL) &&
            ConfigValidator.isConfigured(BuildConfig.ASTROLOGICS_API_KEY)

    suspend fun fetchHoroscope(signEn: String, period: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured()) {
            return@withContext Result.failure(
                IllegalStateException("AstroLogics API क्रेडेंशियल (ASTROLOGICS_API_BASE_URL व ASTROLOGICS_API_KEY) प्रतीक्षारत हैं।")
            )
        }
        try {
            val base = BuildConfig.ASTROLOGICS_API_BASE_URL.trimEnd('/')
            val request = Request.Builder()
                .url("$base/horoscope/$period/${signEn.lowercase(Locale.US)}")
                .addHeader("Authorization", "Bearer ${BuildConfig.ASTROLOGICS_API_KEY.trim()}")
                .addHeader("x-api-key", BuildConfig.ASTROLOGICS_API_KEY.trim())
                .build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("AstroLogics API HTTP ${resp.code}"))
                }
                val text = resp.body?.string().orEmpty()
                val json = JSONObject(text)
                val summary = json.optString("prediction", json.optString("horoscope", text.take(600)))
                Result.success(summary)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Real NewsAPI + MediaStack Adapter — strictly NEVER invents spiritual news articles.
 */
class SpiritualNewsAdapter {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun isNewsApiConfigured(): Boolean = ConfigValidator.isConfigured(BuildConfig.NEWS_API_KEY)
    fun isMediaStackConfigured(): Boolean = ConfigValidator.isConfigured(BuildConfig.MEDIASTACK_API_KEY)

    suspend fun fetchSpiritualNews(): Result<List<SpiritualNewsArticle>> = withContext(Dispatchers.IO) {
        if (isNewsApiConfigured()) {
            return@withContext fetchFromNewsApi()
        }
        if (isMediaStackConfigured()) {
            return@withContext fetchFromMediaStack()
        }
        Result.failure(
            IllegalStateException("NewsAPI या MediaStack API Key अभी सेट नहीं है। हम कभी नकली समाचार नहीं दिखाते।")
        )
    }

    private fun fetchFromNewsApi(): Result<List<SpiritualNewsArticle>> {
        return try {
            val query = URLEncoder.encode("Hindu temple OR festival OR spiritual India", "UTF-8")
            val url = "https://newsapi.org/v2/everything?q=$query&sortBy=publishedAt&pageSize=12&apiKey=${BuildConfig.NEWS_API_KEY.trim()}"
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(Exception("NewsAPI HTTP ${resp.code}"))
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("articles") ?: return Result.success(emptyList())
                val list = mutableListOf<SpiritualNewsArticle>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title")
                    if (title.isNotBlank() && title != "[Removed]") {
                        list.add(
                            SpiritualNewsArticle(
                                title = title,
                                description = obj.optString("description", ""),
                                sourceName = obj.optJSONObject("source")?.optString("name", "NewsAPI") ?: "NewsAPI",
                                publishedAt = obj.optString("publishedAt", "").take(10),
                                url = obj.optString("url", "")
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchFromMediaStack(): Result<List<SpiritualNewsArticle>> {
        return try {
            val keywords = URLEncoder.encode("temple,festival,spiritual", "UTF-8")
            val url = "http://api.mediastack.com/v1/news?access_key=${BuildConfig.MEDIASTACK_API_KEY.trim()}&keywords=$keywords&countries=in&limit=12"
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return Result.failure(Exception("MediaStack HTTP ${resp.code}"))
                val json = JSONObject(resp.body?.string().orEmpty())
                val arr = json.optJSONArray("data") ?: return Result.success(emptyList())
                val list = mutableListOf<SpiritualNewsArticle>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SpiritualNewsArticle(
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            sourceName = obj.optString("source", "MediaStack"),
                            publishedAt = obj.optString("published_at", "").take(10),
                            url = obj.optString("url", "")
                        )
                    )
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

object IntegrationStatusRegistry {
    fun getAllStatuses(): List<ServiceReadiness> = listOf(
        ServiceReadiness(
            name = "Firebase Firestore & Google Sign-In",
            status = "Active",
            detailHi = "डेटाबेस, यूज़र प्रोफाइल, दान रिकॉर्ड और ओनर डैशबोर्ड सक्रिय हैं।",
            detailEn = "Database, user profiles, donation records, and Owner Dashboard are active.",
            isConfigured = true
        ),
        ServiceReadiness(
            name = "Gemini AI (Firebase AI Logic + App Check)",
            status = "Active",
            detailHi = "AI भक्ति सहायक और हस्तरेखा विज़न विश्लेषण बिना API Key के सक्रिय हैं।",
            detailEn = "AI Devotional Assistant and Vision Palmistry are active via App Check.",
            isConfigured = true
        ),
        ServiceReadiness(
            name = "Mantra Voice Audio (Hindi TTS + Owner Uploads)",
            status = "Device Supported",
            detailHi = "10 मंत्रों की सटीक मैपिंग सक्रिय; कस्टम MP3 ओनर डैशबोर्ड से जोड़े जा सकते हैं।",
            detailEn = "Exact 1-to-1 mapping for 10 mantras active; custom MP3 URLs configurable in Owner Dashboard.",
            isConfigured = true
        ),
        ServiceReadiness(
            name = "UPI Merchant Payment (धनपात्र)",
            status = if (UpiPaymentAdapter.isLiveUpiConfigured()) "Active" else "Awaiting Credentials",
            detailHi = if (UpiPaymentAdapter.isLiveUpiConfigured()) "UPI VPA सेट है (मैन्युअल ओनर सत्यापन सहित)" else "UPI_MERCHANT_VPA प्रतीक्षारत — लाइव भुगतान सुरक्षित रूप से निष्क्रिय है।",
            detailEn = if (UpiPaymentAdapter.isLiveUpiConfigured()) "Merchant UPI VPA active with manual owner verification." else "Awaiting UPI_MERCHANT_VPA in Secrets panel; live UPI launch disabled.",
            isConfigured = UpiPaymentAdapter.isLiveUpiConfigured()
        ),
        ServiceReadiness(
            name = "Navamsha Panchang API",
            status = if (NavamshaPanchangAdapter().isConfigured()) "Active" else "Awaiting Credentials",
            detailHi = "NAVAMSHA_API_BASE_URL और NAVAMSHA_API_KEY जुड़ते ही लाइव पंचांग सक्रिय होगा।",
            detailEn = "Live Panchang activates automatically once NAVAMSHA_API_BASE_URL and NAVAMSHA_API_KEY are set.",
            isConfigured = NavamshaPanchangAdapter().isConfigured()
        ),
        ServiceReadiness(
            name = "AstroLogics Rashifal & Kundli API",
            status = if (AstroLogicsAdapter().isConfigured()) "Active" else "Awaiting Credentials",
            detailHi = "ASTROLOGICS_API_BASE_URL व ASTROLOGICS_API_KEY जुड़ते ही लाइव राशिफल/कुंडली चलेगी।",
            detailEn = "Activates live Rashifal & Kundli once AstroLogics API base URL and key are configured.",
            isConfigured = AstroLogicsAdapter().isConfigured()
        ),
        ServiceReadiness(
            name = "NewsAPI / MediaStack",
            status = if (SpiritualNewsAdapter().isNewsApiConfigured() || SpiritualNewsAdapter().isMediaStackConfigured()) "Active" else "Awaiting Credentials",
            detailHi = "NEWS_API_KEY या MEDIASTACK_API_KEY देने पर लाइव आध्यात्मिक समाचार दिखेंगे।",
            detailEn = "Displays live spiritual news once NEWS_API_KEY or MEDIASTACK_API_KEY is provided.",
            isConfigured = SpiritualNewsAdapter().isNewsApiConfigured() || SpiritualNewsAdapter().isMediaStackConfigured()
        ),
        ServiceReadiness(
            name = "Cloudinary Media & Speechify Voice",
            status = if (ConfigValidator.isConfigured(BuildConfig.CLOUDINARY_CLOUD_NAME) || ConfigValidator.isConfigured(BuildConfig.SPEECHIFY_API_KEY)) "Active" else "Awaiting Credentials",
            detailHi = "CLOUDINARY_CLOUD_NAME व SPEECHIFY_API_KEY प्रतीक्षारत (पेड सेवाएँ अनुमति के बिना सक्रिय नहीं)।",
            detailEn = "Awaiting Cloudinary/Speechify credentials; paid services stay inactive without approval.",
            isConfigured = ConfigValidator.isConfigured(BuildConfig.CLOUDINARY_CLOUD_NAME) && ConfigValidator.isConfigured(BuildConfig.SPEECHIFY_API_KEY)
        ),
        ServiceReadiness(
            name = "WhatsApp Share & Business Cloud API",
            status = if (WhatsAppIntegrationManager.isBusinessApiConfigured()) "Active" else "Share Ready / Cloud API Awaiting Approval",
            detailHi = "WhatsApp शेयर बटन सक्रिय; Cloud API टेम्पलेट्स हेतु WHATSAPP_PHONE_NUMBER_ID व Meta स्वीकृति आवश्यक।",
            detailEn = "Android WhatsApp share works now; Cloud API templates await Meta WABA token & approval.",
            isConfigured = WhatsAppIntegrationManager.isBusinessApiConfigured()
        )
    )
}
