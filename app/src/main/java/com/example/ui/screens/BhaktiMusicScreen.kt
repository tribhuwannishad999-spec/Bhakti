package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.audio.BhajanPlayerManager
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LotusRose
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.ui.theme.TempleCream
import com.example.viewmodel.BhaktiViewModel

@Composable
fun BhaktiMusicScreen(
    viewModel: BhaktiViewModel,
    playerManager: BhajanPlayerManager,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val bhajansState by viewModel.bhajansState.collectAsState()
    val favoritesState by viewModel.favoritesState.collectAsState()
    val playerState by playerManager.state.collectAsState()
    val isHindi = language == "hi"

    var selectedCategory by remember { mutableStateOf("All") }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val favoriteIds = ((favoritesState as? UiState.Success)?.data ?: emptyList())
        .filter { it.itemType == "bhajan" }
        .map { it.itemId }
        .toSet()

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "भजन और भक्ति संगीत" else "Bhakti Music Player",
                subtitle = if (isHindi) "भजन, आरती, चालीसा एवं मंत्र (No Autoplay)" else "Aarti, Chalisa & Devotional Player",
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
            // Artwork Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(146.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bhajan_banner),
                    contentDescription = "Bhakti Sangeet Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Active Devotional Player Card (Play, Pause, Next, Previous, Volume)
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, LuminousGold, RoundedCornerShape(22.dp))
                    .testTag("bhajan_player_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val track = playerState.currentTrack
                    Text(
                        text = track?.title ?: if (isHindi) "सुनने के लिए नीचे से भजन चुनें" else "Select a track below to play",
                        style = MaterialTheme.typography.titleMedium,
                        color = LuminousGold
                    )
                    if (track != null) {
                        Text(
                            text = "${track.artist} • ${track.category} • ${track.durationLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TempleCream.copy(alpha = 0.85f)
                        )
                    }
                    if (playerState.playbackModeNote.isNotBlank()) {
                        Text(
                            text = playerState.playbackModeNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFE082)
                        )
                    }
                    if (!playerState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = playerState.errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFB4AB)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playerManager.previous() },
                            modifier = Modifier.testTag("player_previous_button")
                        ) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "पिछला", tint = LuminousGold, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Surface(
                            shape = CircleShape,
                            color = RoyalSaffron,
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { playerManager.togglePlayPause() }
                                .testTag("player_play_pause_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playerState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "चलाएँ/रोकें",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        IconButton(
                            onClick = { playerManager.next() },
                            modifier = Modifier.testTag("player_next_button")
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "अगला", tint = LuminousGold, modifier = Modifier.size(32.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "आवाज़", tint = LuminousGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = playerState.volume,
                            onValueChange = { playerManager.setVolume(it) },
                            modifier = Modifier.weight(1f).testTag("player_volume_slider")
                        )
                    }

                    if (!track?.lyrics.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x44000000),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = track!!.lyrics!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = TempleCream,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // Categories + Favorites Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showOnlyFavorites,
                    onClick = { showOnlyFavorites = !showOnlyFavorites },
                    label = { Text(if (isHindi) "❤ पसंदीदा" else "❤ Favorites") }
                )
                DevotionalSeedCatalog.bhajanCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            when (val st = bhajansState) {
                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                is UiState.Error -> InfoNoticeBanner(title = "त्रुटि", message = st.message)
                is UiState.Success -> {
                    val filtered = st.data.filter { b ->
                        val catMatch = selectedCategory == "All" || b.category.equals(selectedCategory, ignoreCase = true)
                        val favMatch = !showOnlyFavorites || b.id in favoriteIds
                        catMatch && favMatch
                    }
                    playerManager.setPlaylist(filtered)
                    if (filtered.isEmpty()) {
                        Text(if (isHindi) "कोई भजन नहीं मिला।" else "No bhajans found.")
                    } else {
                        filtered.forEach { bhajan ->
                            val isFav = bhajan.id in favoriteIds
                            val isActive = playerState.currentTrack?.id == bhajan.id
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isActive) 1.5.dp else 1.dp,
                                        color = if (isActive) RoyalSaffron else SacredGold.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { playerManager.playTrack(bhajan, filtered) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        contentDescription = null,
                                        tint = RoyalSaffron,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bhajan.title, style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
                                        Text(
                                            "${bhajan.artist} • ${bhajan.category} • ${bhajan.durationLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(bhajan.id, "bhajan", bhajan.title, isFav) }
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = "पसंदीदा",
                                            tint = if (isFav) LotusRose else DeepMaroon
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
