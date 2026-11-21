package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.PalmistryAnalysis
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

/**
 * AI Devotional & Vision Palmistry repository powered by Firebase AI Logic (Gemini 3.8 Flash).
 * Lazily initializes Firebase.ai models, applies realistic 60s coroutine timeouts,
 * uses strict JSON Schema.obj for Palmistry, and refuses to fabricate unclear palm readings.
 */
class AiDevotionalRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Bhakti Darshan's respectful Hindu devotional assistant. " +
                        "Answer in warm, clear, respectful Hindi or English matching the user's selected language. " +
                        "Explain mantras, Hindu festivals, vrat rules, temple traditions, and spiritual concepts accurately. " +
                        "NEVER invent scripture verses, fake citations, or fabricated historical facts. " +
                        "If you are uncertain about a specific regional custom or verse number, state it honestly."
                )
            }
        )
    }

    private val palmistrySchema = Schema.obj(
        properties = mapOf(
            "isClearPalm" to Schema.boolean(description = "True only if a clear human palm with visible lines is shown"),
            "qualityFeedback" to Schema.string(description = "Image clarity feedback in Hindi and English"),
            "lifeLineHi" to Schema.string(description = "Traditional Life Line observation in Hindi, or clear note if not visible"),
            "lifeLineEn" to Schema.string(description = "Traditional Life Line observation in English"),
            "headLineHi" to Schema.string(description = "Traditional Head Line observation in Hindi"),
            "headLineEn" to Schema.string(description = "Traditional Head Line observation in English"),
            "heartLineHi" to Schema.string(description = "Traditional Heart Line observation in Hindi"),
            "heartLineEn" to Schema.string(description = "Traditional Heart Line observation in English"),
            "fateLineHi" to Schema.string(description = "Traditional Fate Line observation in Hindi"),
            "fateLineEn" to Schema.string(description = "Traditional Fate Line observation in English"),
            "sunLineHi" to Schema.string(description = "Traditional Sun Line observation in Hindi"),
            "sunLineEn" to Schema.string(description = "Traditional Sun Line observation in English"),
            "majorMountsHi" to Schema.string(description = "Major Palm Mounts (Guru, Shani, Surya, Budh, Shukra, Chandra) in Hindi"),
            "majorMountsEn" to Schema.string(description = "Major Palm Mounts interpretation in English"),
            "overallSummaryHi" to Schema.string(description = "Balanced traditional Hastarekha summary in Hindi"),
            "overallSummaryEn" to Schema.string(description = "Balanced traditional Hastarekha summary in English")
        )
    )

    private val palmistryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = palmistrySchema
            }
        )
    }

    fun streamDevotionalResponse(
        userPrompt: String,
        language: String
    ): Flow<String> = flow {
        val langInstruction = if (language == "hi") {
            "कृपया उत्तर सरल, शुद्ध और भक्तिपूर्ण हिंदी में दें:\n"
        } else {
            "Please reply in warm, respectful, clear English:\n"
        }
        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chatModel.generateContentStream("$langInstruction$userPrompt").collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    accumulated.append(text)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("AI returned an empty response. Please try again.")
        }
    }

    suspend fun analyzePalmPhoto(
        bitmap: Bitmap,
        handSide: String
    ): Result<PalmistryAnalysis> {
        return try {
            val scaledBitmap = scaleBitmapForVision(bitmap, maxDim = 1024)
            val response = withTimeout(60_000L) {
                palmistryModel.generateContent(
                    content {
                        image(scaledBitmap)
                        text(
                            "Analyze this photo of a user's $handSide palm using traditional Indian Hastarekha Shastra (palmistry). " +
                                "CRITICAL HONESTY RULE: First evaluate if the image clearly shows an open human palm with discernible palm lines in adequate lighting. " +
                                "If the photo is NOT a human palm, is blurry, too dark, cut off, or lines cannot be reliably seen, you MUST set isClearPalm=false and NEVER fabricate line interpretations—instead explain in qualityFeedback how to take a clearer photo, and put 'स्पष्ट नहीं / Not clearly visible' in the line fields. " +
                                "If isClearPalm=true, provide thoughtful, traditional Hastarekha interpretive commentary in BOTH Hindi and English for: Life Line (जीवन रेखा), Head Line (मस्तिष्क रेखा), Heart Line (हृदय रेखा), Fate Line (भाग्य रेखा), Sun Line (सूर्य रेखा), and Major Palm Mounts (प्रमुख पर्वत). " +
                                "Always frame interpretations as traditional cultural/spiritual reflection, never medical, legal, or guaranteed scientific prediction."
                        )
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Vision model returned empty JSON.")
            val json = JSONObject(rawJson)
            Result.success(
                PalmistryAnalysis(
                    isClearPalm = json.optBoolean("isClearPalm", false),
                    qualityFeedback = json.optString("qualityFeedback", ""),
                    handAnalyzed = handSide,
                    lifeLineHi = json.optString("lifeLineHi", ""),
                    lifeLineEn = json.optString("lifeLineEn", ""),
                    headLineHi = json.optString("headLineHi", ""),
                    headLineEn = json.optString("headLineEn", ""),
                    heartLineHi = json.optString("heartLineHi", ""),
                    heartLineEn = json.optString("heartLineEn", ""),
                    fateLineHi = json.optString("fateLineHi", ""),
                    fateLineEn = json.optString("fateLineEn", ""),
                    sunLineHi = json.optString("sunLineHi", ""),
                    sunLineEn = json.optString("sunLineEn", ""),
                    majorMountsHi = json.optString("majorMountsHi", ""),
                    majorMountsEn = json.optString("majorMountsEn", ""),
                    overallSummaryHi = json.optString("overallSummaryHi", ""),
                    overallSummaryEn = json.optString("overallSummaryEn", "")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Palmistry vision analysis timed out", e)
            Result.failure(Exception("हस्तरेखा विश्लेषण का समय समाप्त हो गया (60s)। कृपया पुनः प्रयास करें।"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Palmistry vision analysis failed", e)
            Result.failure(e)
        }
    }

    private fun scaleBitmapForVision(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        return Bitmap.createScaledBitmap(
            source,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }

    private companion object {
        const val TAG = "AiDevotionalRepo"
    }
}
