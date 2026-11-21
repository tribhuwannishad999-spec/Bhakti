package com.example.game

import android.graphics.Paint
import android.graphics.Typeface
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.audio.AudioPlaybackState
import com.example.data.audio.MantraAudioManager
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SanctumMaroonDark
import com.example.ui.theme.SanctumNight
import com.example.ui.theme.SanctumPurpleGlow
import com.example.ui.theme.TempleCream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * FULL-SCREEN MANTRA BUBBLE GAME (Homepage First Viewport).
 * Occupies 100% of the mobile viewport with NO website header, NO navigation menu,
 * NO donation banners, and NO astrology cards — purely the immersive devotional bubble game.
 */
@Composable
fun MantraBubbleGameView(
    audioManager: MantraAudioManager,
    savedBestScore: Long,
    savedTotalChants: Long,
    savedPunyaScore: Long,
    savedSadhanaLevel: Int,
    onPersistStats: (punya: Long, chants: Long, level: Int, best: Long) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) {
            false
        }
    }

    val engine = remember(reducedMotion) { MantraBubbleEngine(reducedMotion = reducedMotion) }
    var frameTick by remember { mutableLongStateOf(0L) }
    var hudState by remember { mutableStateOf(engine.hudState) }
    var chantsSinceLastSync by remember { mutableIntStateOf(0) }

    val soundEnabled by audioManager.soundEnabled.collectAsState()
    val audioPlaybackState by audioManager.audioState.collectAsState()

    val devanagariTypeface = remember(context) {
        try {
            ResourcesCompat.getFont(context, R.font.noto_serif_devanagari) ?: Typeface.DEFAULT_BOLD
        } catch (_: Exception) {
            Typeface.DEFAULT_BOLD
        }
    }

    val mantraPaint = remember(devanagariTypeface) {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(devanagariTypeface, Typeface.BOLD)
            setShadowLayer(8f, 0f, 3f, android.graphics.Color.argb(190, 40, 8, 12))
        }
    }

    val popupPaint = remember(devanagariTypeface) {
        Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.rgb(255, 224, 112)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(devanagariTypeface, Typeface.BOLD)
            setShadowLayer(10f, 0f, 2f, android.graphics.Color.argb(220, 74, 14, 23))
        }
    }

    LaunchedEffect(savedBestScore, savedTotalChants, savedPunyaScore, savedSadhanaLevel) {
        engine.hydrateSavedStats(
            bestScore = savedBestScore,
            totalChants = savedTotalChants,
            punyaScore = savedPunyaScore,
            sadhanaLevel = savedSadhanaLevel
        )
        hudState = engine.hudState
    }

    // High-performance 60fps animation loop synced to display refresh via withFrameNanos
    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val dt = (nowNanos - lastNanos) / 1_000_000_000f
                    if (engine.step(dt)) {
                        frameTick = nowNanos
                        if (hudState.slowMotionActive != engine.hudState.slowMotionActive ||
                            (hudState.slowMotionActive &&
                                kotlin.math.abs(hudState.slowMotionRemainingSec - engine.hudState.slowMotionRemainingSec) > 0.2f)
                        ) {
                            hudState = engine.hudState
                        }
                    }
                }
                lastNanos = nowNanos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SanctumNight,
                        SanctumMaroonDark,
                        SanctumPurpleGlow,
                        Color(0xFF47121E),
                        Color(0xFF5D1D14)
                    )
                )
            )
            .onSizeChanged { size ->
                engine.onViewportSized(size.width.toFloat(), size.height.toFloat())
            }
            .testTag("mantra_bubble_game_root")
    ) {
        // 1. Full-Screen High-Performance Devotional Bubble Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(engine) {
                    detectTapGestures { offset ->
                        val poppedMantra = engine.handleTap(offset.x, offset.y)
                        if (poppedMantra != null) {
                            hudState = engine.hudState
                            frameTick = System.nanoTime()
                            // Exact 1-to-1 mantra voice match (stops previous immediately)
                            audioManager.playExactMantra(poppedMantra)
                            chantsSinceLastSync += 1
                            if (chantsSinceLastSync >= 5) {
                                chantsSinceLastSync = 0
                                onPersistStats(
                                    hudState.punyaScore,
                                    hudState.totalChants,
                                    hudState.sadhanaLevel,
                                    hudState.bestScore
                                )
                            }
                        }
                    }
                }
                .semantics {
                    contentDescription = "मंत्र बबल गेम कैनवास — मंत्र सुनने और पुण्य अर्जित करने के लिए बबल्स पर टैप करें"
                }
                .testTag("mantra_bubble_canvas")
        ) {
            // Read frameTick inside draw lambda to re-draw smoothly without recomposing parent layout
            val activeTick = frameTick
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h * 0.48f)

            // Subtle rotating sacred temple mandala aura in background
            val mandalaAngle = if (reducedMotion) 0f else ((activeTick / 1_000_000_000.0) * 4.5 % 360.0).toFloat()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x38F6C94C),
                        Color(0x16E65100),
                        Color.Transparent
                    ),
                    center = center,
                    radius = minOf(w, h) * 0.56f
                ),
                center = center,
                radius = minOf(w, h) * 0.56f
            )

            rotate(degrees = mandalaAngle, pivot = center) {
                val ringRadius = minOf(w, h) * 0.36f
                drawCircle(
                    color = Color(0x1FF6C94C),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color(0x18FF8F00),
                    radius = ringRadius * 0.72f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                for (i in 0 until 12) {
                    val rad = (i * 30.0) * PI / 180.0
                    val sx = center.x + (ringRadius * 0.72f * cos(rad)).toFloat()
                    val sy = center.y + (ringRadius * 0.72f * sin(rad)).toFloat()
                    val ex = center.x + (ringRadius * cos(rad)).toFloat()
                    val ey = center.y + (ringRadius * sin(rad)).toFloat()
                    drawLine(
                        color = Color(0x1DF6C94C),
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Ambient golden temple light particles
            for (ap in engine.ambientParticles) {
                drawCircle(
                    color = Color(0xFFFFE082).copy(alpha = ap.alpha),
                    radius = ap.radius,
                    center = Offset(ap.x, ap.y)
                )
            }

            // Slow-motion Divya Shakti aura border tint
            if (hudState.slowMotionActive) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x33F6C94C)),
                        center = center,
                        radius = maxOf(w, h) * 0.72f
                    )
                )
            }

            // Rising Luminous Mantra Bubbles
            for (bubble in engine.bubbles) {
                val bCenter = Offset(bubble.x, bubble.y)
                val primaryColor = Color(bubble.mantra.bubblePrimaryHex)
                val secondaryColor = Color(bubble.mantra.bubbleSecondaryHex)

                // Outer divine golden halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55FFD54F),
                            primaryColor.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = bCenter,
                        radius = bubble.radius * 1.28f
                    ),
                    radius = bubble.radius * 1.28f,
                    center = bCenter
                )

                // Main lustrous devotional orb gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE082).copy(alpha = 0.52f),
                            primaryColor.copy(alpha = 0.92f),
                            secondaryColor.copy(alpha = 0.95f)
                        ),
                        center = Offset(
                            bubble.x - bubble.radius * 0.26f,
                            bubble.y - bubble.radius * 0.28f
                        ),
                        radius = bubble.radius * 1.15f
                    ),
                    radius = bubble.radius,
                    center = bCenter
                )

                // Glowing golden outline ring
                drawCircle(
                    color = LuminousGold.copy(alpha = 0.92f),
                    radius = bubble.radius,
                    center = bCenter,
                    style = Stroke(width = 4.2f)
                )

                // Soft glass highlight crescent on top-left
                drawCircle(
                    color = Color.White.copy(alpha = 0.26f),
                    radius = bubble.radius * 0.24f,
                    center = Offset(
                        bubble.x - bubble.radius * 0.38f,
                        bubble.y - bubble.radius * 0.38f
                    )
                )

                // Centered Mantra Devanagari Text with gentle rotation
                rotate(degrees = bubble.rotationDeg, pivot = bCenter) {
                    drawIntoCanvas { canvas ->
                        val text = bubble.mantra.mantraHindi
                        val words = text.split(" ")
                        val lines = when {
                            text.length <= 12 -> listOf(text)
                            words.size >= 3 -> listOf(
                                words.take(2).joinToString(" "),
                                words.drop(2).joinToString(" ")
                            )
                            else -> listOf(text)
                        }
                        val baseSize = (bubble.radius * (if (lines.size > 1) 0.255f else 0.28f))
                            .coerceIn(28f, 44f)
                        mantraPaint.textSize = baseSize
                        val lineHeight = baseSize * 1.26f
                        val startY = bubble.y - ((lines.size - 1) * lineHeight / 2f) + (baseSize * 0.34f)
                        lines.forEachIndexed { idx, line ->
                            canvas.nativeCanvas.drawText(
                                line,
                                bubble.x,
                                startY + idx * lineHeight,
                                mantraPaint
                            )
                        }
                    }
                }
            }

            // Burst Particles (Flower Petals, Sparks, Expanding Divine Glow Ring)
            for (p in engine.particles) {
                val pColor = Color(p.colorHex).copy(alpha = p.alpha)
                when (p.kind) {
                    ParticleKind.GLOW_RING -> {
                        drawCircle(
                            color = pColor,
                            radius = p.size,
                            center = Offset(p.x, p.y),
                            style = Stroke(width = 7f * p.alpha)
                        )
                    }
                    ParticleKind.SPARK -> {
                        drawCircle(
                            color = pColor,
                            radius = p.size,
                            center = Offset(p.x, p.y)
                        )
                    }
                    ParticleKind.PETAL -> {
                        rotate(degrees = p.rotationDeg, pivot = Offset(p.x, p.y)) {
                            drawOval(
                                color = pColor,
                                topLeft = Offset(p.x - p.size * 0.55f, p.y - p.size * 0.95f),
                                size = androidx.compose.ui.geometry.Size(p.size * 1.1f, p.size * 1.9f)
                            )
                        }
                    }
                }
            }

            // Floating "+10 पुण्य" score popups
            for (pop in engine.popups) {
                val progress = (pop.lifeSec / pop.maxLifeSec).coerceIn(0f, 1f)
                val alphaInt = ((1f - progress) * 255).toInt().coerceIn(0, 255)
                drawIntoCanvas { canvas ->
                    popupPaint.alpha = alphaInt
                    popupPaint.textSize = 40f
                    canvas.nativeCanvas.drawText(pop.text, pop.x, pop.y, popupPaint)
                }
            }
        }

        // 2. Minimal Translucent Devotional Game HUD (Top safe area — NO website header or menu!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DevotionalStatChip(
                    label = "पुण्य अंक",
                    value = hudState.punyaScore.toString(),
                    modifier = Modifier.weight(1f).testTag("hud_punya_score")
                )
                DevotionalStatChip(
                    label = "साधना स्तर",
                    value = hudState.sadhanaLevel.toString(),
                    modifier = Modifier.weight(0.85f).testTag("hud_sadhana_level")
                )
                DevotionalStatChip(
                    label = "मंत्र स्मरण",
                    value = hudState.totalChants.toString(),
                    modifier = Modifier.weight(1f).testTag("hud_total_chants")
                )
                DevotionalStatChip(
                    label = "सर्वश्रेष्ठ",
                    value = hudState.bestScore.toString(),
                    modifier = Modifier.weight(0.95f).testTag("hud_best_score")
                )
            }

            // Sadhana Level Progress Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x5524060D))
                    .border(1.dp, Color(0x44F6C94C), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "साधना प्रगति • स्तर ${hudState.sadhanaLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LuminousGold
                    )
                    Text(
                        text = "अगले स्तर हेतु ${hudState.chantsToNextLevel} मंत्र",
                        style = MaterialTheme.typography.labelSmall,
                        color = TempleCream.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { hudState.levelProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = LuminousGold,
                    trackColor = Color(0x44FFFFFF)
                )
            }

            // Active Spoken Mantra Indicator (proves exact audio-to-bubble match visibly!)
            val activeAudioText = when (val st = audioPlaybackState) {
                is AudioPlaybackState.Playing -> "🔊 ${st.mantra.mantraHindi} (${st.sourceLabel})"
                is AudioPlaybackState.Loading -> "⏳ मंत्र ऑडियो लोड हो रहा है..."
                is AudioPlaybackState.Error -> "⚠ ${st.message}"
                AudioPlaybackState.Idle -> hudState.lastPoppedMantra?.let { "✨ ${it.mantraHindi} — ${it.deityHindi}" }
            }
            if (activeAudioText != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x884A0E17),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .border(1.dp, LuminousGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = activeAudioText,
                        style = MaterialTheme.typography.labelLarge,
                        color = LuminousGold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Pause Overlay
        if (hudState.isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xAA1A0409))
                    .clickable {
                        engine.resume()
                        hudState = engine.hudState
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeepMaroon,
                    modifier = Modifier
                        .padding(24.dp)
                        .border(1.5.dp, LuminousGold, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "साधना विराम (Paused)",
                            style = MaterialTheme.typography.headlineMedium,
                            color = LuminousGold
                        )
                        Text(
                            text = "पुण्य अंक: ${hudState.punyaScore} • कुल मंत्र स्मरण: ${hudState.totalChants}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TempleCream
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalSaffron,
                            modifier = Modifier
                                .clickable {
                                    engine.resume()
                                    hudState = engine.hudState
                                }
                                .testTag("resume_game_overlay_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "पुनः प्रारंभ", tint = Color.White)
                                Spacer(Modifier.width(6.dp))
                                Text("जारी रखें (Resume)", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Bottom Game Controls + Subtle Scroll Cue (NO nav menu, NO donation/astrology buttons!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x8824060D))
                    .border(1.dp, Color(0x55F6C94C), RoundedCornerShape(22.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sound ON/OFF
                GameControlIconButton(
                    icon = if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    label = if (soundEnabled) "ध्वनि ON" else "ध्वनि OFF",
                    highlighted = soundEnabled,
                    testTag = "game_sound_toggle",
                    onClick = { audioManager.toggleSound() }
                )

                // Slow-Motion Booster (दिव्य शक्ति / धीमा प्रवाह)
                val slowReady = hudState.slowMotionCharge >= 0.35f || hudState.slowMotionActive
                GameControlIconButton(
                    icon = Icons.Filled.SlowMotionVideo,
                    label = if (hudState.slowMotionActive) {
                        "धीमा ${hudState.slowMotionRemainingSec.toInt() + 1}s"
                    } else {
                        "दिव्य शक्ति"
                    },
                    highlighted = slowReady,
                    accentGold = hudState.slowMotionActive,
                    testTag = "game_slow_motion_button",
                    onClick = {
                        if (engine.activateSlowMotion()) {
                            hudState = engine.hudState
                        }
                    }
                )

                // Pause / Resume
                GameControlIconButton(
                    icon = if (hudState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    label = if (hudState.isPaused) "जारी" else "विराम",
                    highlighted = !hudState.isPaused,
                    testTag = "game_pause_toggle",
                    onClick = {
                        engine.togglePause()
                        hudState = engine.hudState
                        if (hudState.isPaused) {
                            onPersistStats(
                                hudState.punyaScore,
                                hudState.totalChants,
                                hudState.sadhanaLevel,
                                hudState.bestScore
                            )
                        }
                    }
                )

                // Restart
                GameControlIconButton(
                    icon = Icons.Filled.Refresh,
                    label = "पुनः शुरू",
                    highlighted = false,
                    testTag = "game_restart_button",
                    onClick = {
                        onPersistStats(
                            hudState.punyaScore,
                            hudState.totalChants,
                            hudState.sadhanaLevel,
                            hudState.bestScore
                        )
                        audioManager.stopAll()
                        engine.restartSession()
                        hudState = engine.hudState
                    }
                )
            }

            // Subtle scroll-down hint to explore the 9 devotional sections below the game
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0x664A0E17),
                modifier = Modifier
                    .clickable {
                        onPersistStats(
                            hudState.punyaScore,
                            hudState.totalChants,
                            hudState.sadhanaLevel,
                            hudState.bestScore
                        )
                        onExploreClick()
                    }
                    .testTag("scroll_to_sections_cue")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "नीचे स्क्रॉल करें • भक्ति दर्शन",
                        style = MaterialTheme.typography.labelMedium,
                        color = TempleCream.copy(alpha = 0.92f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "नीचे जाएँ",
                        tint = LuminousGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DevotionalStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x78360A13))
            .border(1.dp, Color(0x55F6C94C), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TempleCream.copy(alpha = 0.85f),
            maxLines = 1
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = LuminousGold,
            maxLines = 1
        )
    }
}

@Composable
private fun GameControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean,
    testTag: String,
    accentGold: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    when {
                        accentGold -> LuminousGold
                        highlighted -> RoyalSaffron
                        else -> Color(0x55FFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (accentGold) DeepMaroon else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TempleCream,
            maxLines = 1
        )
    }
}
