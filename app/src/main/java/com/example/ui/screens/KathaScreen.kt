package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.integrations.WhatsAppIntegrationManager
import com.example.data.model.KathaVideo
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LotusRose
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@Composable
fun KathaScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val kathasState by viewModel.kathasState.collectAsState()
    val favoritesState by viewModel.favoritesState.collectAsState()
    val isHindi = language == "hi"

    var selectedCategory by remember { mutableStateOf("सभी") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVideo by remember { mutableStateOf<KathaVideo?>(null) }

    val favoriteIds = ((favoritesState as? UiState.Success)?.data ?: emptyList())
        .filter { it.itemType == "katha" }
        .map { it.itemId }
        .toSet()

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "भक्ति कथाएँ" else "Bhakti Katha Videos",
                subtitle = if (isHindi) "अधिकृत धार्मिक कथा संग्रह (No Autoplay)" else "Authorized Devotional Videos",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_katha_banner),
                    contentDescription = "Bhakti Katha Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (isHindi) "कथा खोजें..." else "Search Katha...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("katha_search_input")
            )

            // 7 Required Categories
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DevotionalSeedCatalog.kathaCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            // Selected Video Detail & Manual Playback Launcher (NO Autoplay)
            if (selectedVideo != null) {
                val vid = selectedVideo!!
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, RoyalSaffron, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(vid.title, style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
                        Text("${vid.category} • अवधि: ${vid.duration}${vid.speaker?.let { " • $it" } ?: ""}", style = MaterialTheme.typography.labelSmall)
                        Text(vid.description, style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (vid.videoUrl.startsWith("https://") || vid.videoUrl.startsWith("http://")) {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(vid.videoUrl)))
                                        } catch (_: Exception) {
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron),
                                modifier = Modifier.weight(1f).testTag("play_katha_button")
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isHindi) "कथा चलाएँ (Play)" else "Play Katha")
                            }
                            IconButton(
                                onClick = {
                                    WhatsAppIntegrationManager.shareDevotionalMessage(
                                        context,
                                        "🙏 Bhakti Darshan पर यह पावन कथा देखें:\n${vid.title}\n${vid.videoUrl}"
                                    )
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "WhatsApp शेयर", tint = DeepMaroon)
                            }
                        }
                    }
                }
            }

            when (val st = kathasState) {
                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                is UiState.Error -> InfoNoticeBanner(title = "त्रुटि", message = st.message)
                is UiState.Success -> {
                    val filtered = st.data.filter { k ->
                        val catMatch = selectedCategory == "सभी" || k.category == selectedCategory
                        val qMatch = searchQuery.isBlank() ||
                            k.title.contains(searchQuery, ignoreCase = true) ||
                            k.description.contains(searchQuery, ignoreCase = true)
                        catMatch && qMatch
                    }
                    if (filtered.isEmpty()) {
                        Text(if (isHindi) "इस श्रेणी में अभी कोई कथा नहीं मिली।" else "No kathas found in this category.")
                    } else {
                        filtered.forEach { katha ->
                            val isFav = katha.id in favoriteIds
                            KathaListCard(
                                katha = katha,
                                isFavorite = isFav,
                                onSelect = { selectedVideo = katha },
                                onToggleFavorite = { viewModel.toggleFavorite(katha.id, "katha", katha.title, isFav) },
                                onShare = {
                                    WhatsAppIntegrationManager.shareDevotionalMessage(
                                        context,
                                        "🙏 ${katha.title} (${katha.category})\n${katha.videoUrl}"
                                    )
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun KathaListCard(
    katha: KathaVideo,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SacredGold.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .clickable(onClick = onSelect)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!katha.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = katha.thumbnailUrl,
                    contentDescription = katha.title,
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
                StatusPill(text = katha.category, bgColor = DeepMaroon)
                Text("⏱ ${katha.duration}", style = MaterialTheme.typography.labelSmall)
            }
            Text(katha.title, style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
            Text(katha.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "पसंदीदा",
                        tint = if (isFavorite) LotusRose else DeepMaroon
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, contentDescription = "शेयर", tint = DeepMaroon)
                }
            }
        }
    }
}
