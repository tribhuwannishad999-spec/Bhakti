package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.BhajanTrack
import com.example.data.model.DonationRecord
import com.example.data.model.DonationStatus
import com.example.data.model.FavoriteItem
import com.example.data.model.FestivalEntry
import com.example.data.model.KathaVideo
import com.example.data.model.MantraAudioMapping
import com.example.data.model.TempleEntry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class BhaktiRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, expectedUid: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == expectedUid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // ==========================================
    // USER PROFILE & MANTRA GAME CLOUD PROGRESS
    // ==========================================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile observation" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .logFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun ensureUserProfile(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "भक्त" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "language" to "hi",
                    "punyaScore" to 0L,
                    "totalChants" to 0L,
                    "sadhanaLevel" to 1L,
                    "bestScore" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveGameProgress(
        punyaScore: Long,
        totalChants: Long,
        sadhanaLevel: Long,
        bestScore: Long
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val updates = mapOf(
                "punyaScore" to punyaScore.coerceIn(0L, 100_000_000L),
                "totalChants" to totalChants.coerceIn(0L, 100_000_000L),
                "sadhanaLevel" to sadhanaLevel.coerceIn(1L, 1000L),
                "bestScore" to bestScore.coerceIn(0L, 100_000_000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferredLanguage(languageCode: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeLang = if (languageCode == "en") "en" else "hi"
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "language" to safeLang,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // FAVORITES
    // ==========================================

    fun observeFavorites(userId: String): Flow<List<FavoriteItem>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$uid/favorites"
        emitAll(
            db.collection("users").document(uid).collection("favorites")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FavoriteItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun toggleFavorite(itemId: String, itemType: String, title: String, currentlyFavorited: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val safeId = "${itemType}_${itemId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        val path = "users/$uid/favorites/$safeId"
        return try {
            val ref = db.collection("users").document(uid).collection("favorites").document(safeId)
            if (currentlyFavorited) {
                ref.delete().await()
            } else {
                val safeType = if (itemType in listOf("katha", "bhajan", "temple", "festival")) itemType else "bhajan"
                ref.set(
                    mapOf(
                        "itemId" to itemId.take(128),
                        "itemType" to safeType,
                        "title" to title.trim().ifBlank { "Bhakti" }.take(160),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // DHANPATRA DONATIONS (STRICT VERIFICATION LIFECYCLE)
    // ==========================================

    fun observeUserDonations(userId: String): Flow<List<DonationRecord>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "donations"
        emitAll(
            db.collection("donations")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAllDonationsForOwner(): Flow<List<DonationRecord>> = flow {
        val uid = requireUserId()
        val path = "donations"
        emitAll(
            db.collection("donations")
                .snapshots()
                .map { snap ->
                    snap.toObjects(DonationRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .logFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    /**
     * Records a donation submission strictly as PENDING_VERIFICATION.
     * Prevents duplicate transaction references per user and NEVER fakes VERIFIED_SUCCESS.
     */
    suspend fun submitPendingDonation(
        donorName: String,
        donorPhone: String?,
        donorEmail: String?,
        amountInr: Long,
        purpose: String,
        paymentApp: String,
        transactionRef: String
    ): Result<DonationRecord> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanName = donorName.trim()
        val cleanRef = transactionRef.trim().uppercase(Locale.US)
        val cleanPurpose = purpose.trim().ifBlank { "भक्ति दर्शन सेवा" }
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("कृपया दाता का सही नाम दर्ज करें।"))
        if (amountInr < 1L || amountInr > 10_000_000L) return Result.failure(IllegalArgumentException("कृपया मान्य दान राशि दर्ज करें।"))
        if (cleanRef.length < 4 || cleanRef.length > 64) return Result.failure(IllegalArgumentException("कृपया मान्य UTR / Transaction Reference दर्ज करें (कम से कम 4 अक्षर)।"))

        // Duplicate transaction protection using deterministic sanitized ID derived from ref
        val safeDocId = "don_${cleanRef.replace(Regex("[^A-Za-z0-9_\\-]"), "_")}".take(120)
        val path = "donations/$safeDocId"
        return try {
            val existingQuery = db.collection("donations")
                .whereEqualTo("userId", uid)
                .whereEqualTo("transactionRef", cleanRef)
                .get()
                .await()
            if (!existingQuery.isEmpty) {
                return Result.failure(IllegalStateException("यह Transaction Reference पहले ही दर्ज किया जा चुका है (Duplicate blocked)।"))
            }

            val receiptNo = "BD-${System.currentTimeMillis() % 1000000}-${cleanRef.takeLast(4)}"
            val allowedApp = if (paymentApp in listOf("UPI", "Google Pay", "PhonePe", "Paytm", "Other UPI", "Gateway")) {
                paymentApp
            } else "UPI"

            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "donorName" to cleanName.take(100),
                "donorPhone" to donorPhone?.trim()?.takeIf { it.isNotBlank() }?.take(20),
                "donorEmail" to donorEmail?.trim()?.takeIf { it.isNotBlank() }?.take(120),
                "amountInr" to amountInr,
                "purpose" to cleanPurpose.take(120),
                "paymentApp" to allowedApp,
                "transactionRef" to cleanRef,
                "status" to DonationStatus.PENDING_VERIFICATION.name,
                "receiptNumber" to receiptNo.take(64),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("donations").document(safeDocId).set(payload).await()
            Result.success(
                DonationRecord(
                    id = safeDocId,
                    userId = uid,
                    donorName = cleanName,
                    donorPhone = donorPhone?.takeIf { it.isNotBlank() },
                    donorEmail = donorEmail?.takeIf { it.isNotBlank() },
                    amountInr = amountInr,
                    purpose = cleanPurpose,
                    paymentApp = allowedApp,
                    transactionRef = cleanRef,
                    status = DonationStatus.PENDING_VERIFICATION.name,
                    receiptNumber = receiptNo
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun ownerUpdateDonationStatus(
        donationId: String,
        newStatus: DonationStatus,
        ownerNote: String
    ): Result<Unit> {
        val path = "donations/$donationId"
        return try {
            requireUserId()
            require(donationId.isNotBlank())
            val updates = mutableMapOf<String, Any?>(
                "status" to newStatus.name,
                "ownerNote" to ownerNote.trim().takeIf { it.isNotBlank() }?.take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("donations").document(donationId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // ==========================================
    // PUBLISHED DEVOTIONAL CATALOG STREAMS
    // ==========================================

    fun observePublishedKathas(): Flow<List<KathaVideo>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("kathas")
                .whereEqualTo("published", true)
                .snapshots()
                .map { it.toObjects(KathaVideo::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "kathas", uid)
        )
    }

    fun observePublishedTemples(): Flow<List<TempleEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("temples")
                .whereEqualTo("published", true)
                .snapshots()
                .map { it.toObjects(TempleEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "temples", uid)
        )
    }

    fun observePublishedFestivals(): Flow<List<FestivalEntry>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("festivals")
                .whereEqualTo("published", true)
                .snapshots()
                .map { it.toObjects(FestivalEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "festivals", uid)
        )
    }

    fun observePublishedBhajans(): Flow<List<BhajanTrack>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("bhajans")
                .whereEqualTo("published", true)
                .snapshots()
                .map { it.toObjects(BhajanTrack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "bhajans", uid)
        )
    }

    fun observePublishedMantraAudio(): Flow<List<MantraAudioMapping>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("mantra_audio")
                .whereEqualTo("published", true)
                .snapshots()
                .map { it.toObjects(MantraAudioMapping::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(OperationType.LIST, "mantra_audio", uid)
        )
    }

    // ==========================================
    // OWNER DASHBOARD CONTENT MANAGEMENT (ADMIN ONLY IN RULES)
    // ==========================================

    suspend fun ownerAddKatha(
        title: String,
        description: String,
        category: String,
        videoUrl: String,
        thumbnailUrl: String?,
        duration: String,
        speaker: String?
    ): Result<Unit> {
        val docRef = db.collection("kathas").document()
        return try {
            requireUserId()
            val payload = mapOf(
                "title" to title.trim().take(160),
                "description" to description.trim().take(1500),
                "category" to category.trim().take(80),
                "videoUrl" to videoUrl.trim().take(500),
                "thumbnailUrl" to thumbnailUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "duration" to duration.trim().ifBlank { "15:00" }.take(30),
                "speaker" to speaker?.trim()?.takeIf { it.isNotBlank() }?.take(120),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ownerDeleteDocument(collection: String, docId: String): Result<Unit> {
        val path = "$collection/$docId"
        return try {
            requireUserId()
            db.collection(collection).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun ownerAddTemple(
        name: String,
        location: String,
        deity: String,
        history: String,
        darshanTimings: String,
        imageUrl: String?,
        videoUrl: String?
    ): Result<Unit> {
        val docRef = db.collection("temples").document()
        return try {
            requireUserId()
            val payload = mapOf(
                "name" to name.trim().take(160),
                "location" to location.trim().take(160),
                "deity" to deity.trim().take(120),
                "history" to history.trim().take(2000),
                "darshanTimings" to darshanTimings.trim().take(300),
                "imageUrl" to imageUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "videoUrl" to videoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ownerAddFestival(
        name: String,
        dateLabel: String,
        significance: String,
        pujaVidhi: String,
        mantra: String,
        kathaSummary: String?,
        relatedVideoUrl: String?
    ): Result<Unit> {
        val docRef = db.collection("festivals").document()
        return try {
            requireUserId()
            val payload = mapOf(
                "name" to name.trim().take(160),
                "dateLabel" to dateLabel.trim().take(120),
                "significance" to significance.trim().take(1500),
                "pujaVidhi" to pujaVidhi.trim().take(1500),
                "mantra" to mantra.trim().take(300),
                "kathaSummary" to kathaSummary?.trim()?.takeIf { it.isNotBlank() }?.take(1500),
                "relatedVideoUrl" to relatedVideoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ownerAddBhajan(
        title: String,
        artist: String,
        category: String,
        audioUrl: String?,
        lyrics: String?,
        durationLabel: String
    ): Result<Unit> {
        val docRef = db.collection("bhajans").document()
        return try {
            requireUserId()
            val payload = mapOf(
                "title" to title.trim().take(160),
                "artist" to artist.trim().ifBlank { "पारंपरिक" }.take(120),
                "category" to category.trim().take(60),
                "audioUrl" to audioUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "lyrics" to lyrics?.trim()?.takeIf { it.isNotBlank() }?.take(4000),
                "durationLabel" to durationLabel.trim().ifBlank { "04:30" }.take(30),
                "published" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun ownerSaveMantraAudio(
        mantraId: String,
        mantraText: String,
        audioUrl: String?,
        voiceLabel: String
    ): Result<Unit> {
        val path = "mantra_audio/$mantraId"
        return try {
            requireUserId()
            val hasCustomUrl = !audioUrl.isNullOrBlank() &&
                (audioUrl.startsWith("https://") || audioUrl.startsWith("http://"))
            val payload = mapOf(
                "mantraText" to mantraText.take(120),
                "audioUrl" to audioUrl?.trim()?.takeIf { it.isNotBlank() }?.take(500),
                "voiceLabel" to voiceLabel.trim().ifBlank {
                    if (hasCustomUrl) "Owner Studio Recording" else "Hindi TTS"
                }.take(120),
                "customRecording" to hasCustomUrl,
                "published" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("mantra_audio").document(mantraId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
