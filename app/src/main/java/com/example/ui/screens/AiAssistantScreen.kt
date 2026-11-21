package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredGold
import com.example.ui.theme.TempleCream
import com.example.viewmodel.BhaktiViewModel

@Composable
fun AiAssistantScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val streamingReply by viewModel.streamingReply.collectAsState()
    val chatError by viewModel.chatError.collectAsState()
    val isHindi = language == "hi"

    var inputText by remember { mutableStateOf("") }

    val quickPrompts = if (isHindi) {
        listOf(
            "ॐ नमः शिवाय मंत्र का अर्थ बताएं",
            "एकादशी व्रत की सरल पूजा विधि",
            "हनुमान चालीसा पाठ का महत्व",
            "प्रातःकाल दैनिक साधना कैसे करें?"
        )
    } else {
        listOf(
            "Explain the meaning of Om Namah Shivaya",
            "Simple Ekadashi Vrat Puja Vidhi",
            "Significance of Maha Shivratri",
            "Daily morning devotional routine"
        )
    }

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "AI भक्ति सहायक" else "AI Devotional Assistant",
                subtitle = "Primary: Gemini (Firebase AI Logic) • Fallbacks Configurable",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Provider Status Strip
            Surface(
                color = Color(0xFFFFF8E1),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Primary: Gemini 3.8 Flash (Firebase AI Logic सक्रिय) | Optional Fallbacks (Groq / HF / DeepSeek / OpenAI): निष्क्रिय",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepMaroon,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // Quick Devotional Prompt Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { prompt ->
                    AssistChip(
                        onClick = { viewModel.sendDevotionalQuestion(prompt) },
                        label = { Text(prompt) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isUser) RoyalSaffron else Color(0xFFFFF8EE),
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .border(
                                    1.dp,
                                    if (isUser) RoyalSaffron else SacredGold.copy(alpha = 0.55f),
                                    RoundedCornerShape(18.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = msg.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) Color.White else DeepMaroon
                                )
                                Text(
                                    text = msg.timestampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isUser) TempleCream.copy(alpha = 0.8f) else DeepMaroon.copy(alpha = 0.55f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }

                if (streamingReply != null) {
                    item(key = "streaming_bubble") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFF8EE),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .border(1.dp, SacredGold, RoundedCornerShape(18.dp))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                if (streamingReply!!.isBlank()) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = RoyalSaffron, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isHindi) "विचार किया जा रहा है..." else "Thinking...", color = DeepMaroon)
                                } else {
                                    Text(streamingReply!!, style = MaterialTheme.typography.bodyMedium, color = DeepMaroon)
                                }
                            }
                        }
                    }
                }

                if (!chatError.isNullOrBlank()) {
                    item(key = "chat_error") {
                        InfoNoticeBanner(
                            title = if (isHindi) "AI त्रुटि" else "AI Error",
                            message = chatError!!,
                            containerColor = Color(0xFFFFEBEE),
                            borderColor = Color(0xFFB71C1C),
                            contentColor = Color(0xFFB71C1C)
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(if (isHindi) "अपना आध्यात्मिक प्रश्न पूछें..." else "Ask your devotional question...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input"),
                    maxLines = 3
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val q = inputText.trim()
                        if (q.isNotBlank()) {
                            inputText = ""
                            viewModel.sendDevotionalQuestion(q)
                        }
                    },
                    enabled = inputText.isNotBlank() && streamingReply == null,
                    modifier = Modifier
                        .size(48.dp)
                        .background(RoyalSaffron, RoundedCornerShape(14.dp))
                        .testTag("ai_chat_send_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "भेजें", tint = Color.White)
                }
            }
        }
    }
}
