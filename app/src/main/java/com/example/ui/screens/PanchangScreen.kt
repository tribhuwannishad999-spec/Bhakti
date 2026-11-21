package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredEmerald
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PanchangScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val panchangState by viewModel.panchangState.collectAsState()
    val isHindi = language == "hi"

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "आज का पंचांग" else "Today's Panchang",
                subtitle = "Navamsha API • Asia/Kolkata (4:00 AM IST)",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshPanchang(selectedCity) },
                        modifier = Modifier.testTag("panchang_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "रीफ्रेश", tint = LuminousGold)
                    }
                }
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
            Text(
                text = if (isHindi) "शहर चुनें (City Selection)" else "Select City",
                style = MaterialTheme.typography.titleMedium,
                color = DeepMaroon
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DevotionalSeedCatalog.panchangCities.forEach { city ->
                    FilterChip(
                        selected = selectedCity == city,
                        onClick = { viewModel.refreshPanchang(city) },
                        label = { Text(city) },
                        modifier = Modifier.testTag("city_chip_${city.lowercase().replace(" ", "_")}")
                    )
                }
            }

            when (val st = panchangState) {
                UiState.Loading, UiState.Idle -> {
                    BoxCenterLoader()
                }
                is UiState.Error -> {
                    InfoNoticeBanner(
                        title = "पंचांग त्रुटि",
                        message = st.message
                    )
                }
                is UiState.Success -> {
                    val data = st.data
                    InfoNoticeBanner(
                        title = if (data.isLiveApiData) {
                            "लाइव पंचांग सक्रिय (${data.city} • ${data.dateLabel})"
                        } else {
                            "Navamsha API क्रेडेंशियल प्रतीक्षारत (Stale / Inactive)"
                        },
                        message = "${data.statusMessage}\nअंतिम जाँच (IST): ${data.lastUpdatedIst} • दैनिक ऑटो-अपडेट लक्ष्य: प्रातः 04:00 AM IST (Asia/Kolkata)",
                        containerColor = if (data.isLiveApiData) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        borderColor = if (data.isLiveApiData) SacredEmerald else RoyalSaffron,
                        contentColor = if (data.isLiveApiData) SacredEmerald else DeepMaroon
                    )

                    val rows = listOf(
                        Pair("तिथि (Tithi)", data.tithi),
                        Pair("पक्ष (Paksha)", data.paksha),
                        Pair("नक्षत्र (Nakshatra)", data.nakshatra),
                        Pair("योग (Yoga)", data.yoga),
                        Pair("करण (Karana)", data.karana),
                        Pair("सूर्योदय (Sunrise)", data.sunrise),
                        Pair("सूर्यास्त (Sunset)", data.sunset),
                        Pair("चंद्रोदय (Moonrise)", data.moonrise),
                        Pair("चंद्रास्त (Moonset)", data.moonset),
                        Pair("राहु काल (Rahu Kaal)", data.rahuKaal),
                        Pair("गुलिक काल (Gulika Kaal)", data.gulikaKaal),
                        Pair("अभिजीत मुहूर्त (Abhijit Muhurat)", data.abhijitMuhurat)
                    )

                    rows.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { (label, value) ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeepMaroon,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
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
internal fun BoxCenterLoader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = RoyalSaffron)
    }
}
