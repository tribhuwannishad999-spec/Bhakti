package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.audio.MantraAudioManager
import com.example.data.integrations.WhatsAppIntegrationManager
import com.example.data.model.UiState
import com.example.game.MantraBubbleGameView
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LotusRose
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SpiritualPurple
import com.example.ui.theme.TempleCream
import com.example.viewmodel.BhaktiViewModel
import kotlinx.coroutines.launch

/**
 * Homepage:
 * 1) Complete FIRST viewport = 100% FULL-SCREEN MANTRA BUBBLE GAME ONLY
 *    (NO header, NO navigation menu, NO donation card, NO astrology buttons visible in first viewport).
 * 2) Below the game, displays the exact 9 requested sections in exact order:
 *    1. धनपात्र
 *    2. आज का राशिफल
 *    3. हस्तरेखा
 *    4. भक्ति कथाएँ
 *    5. आज का पंचांग
 *    6. व्रत और उपवास
 *    7. त्योहार और पर्व
 *    8. मंदिर दर्शन
 *    9. भजन और भक्ति संगीत
 */
@Composable
fun HomeScreen(
    viewModel: BhaktiViewModel,
    audioManager: MantraAudioManager,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit
) {
    val profileState by viewModel.userProfileState.collectAsState()
    val language by viewModel.language.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val profile = (profileState as? UiState.Success)?.data
    val isHindi = language == "hi"

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val viewportHeightDp = maxHeight
        val viewportHeightPx = with(LocalDensity.current) { viewportHeightDp.toPx().toInt() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // ==========================================================
            // VIEWPORT 1: FULL-SCREEN MANTRA BUBBLE GAME ONLY
            // ==========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(viewportHeightDp)
            ) {
                MantraBubbleGameView(
                    audioManager = audioManager,
                    savedBestScore = profile?.bestScore ?: 0L,
                    savedTotalChants = profile?.totalChants ?: 0L,
                    savedPunyaScore = profile?.punyaScore ?: 0L,
                    savedSadhanaLevel = (profile?.sadhanaLevel ?: 1L).toInt(),
                    onPersistStats = { punya, chants, level, best ->
                        viewModel.syncBubbleGameStats(punya, chants, level, best)
                    },
                    onExploreClick = {
                        scope.launch {
                            scrollState.animateScrollTo(viewportHeightPx)
                        }
                    }
                )
            }

            // ==========================================================
            // BELOW THE GAME: BHAKTI DARSHAN BANNER + EXACT 9 ORDERED SECTIONS
            // ==========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Devotional Sanctuary Header (only appears BELOW the full-screen game)
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, SacredGold, RoundedCornerShape(24.dp))
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_temple_hero),
                            contentDescription = "Bhakti Darshan Temple Sanctum",
                            contentScale = ContentScale.Crop,
                            alpha = 0.38f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(156.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHindi) "भक्ति दर्शन" else "BHAKTI DARSHAN",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = LuminousGold
                                )
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0x88000000),
                                    modifier = Modifier
                                        .clickable { viewModel.toggleLanguage() }
                                        .testTag("language_toggle_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Language,
                                            contentDescription = "भाषा बदलें",
                                            tint = LuminousGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isHindi) "EN" else "हिन्दी",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (isHindi) {
                                    "प्रणाम ${profile?.displayName ?: "भक्त"} जी • सनातन साधना, दर्शन एवं आध्यात्मिक सेवा"
                                } else {
                                    "Pranam ${profile?.displayName ?: "Devotee"} • Sacred Hindu Devotional Platform"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TempleCream
                            )
                        }
                    }
                }

                Text(
                    text = if (isHindi) "पावन अनुभाग" else "Sacred Sections",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary
                )

                // EXACT ORDER REQUIRED BY SPECIFICATION (1 to 9):
                val orderedSections = listOf(
                    SectionCardSpec(
                        index = 1,
                        titleHi = "धनपात्र",
                        titleEn = "Dhanpatra Donations",
                        subtitleHi = "मंदिर सेवा, गौ सेवा एवं धार्मिक कार्यों में सुरक्षित सहयोग",
                        subtitleEn = "Verified UPI & Gateway devotional support with receipts",
                        icon = Icons.Filled.VolunteerActivism,
                        accent = RoyalSaffron,
                        route = Routes.DHANPATRA,
                        testTag = "section_1_dhanpatra"
                    ),
                    SectionCardSpec(
                        index = 2,
                        titleHi = "आज का राशिफल",
                        titleEn = "Today's Rashifal & Kundli",
                        subtitleHi = "12 राशियाँ, दैनिक/साप्ताहिक/मासिक राशिफल, कुंडली, मुहूर्त और चौघड़िया",
                        subtitleEn = "12 Zodiac signs, Kundli, Lagna, Navamsha, Muhurat & Numerology",
                        icon = Icons.Filled.Stars,
                        accent = SpiritualPurple,
                        route = Routes.RASHIFAL,
                        testTag = "section_2_rashifal"
                    ),
                    SectionCardSpec(
                        index = 3,
                        titleHi = "हस्तरेखा",
                        titleEn = "AI Palmistry (Hastarekha)",
                        subtitleHi = "कैमरा या फोटो से जीवन, मस्तिष्क, हृदय, भाग्य व सूर्य रेखा विश्लेषण",
                        subtitleEn = "Camera/photo capture with Gemini Vision palm line reading",
                        icon = Icons.Filled.PanTool,
                        accent = DeepMaroon,
                        route = Routes.PALMISTRY,
                        testTag = "section_3_palmistry"
                    ),
                    SectionCardSpec(
                        index = 4,
                        titleHi = "भक्ति कथाएँ",
                        titleEn = "Bhakti Katha Videos",
                        subtitleHi = "शिव, श्रीराम, श्रीकृष्ण, हनुमान, माता रानी व विष्णु कथा संग्रह",
                        subtitleEn = "Devotional video discourses with search, filters & sharing",
                        icon = Icons.Filled.PlayCircle,
                        accent = Color(0xFFC62828),
                        route = Routes.KATHA,
                        testTag = "section_4_katha"
                    ),
                    SectionCardSpec(
                        index = 5,
                        titleHi = "आज का पंचांग",
                        titleEn = "Today's Panchang",
                        subtitleHi = "तिथि, पक्ष, नक्षत्र, योग, करण, राहुकाल और अभिजीत मुहूर्त",
                        subtitleEn = "Tithi, Nakshatra, Yoga, Rahu Kaal & daily 4 AM IST sync",
                        icon = Icons.Filled.CalendarMonth,
                        accent = Color(0xFFBF360C),
                        route = Routes.PANCHANG,
                        testTag = "section_5_panchang"
                    ),
                    SectionCardSpec(
                        index = 6,
                        titleHi = "व्रत और उपवास",
                        titleEn = "Vrat & Upvas",
                        subtitleHi = "एकादशी, प्रदोष, शिवरात्रि, चतुर्थी, पूर्णिमा और अमावस्या विधि",
                        subtitleEn = "Sacred fasts, observances, puja vidhi & mantras",
                        icon = Icons.Filled.SelfImprovement,
                        accent = Color(0xFF00695C),
                        route = Routes.VRAT,
                        testTag = "section_6_vrat"
                    ),
                    SectionCardSpec(
                        index = 7,
                        titleHi = "त्योहार और पर्व",
                        titleEn = "Festivals & Parv",
                        subtitleHi = "प्रमुख हिंदू पर्व, तिथि, महत्व, पूजा विधि, मंत्र और कथा",
                        subtitleEn = "Festival dates, significance, Puja Vidhi, mantras & katha",
                        icon = Icons.Filled.Celebration,
                        accent = LotusRose,
                        route = Routes.FESTIVALS,
                        testTag = "section_7_festivals"
                    ),
                    SectionCardSpec(
                        index = 8,
                        titleHi = "मंदिर दर्शन",
                        titleEn = "Temple Darshan",
                        subtitleHi = "प्रमुख तीर्थ स्थल, इतिहास, दर्शन समय और पावन झलकियाँ",
                        subtitleEn = "Sacred temples, history, timings, images & darshan info",
                        icon = Icons.Filled.AccountBalance,
                        accent = Color(0xFF8D4E00),
                        route = Routes.TEMPLES,
                        testTag = "section_8_temples"
                    ),
                    SectionCardSpec(
                        index = 9,
                        titleHi = "भजन और भक्ति संगीत",
                        titleEn = "Bhajan & Bhakti Music",
                        subtitleHi = "शिव, कृष्ण, राम, हनुमान, माता रानी, आरती, चालीसा व मंत्र प्लेयर",
                        subtitleEn = "Devotional music player with playlist, volume & favorites",
                        icon = Icons.Filled.MusicNote,
                        accent = Color(0xFF4A148C),
                        route = Routes.BHAJAN,
                        testTag = "section_9_bhajan"
                    )
                )

                orderedSections.forEach { spec ->
                    DevotionalSectionCard(
                        spec = spec,
                        isHindi = isHindi,
                        onClick = { onNavigate(spec.route) }
                    )
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isHindi) "अतिरिक्त दिव्य सेवाएँ" else "Additional Services",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary
                )

                // AI Devotional Assistant + Spiritual News + WhatsApp Contact + Owner Dashboard
                DevotionalSectionCard(
                    spec = SectionCardSpec(
                        index = 10,
                        titleHi = "AI भक्ति सहायक",
                        titleEn = "AI Devotional Assistant",
                        subtitleHi = "हिंदी व English में मंत्र अर्थ, पूजा विधि और आध्यात्मिक संवाद (Gemini AI)",
                        subtitleEn = "Streaming devotional Q&A powered by Firebase AI Logic",
                        icon = Icons.Filled.AutoAwesome,
                        accent = SpiritualPurple,
                        route = Routes.AI_ASSISTANT,
                        testTag = "section_ai_assistant"
                    ),
                    isHindi = isHindi,
                    onClick = { onNavigate(Routes.AI_ASSISTANT) }
                )

                DevotionalSectionCard(
                    spec = SectionCardSpec(
                        index = 11,
                        titleHi = "आध्यात्मिक समाचार",
                        titleEn = "Spiritual & Temple News",
                        subtitleHi = "मंदिर समाचार, पर्व अपडेट व धार्मिक लेख (NewsAPI / MediaStack)",
                        subtitleEn = "Verified temple & festival updates from original sources",
                        icon = Icons.Filled.Newspaper,
                        accent = Color(0xFF2E4053),
                        route = Routes.NEWS,
                        testTag = "section_spiritual_news"
                    ),
                    isHindi = isHindi,
                    onClick = { onNavigate(Routes.NEWS) }
                )

                // WhatsApp Share & Contact Row
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) "WhatsApp भक्ति संपर्क व शेयर" else "WhatsApp Contact & Share",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isHindi) "परिवार संग भक्ति दर्शन साझा करें" else "Share Bhakti Darshan with family & devotees",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1B5E20),
                            modifier = Modifier
                                .clickable {
                                    WhatsAppIntegrationManager.shareDevotionalMessage(
                                        context,
                                        "🙏 जय श्रीराम! Bhakti Darshan ऐप पर मंत्र बबल साधना, पंचांग, भक्ति कथाएँ और भजन का दिव्य अनुभव लें।"
                                    )
                                }
                                .testTag("whatsapp_share_home_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isHindi) "शेयर" else "Share", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Owner Dashboard Entry (Visibly shown to verified owner, or shows info/status for owner management)
                if (viewModel.isOwner) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, LuminousGold, RoundedCornerShape(20.dp))
                            .clickable { onNavigate(Routes.OWNER_DASHBOARD) }
                            .testTag("owner_dashboard_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(LuminousGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Owner Dashboard", tint = DeepMaroon)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHindi) "ओनर डैशबोर्ड (प्रबंधन)" else "Owner Dashboard",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LuminousGold
                                )
                                Text(
                                    text = if (isHindi) "कथा, मंदिर, भजन, मंत्र ऑडियो और UPI दान सत्यापन" else "Manage Kathas, Temples, Bhajans, Mantra Audio & Verify UPI",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TempleCream
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LuminousGold)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isHindi) "साइन आउट करें (Sign Out)" else "Sign Out")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private data class SectionCardSpec(
    val index: Int,
    val titleHi: String,
    val titleEn: String,
    val subtitleHi: String,
    val subtitleEn: String,
    val icon: ImageVector,
    val accent: Color,
    val route: String,
    val testTag: String
)

@Composable
private fun DevotionalSectionCard(
    spec: SectionCardSpec,
    isHindi: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SacredGold.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(spec.testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(spec.accent, spec.accent.copy(alpha = 0.78f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = spec.icon,
                    contentDescription = if (isHindi) spec.titleHi else spec.titleEn,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) spec.titleHi else spec.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isHindi) spec.subtitleHi else spec.subtitleEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = SacredGold
            )
        }
    }
}

object Routes {
    const val HOME = "home"
    const val DHANPATRA = "dhanpatra"
    const val RASHIFAL = "rashifal"
    const val PALMISTRY = "palmistry"
    const val KATHA = "katha"
    const val PANCHANG = "panchang"
    const val VRAT = "vrat"
    const val FESTIVALS = "festivals"
    const val TEMPLES = "temples"
    const val BHAJAN = "bhajan"
    const val AI_ASSISTANT = "ai_assistant"
    const val NEWS = "news"
    const val OWNER_DASHBOARD = "owner_dashboard"
}
