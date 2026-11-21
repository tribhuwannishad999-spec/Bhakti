package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@Composable
fun SpiritualNewsScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val newsState by viewModel.newsState.collectAsState()
    val isHindi = language == "hi"

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "आध्यात्मिक समाचार" else "Spiritual News",
                subtitle = "NewsAPI & MediaStack Integration",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.refreshSpiritualNews() }) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val st = newsState) {
                UiState.Loading, UiState.Idle -> BoxCenterLoader()
                is UiState.Error -> {
                    InfoNoticeBanner(
                        title = if (isHindi) "News API क्रेडेंशियल प्रतीक्षारत" else "NewsAPI / MediaStack Credentials Required",
                        message = "${st.message}\nAI Studio Secrets पैनल में NEWS_API_KEY या MEDIASTACK_API_KEY जोड़ते ही धार्मिक, मंदिर और त्योहार समाचार मूल स्रोत एवं प्रकाशन तिथि सहित यहाँ दिखाई देंगे।"
                    )
                }
                is UiState.Success -> {
                    if (st.data.isEmpty()) {
                        Text(if (isHindi) "अभी कोई नया समाचार उपलब्ध नहीं है।" else "No articles returned.")
                    } else {
                        st.data.forEach { article ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                    .clickable {
                                        if (article.url.startsWith("https://") || article.url.startsWith("http://")) {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        StatusPill(text = article.sourceName, bgColor = RoyalSaffron)
                                        Text(article.publishedAt, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(article.title, style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
                                    if (article.description.isNotBlank()) {
                                        Text(article.description, style = MaterialTheme.typography.bodySmall)
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
