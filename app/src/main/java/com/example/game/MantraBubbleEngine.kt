package com.example.game

import com.example.data.model.MantraItem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class MantraBubble(
    val id: Long,
    val mantra: MantraItem,
    var x: Float,
    var y: Float,
    val radius: Float,
    val baseSpeedPxPerSec: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val phaseOffset: Float,
    var rotationDeg: Float,
    val rotationSpeedDeg: Float,
    var ageSec: Float = 0f
)

enum class ParticleKind {
    PETAL,
    SPARK,
    GLOW_RING
}

data class BurstParticle(
    val kind: ParticleKind,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var size: Float,
    val colorHex: Long,
    var alpha: Float = 1f,
    var lifeSec: Float = 0f,
    val maxLifeSec: Float = 0.85f,
    var rotationDeg: Float = 0f,
    val rotationVel: Float = 90f
)

data class FloatingScorePopup(
    val text: String,
    val mantraHindi: String,
    var x: Float,
    var y: Float,
    var lifeSec: Float = 0f,
    val maxLifeSec: Float = 1.15f
)

data class AmbientDiyaParticle(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speedY: Float,
    val phase: Float,
    val alpha: Float
)

data class BubbleGameHudState(
    val punyaScore: Long = 0L,
    val totalChants: Long = 0L,
    val sadhanaLevel: Int = 1,
    val bestScore: Long = 0L,
    val levelProgress: Float = 0f,
    val chantsToNextLevel: Int = 10,
    val isPaused: Boolean = false,
    val slowMotionActive: Boolean = false,
    val slowMotionRemainingSec: Float = 0f,
    val slowMotionCharge: Float = 1f, // 0..1, charges up with every popped bubble
    val lastPoppedMantra: MantraItem? = null
)

/**
 * High-performance Mantra Bubble Game physics & particle engine.
 * Mutates internal pools per animation frame without triggering Compose layout churn,
 * respects reduced-motion accessibility settings, and guarantees balanced readable speed.
 */
