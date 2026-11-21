package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RashifalKundliScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val horoscopeState by viewModel.horoscopeState.collectAsState()
    val isHindi = language == "hi"

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedSign by remember { mutableStateOf(DevotionalSeedCatalog.zodiacSigns.first()) }
    var selectedPeriod by remember { mutableStateOf("daily") }

    // Birth details for Kundli
    var birthName by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var birthTime by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var kundliNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedSign, selectedPeriod) {
        viewModel.fetchHoroscope(selectedSign.nameEn, selectedPeriod)
    }

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "राशिफल एवं कुंडली" else "Rashifal & Kundli",
                subtitle = "AstroLogics API Integration",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(if (isHindi) "राशिफल" else "Rashifal") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(if (isHindi) "कुंडली व चार्ट" else "Kundli") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(if (isHindi) "मुहूर्त व अंक" else "Muhurat") })
            }

            when (selectedTab) {
                0 -> {
                    // All 12 Zodiac Signs
                    Text(
                        text = if (isHindi) "अपनी राशि चुनें (12 राशियाँ)" else "Select Zodiac Sign (12 Signs)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DevotionalSeedCatalog.zodiacSigns.forEach { sign ->
                            FilterChip(
                                selected = selectedSign.id == sign.id,
                                onClick = { selectedSign = sign },
                                label = { Text("${sign.symbol} ${if (isHindi) sign.nameHi else sign.nameEn}") },
                                modifier = Modifier.testTag("zodiac_${sign.id}")
                            )
                        }
                    }

                    // Daily / Weekly / Monthly
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("daily" to "दैनिक / Daily", "weekly" to "साप्ताहिक / Weekly", "monthly" to "मासिक / Monthly").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedPeriod == key,
                                onClick = { selectedPeriod = key },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Sign Traditional Metadata + Live API Result
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${selectedSign.symbol} ${selectedSign.nameHi} (${selectedSign.nameEn})",
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepMaroon
                            )
                            Text(
                                text = "तत्व: ${selectedSign.elementHi} • स्वामी ग्रह: ${selectedSign.rulingPlanetHi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            when (val st = horoscopeState) {
                                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                                is UiState.Success -> Text(st.data, style = MaterialTheme.typography.bodyLarge)
                                is UiState.Error -> InfoNoticeBanner(
                                    title = "AstroLogics API प्रतीक्षारत",
                                    message = "${st.message}\nहम कभी मनगढ़ंत राशिफल नहीं दिखाते। AI Studio Secrets में ASTROLOGICS_API_BASE_URL और ASTROLOGICS_API_KEY सेट करने पर वास्तविक राशिफल दिखने लगेगा।"
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Birth Details + Lagna & Navamsha Kundli Charts
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = if (isHindi) "जन्म विवरण (कुंडली, लग्न व नवमांश चार्ट)" else "Birth Details (Kundli, Lagna & Navamsha)",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepMaroon
                            )
                            OutlinedTextField(
                                value = birthName,
                                onValueChange = { birthName = it },
                                label = { Text("नाम / Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = birthDate,
                                onValueChange = { birthDate = it },
                                label = { Text("जन्म तिथि (DD/MM/YYYY)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = birthTime,
                                onValueChange = { birthTime = it },
                                label = { Text("जन्म समय (HH:MM AM/PM)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = birthPlace,
                                onValueChange = { birthPlace = it },
                                label = { Text("जन्म स्थान (शहर)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    kundliNotice = "AstroLogics API क्रेडेंशियल (ASTROLOGICS_API_BASE_URL व ASTROLOGICS_API_KEY) जुड़ते ही लग्न चार्ट, नवमांश चार्ट, ग्रह स्थिति व गोचर का सटीक डेटा प्राप्त होगा। हम नकली ग्रह गणना नहीं बनाते।"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron)
                            ) {
                                Text(if (isHindi) "कुंडली गणना करें" else "Generate Kundli", fontWeight = FontWeight.Bold)
                            }
                            if (!kundliNotice.isNullOrBlank()) {
                                InfoNoticeBanner(title = "कुंडली API स्थिति", message = kundliNotice!!)
                            }
                        }
                    }

                    // North Indian Diamond Kundli Chart Frame (Lagna & Navamsha visual container)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NorthIndianChartCard(title = "लग्न कुंडली (Lagna)", modifier = Modifier.weight(1f))
                        NorthIndianChartCard(title = "नवमांश (Navamsha)", modifier = Modifier.weight(1f))
                    }

                    InfoNoticeBanner(
                        title = "ग्रह स्थिति एवं गोचर (Planetary Positions & Transit)",
                        message = "सूर्य, चंद्र, मंगल, बुध, गुरु, शुक्र, शनि, राहु व केतु के अंश और गोचर AstroLogics API सक्रिय होने पर यहाँ स्वतः लोड होंगे।"
                    )
                }

                2 -> {
                    // Marriage Muhurat, Housewarming Muhurat, Choghadiya, Numerology
                    val items = listOf(
                        "विवाह मुहूर्त (Marriage Muhurat)" to "AstroLogics API के शुभ विवाह मुहूर्त एंडपॉइंट से लाइव तिथियाँ प्राप्त होंगी।",
                        "गृह प्रवेश मुहूर्त (Housewarming Muhurat)" to "नक्षत्र, वार व तिथि शुद्धता के साथ गृह प्रवेश मुहूर्त AstroLogics API से लोड होगा।",
                        "चौघड़िया (Choghadiya)" to "अमृत, शुभ, लाभ, चर, रोग, काल व उद्वेग चौघड़िया शहर व सूर्योदय के अनुसार।",
                        "अंक ज्योतिष (Numerology)" to "मूलांक, भाग्यांक और नामांक विश्लेषण हेतु AstroLogics API एडाप्टर तैयार है।"
                    )
                    items.forEach { (title, desc) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NorthIndianChartCard(title: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EE)),
        modifier = modifier.border(1.2.dp, SacredGold, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = DeepMaroon)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFFFFDF7))
            ) {
                val w = size.width
                val h = size.height
                val stroke = 2.5f
                val lineColor = Color(0xFF8D4E00)
                // Outer box
                drawLine(lineColor, Offset(0f, 0f), Offset(w, 0f), stroke)
                drawLine(lineColor, Offset(w, 0f), Offset(w, h), stroke)
                drawLine(lineColor, Offset(w, h), Offset(0f, h), stroke)
                drawLine(lineColor, Offset(0f, h), Offset(0f, 0f), stroke)
                // Diagonals
                drawLine(lineColor, Offset(0f, 0f), Offset(w, h), stroke)
                drawLine(lineColor, Offset(w, 0f), Offset(0f, h), stroke)
                // Inner diamond
                drawLine(lineColor, Offset(w / 2f, 0f), Offset(w, h / 2f), stroke)
                drawLine(lineColor, Offset(w, h / 2f), Offset(w / 2f, h), stroke)
                drawLine(lineColor, Offset(w / 2f, h), Offset(0f, h / 2f), stroke)
                drawLine(lineColor, Offset(0f, h / 2f), Offset(w / 2f, 0f), stroke)
            }
        }
    }
}
