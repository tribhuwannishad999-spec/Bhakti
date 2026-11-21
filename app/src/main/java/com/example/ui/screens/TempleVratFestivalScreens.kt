package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.integrations.WhatsAppIntegrationManager
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LotusRose
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@Composable
fun TempleDarshanScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val templesState by viewModel.templesState.collectAsState()
    val favoritesState by viewModel.favoritesState.collectAsState()
    val isHindi = language == "hi"

    val favIds = ((favoritesState as? UiState.Success)?.data ?: emptyList())
        .filter { it.itemType == "temple" }
        .map { it.itemId }
        .toSet()

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "मंदिर दर्शन" else "Temple Darshan",
                subtitle = if (isHindi) "पावन तीर्थ, इतिहास एवं दर्शन समय" else "Sacred Temples & Darshan Timings",
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
            Image(
                painter = painterResource(id = R.drawable.img_temple_hero),
                contentDescription = "Temple Hero",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            )

            when (val st = templesState) {
                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                is UiState.Error -> InfoNoticeBanner(title = "त्रुटि", message = st.message)
                is UiState.Success -> {
                    st.data.forEach { temple ->
                        val isFav = temple.id in favIds
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!temple.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = temple.imageUrl,
                                        contentDescription = temple.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(temple.name, style = MaterialTheme.typography.titleLarge, color = DeepMaroon, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.toggleFavorite(temple.id, "temple", temple.name, isFav) }) {
                                        Icon(
                                            if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "पसंदीदा",
                                            tint = if (isFav) LotusRose else DeepMaroon
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            WhatsAppIntegrationManager.shareDevotionalMessage(
                                                context,
                                                "🙏 मंदिर दर्शन: ${temple.name}\nस्थान: ${temple.location}\nदर्शन समय: ${temple.darshanTimings}"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "शेयर", tint = DeepMaroon)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = RoyalSaffron)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${temple.location} • ${temple.deity}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(temple.history, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "दर्शन जानकारी: ${temple.darshanTimings}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepMaroon,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!temple.videoUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(temple.videoUrl)))
                                            } catch (_: Exception) {
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("दर्शन वीडियो देखें")
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
fun VratUpvasScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val isHindi = language == "hi"

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "व्रत और उपवास" else "Vrat & Upvas",
                subtitle = if (isHindi) "एकादशी, प्रदोष, शिवरात्रि, चतुर्थी, पूर्णिमा व अमावस्या" else "Sacred Hindu Fasts & Observances",
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
            DevotionalSeedCatalog.referenceVrats.forEach { vrat ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isHindi) vrat.titleHi else "${vrat.titleEn} (${vrat.titleHi})",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepMaroon
                        )
                        StatusPill(text = vrat.frequencyHi, bgColor = RoyalSaffron)
                        Text("आराध्य देव: ${vrat.deityHi}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("महत्व: ${vrat.significanceHi}", style = MaterialTheme.typography.bodyMedium)
                        Text("व्रत विधि: ${vrat.vidhiHi}", style = MaterialTheme.typography.bodyMedium)
                        Text("पावन मंत्र: ${vrat.mantra}", style = MaterialTheme.typography.titleSmall, color = DeepMaroon)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FestivalsScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val festivalsState by viewModel.festivalsState.collectAsState()
    val isHindi = language == "hi"

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "त्योहार और पर्व" else "Festivals & Parv",
                subtitle = if (isHindi) "तिथि, महत्व, पूजा विधि, मंत्र एवं कथा" else "Dates, Puja Vidhi, Mantras & Katha",
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
            when (val st = festivalsState) {
                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                is UiState.Error -> InfoNoticeBanner(title = "त्रुटि", message = st.message)
                is UiState.Success -> {
                    st.data.forEach { fest ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(fest.name, style = MaterialTheme.typography.titleLarge, color = DeepMaroon, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            WhatsAppIntegrationManager.shareDevotionalMessage(
                                                context,
                                                "🪔 पावन पर्व: ${fest.name} (${fest.dateLabel})\nमंत्र: ${fest.mantra}\n— Bhakti Darshan"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = "शेयर", tint = DeepMaroon)
                                    }
                                }
                                StatusPill(text = fest.dateLabel, bgColor = DeepMaroon)
                                Text("महत्व: ${fest.significance}", style = MaterialTheme.typography.bodyMedium)
                                Text("पूजा विधि: ${fest.pujaVidhi}", style = MaterialTheme.typography.bodyMedium)
                                Text("मंत्र: ${fest.mantra}", style = MaterialTheme.typography.titleSmall, color = RoyalSaffron)
                                if (!fest.kathaSummary.isNullOrBlank()) {
                                    Text("पौराणिक कथा: ${fest.kathaSummary}", style = MaterialTheme.typography.bodySmall)
                                }
                                if (!fest.relatedVideoUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fest.relatedVideoUrl)))
                                            } catch (_: Exception) {
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("संबंधित कथा वीडियो")
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