class MantraBubbleEngine(
    private val reducedMotion: Boolean = false,
    private val random: Random = Random.Default
) {
    val bubbles = mutableListOf<MantraBubble>()
    val particles = mutableListOf<BurstParticle>()
    val popups = mutableListOf<FloatingScorePopup>()
    val ambientParticles = mutableListOf<AmbientDiyaParticle>()

    private var nextBubbleId = 1L
    private var spawnTimerSec = 0f
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var mantraDeckIndex = 0
    private var shuffledDeck = MantraItem.entries.shuffled(random)

    var hudState = BubbleGameHudState()
        private set

    fun hydrateSavedStats(bestScore: Long, totalChants: Long, punyaScore: Long, sadhanaLevel: Int) {
        val safeLevel = sadhanaLevel.coerceAtLeast(1)
        hudState = hudState.copy(
            bestScore = maxOf(hudState.bestScore, bestScore),
            totalChants = maxOf(hudState.totalChants, totalChants),
            punyaScore = maxOf(hudState.punyaScore, punyaScore),
            sadhanaLevel = maxOf(hudState.sadhanaLevel, safeLevel),
            levelProgress = calculateLevelProgress(maxOf(hudState.totalChants, totalChants))
        )
    }

    fun onViewportSized(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val firstInit = canvasWidth == 0f || canvasHeight == 0f
        canvasWidth = width
        canvasHeight = height
        if (firstInit) {
            seedAmbientParticles()
            // Spawn 5 well-spaced starter bubbles immediately so screen feels alive on first paint
            repeat(5) { idx ->
                spawnBubble(initialYFraction = 0.25f + idx * 0.16f)
            }
        }
    }

    private fun seedAmbientParticles() {
        ambientParticles.clear()
        val count = if (reducedMotion) 10 else 28
        repeat(count) {
            ambientParticles.add(
                AmbientDiyaParticle(
                    x = random.nextFloat() * canvasWidth,
                    y = random.nextFloat() * canvasHeight,
                    radius = 2f + random.nextFloat() * 4.5f,
                    speedY = 12f + random.nextFloat() * 24f,
                    phase = random.nextFloat() * (2f * PI.toFloat()),
                    alpha = 0.18f + random.nextFloat() * 0.42f
                )
            )
        }
    }

    fun step(dtSecRaw: Float): Boolean {
        if (canvasWidth <= 0f || canvasHeight <= 0f) return false
        val dt = dtSecRaw.coerceIn(0.001f, 0.05f)
        if (hudState.isPaused) return false

        // Update slow-motion timer
        var slowActive = hudState.slowMotionActive
        var slowRemaining = hudState.slowMotionRemainingSec
        if (slowActive) {
            slowRemaining -= dt
            if (slowRemaining <= 0f) {
                slowActive = false
                slowRemaining = 0f
            }
            hudState = hudState.copy(
                slowMotionActive = slowActive,
                slowMotionRemainingSec = slowRemaining
            )
        }

        val speedMultiplier = when {
            slowActive -> 0.42f
            reducedMotion -> 0.65f
            else -> 1f
        }

        // Update ambient temple dust / floating light particles
        for (p in ambientParticles) {
            p.y -= p.speedY * dt * (if (reducedMotion) 0.5f else 1f)
            if (p.y < -12f) {
                p.y = canvasHeight + 12f
                p.x = random.nextFloat() * canvasWidth
            }
        }

        // Spawn new mantra bubbles continuously while keeping density readable
        val maxBubbles = if (canvasWidth > 900f) 9 else 7
        val spawnInterval = if (slowActive) 1.65f else 1.12f
        spawnTimerSec += dt
        if (spawnTimerSec >= spawnInterval && bubbles.size < maxBubbles) {
            spawnTimerSec = 0f
            spawnBubble(initialYFraction = null)
        }

        // Move bubbles from bottom to top with gentle sway & rotation
        val bubbleIter = bubbles.iterator()
        while (bubbleIter.hasNext()) {
            val b = bubbleIter.next()
            b.ageSec += dt
            b.y -= b.baseSpeedPxPerSec * dt * speedMultiplier
            if (!reducedMotion) {
                b.x += cos((b.ageSec * b.swayFrequency + b.phaseOffset).toDouble()).toFloat() *
                    b.swayAmplitude * dt * speedMultiplier
                b.x = b.x.coerceIn(b.radius + 14f, (canvasWidth - b.radius - 14f).coerceAtLeast(b.radius + 15f))
                b.rotationDeg += b.rotationSpeedDeg * dt * speedMultiplier
            }
            if (b.y + b.radius < -24f) {
                bubbleIter.remove()
            }
        }

        // Update burst particles (petals, sparks, divine glow rings)
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.lifeSec += dt
            if (p.lifeSec >= p.maxLifeSec) {
                pIter.remove()
                continue
            }
            val progress = p.lifeSec / p.maxLifeSec
            if (p.kind == ParticleKind.GLOW_RING) {
                p.size += 260f * dt
                p.alpha = (1f - progress).coerceIn(0f, 1f)
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt + (if (p.kind == ParticleKind.PETAL) 70f * dt * progress else 0f)
                p.rotationDeg += p.rotationVel * dt
                p.alpha = (1f - progress * progress).coerceIn(0f, 1f)
            }
        }

        // Update floating score popups
        val popIter = popups.iterator()
        while (popIter.hasNext()) {
            val pop = popIter.next()
            pop.lifeSec += dt
            pop.y -= 72f * dt
            if (pop.lifeSec >= pop.maxLifeSec) {
                popIter.remove()
            }
        }

        return true
    }

    private fun nextMantraFromDeck(): MantraItem {
        if (mantraDeckIndex >= shuffledDeck.size) {
            shuffledDeck = MantraItem.entries.shuffled(random)
            mantraDeckIndex = 0
        }
        return shuffledDeck[mantraDeckIndex++]
    }

    private fun spawnBubble(initialYFraction: Float?) {
        val mantra = nextMantraFromDeck()
        val baseRadius = (minOf(canvasWidth, canvasHeight) * 0.145f).coerceIn(112f, 175f)
        val radius = baseRadius * (0.92f + random.nextFloat() * 0.16f)
        val margin = radius + 22f
        val xSpan = (canvasWidth - margin * 2f).coerceAtLeast(10f)

        // Pick X avoiding overlap with recently spawned bubbles
        var chosenX = margin + random.nextFloat() * xSpan
        repeat(5) {
            val candidate = margin + random.nextFloat() * xSpan
            val tooClose = bubbles.any { existing ->
                hypot((existing.x - candidate).toDouble(), (existing.y - (canvasHeight + radius)).toDouble()) < radius * 1.9
            }
            if (!tooClose) {
                chosenX = candidate
                return@repeat
            }
        }

        val yPos = if (initialYFraction != null) {
            canvasHeight * initialYFraction
        } else {
            canvasHeight + radius + 16f
        }

        // Varied speeds: some bubbles float serenely, a few move slightly faster while remaining readable
        val speedFactor = if (random.nextFloat() < 0.28f) {
            1.28f + random.nextFloat() * 0.22f
        } else {
            0.82f + random.nextFloat() * 0.26f
        }
        val levelSpeedBoost = 1f + ((hudState.sadhanaLevel - 1).coerceAtMost(10) * 0.03f)
        val baseSpeed = (canvasHeight * 0.125f).coerceIn(115f, 220f) * speedFactor * levelSpeedBoost

        bubbles.add(
            MantraBubble(
                id = nextBubbleId++,
                mantra = mantra,
                x = chosenX,
                y = yPos,
                radius = radius,
                baseSpeedPxPerSec = baseSpeed,
                swayAmplitude = 24f + random.nextFloat() * 34f,
                swayFrequency = 1.1f + random.nextFloat() * 0.9f,
                phaseOffset = random.nextFloat() * (2f * PI.toFloat()),
                rotationDeg = (random.nextFloat() - 0.5f) * 8f,
                rotationSpeedDeg = (random.nextFloat() - 0.5f) * 14f
            )
        )
    }

    /**
     * Checks if tap coordinates hit any bubble (generous 1.14x touch hit radius for effortless mobile play).
     * Returns the popped MantraItem if a bubble was popped, or null if tap missed.
     */
    fun handleTap(tapX: Float, tapY: Float): MantraItem? {
        if (hudState.isPaused) return null
        // Check from topmost (newest) to oldest
        for (i in bubbles.indices.reversed()) {
            val b = bubbles[i]
            val dist = hypot((tapX - b.x).toDouble(), (tapY - b.y).toDouble()).toFloat()
            if (dist <= b.radius * 1.14f) {
                bubbles.removeAt(i)
                spawnDivineBurst(b)
                val earnedPoints = b.mantra.punyaPoints.toLong()
                val newScore = hudState.punyaScore + earnedPoints
                val newChants = hudState.totalChants + 1L
                val newBest = maxOf(hudState.bestScore, newScore)
                val newLevel = (newChants / 10L).toInt() + 1
                val newCharge = (hudState.slowMotionCharge + 0.18f).coerceAtMost(1f)
                hudState = hudState.copy(
                    punyaScore = newScore,
                    totalChants = newChants,
                    sadhanaLevel = newLevel,
                    bestScore = newBest,
                    levelProgress = calculateLevelProgress(newChants),
                    chantsToNextLevel = 10 - (newChants % 10L).toInt(),
                    slowMotionCharge = newCharge,
                    lastPoppedMantra = b.mantra
                )
                return b.mantra
            }
        }
        return null
    }

    private fun spawnDivineBurst(bubble: MantraBubble) {
        // Expanding golden light aura ring
        particles.add(
            BurstParticle(
                kind = ParticleKind.GLOW_RING,
                x = bubble.x,
                y = bubble.y,
                vx = 0f,
                vy = 0f,
                size = bubble.radius * 0.7f,
                colorHex = 0xFFFFD54F,
                maxLifeSec = 0.52f
            )
        )

        val petalCount = if (reducedMotion) 4 else 14
        val sparkCount = if (reducedMotion) 4 else 16
        val petalPalette = listOf(0xFFFF9800L, 0xFFFFC107L, 0xFFE91E63L, 0xFFFFEB3BL, 0xFFFF5722L)

        repeat(petalCount) { idx ->
            val angle = (2.0 * PI * idx / petalCount) + (random.nextDouble() - 0.5) * 0.35
            val speed = 130f + random.nextFloat() * 190f
            particles.add(
                BurstParticle(
                    kind = ParticleKind.PETAL,
                    x = bubble.x,
                    y = bubble.y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 40.0).toFloat(),
                    size = 13f + random.nextFloat() * 10f,
                    colorHex = petalPalette[idx % petalPalette.size],
                    maxLifeSec = 0.78f + random.nextFloat() * 0.32f,
                    rotationDeg = random.nextFloat() * 360f,
                    rotationVel = (random.nextFloat() - 0.5f) * 320f
                )
            )
        }

        repeat(sparkCount) {
            val angle = random.nextDouble() * 2.0 * PI
            val speed = 160f + random.nextFloat() * 260f
            particles.add(
                BurstParticle(
                    kind = ParticleKind.SPARK,
                    x = bubble.x,
                    y = bubble.y,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed).toFloat(),
                    size = 4f + random.nextFloat() * 6f,
                    colorHex = if (random.nextBoolean()) 0xFFFFF8E1L else 0xFFFFD54FL,
                    maxLifeSec = 0.45f + random.nextFloat() * 0.35f
                )
            )
        }

        popups.add(
            FloatingScorePopup(
                text = "+${bubble.mantra.punyaPoints} पुण्य",
                mantraHindi = bubble.mantra.mantraHindi,
                x = bubble.x,
                y = bubble.y - bubble.radius * 0.2f
            )
        )
    }

    fun togglePause() {
        hudState = hudState.copy(isPaused = !hudState.isPaused)
    }

    fun pause() {
        hudState = hudState.copy(isPaused = true)
    }

    fun resume() {
        hudState = hudState.copy(isPaused = false)
    }

    fun activateSlowMotion(): Boolean {
        if (hudState.slowMotionActive || hudState.slowMotionCharge < 0.35f) return false
        hudState = hudState.copy(
            slowMotionActive = true,
            slowMotionRemainingSec = 7.0f,
            slowMotionCharge = 0f
        )
        return true
    }

    fun restartSession() {
        bubbles.clear()
        particles.clear()
        popups.clear()
        spawnTimerSec = 0f
        hudState = hudState.copy(
            punyaScore = 0L,
            totalChants = 0L,
            sadhanaLevel = 1,
            levelProgress = 0f,
            chantsToNextLevel = 10,
            isPaused = false,
            slowMotionActive = false,
            slowMotionRemainingSec = 0f,
            slowMotionCharge = 1f,
            lastPoppedMantra = null
        )
        if (canvasWidth > 0f && canvasHeight > 0f) {
            repeat(5) { idx ->
                spawnBubble(initialYFraction = 0.25f + idx * 0.16f)
            }
        }
    }

    private fun calculateLevelProgress(chants: Long): Float = ((chants % 10L).toFloat() / 10f).coerceIn(0f, 1f)
}
