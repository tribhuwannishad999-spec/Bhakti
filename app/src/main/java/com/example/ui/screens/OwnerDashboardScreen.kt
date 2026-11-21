package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.integrations.IntegrationStatusRegistry
import com.example.data.model.DonationStatus
import com.example.data.model.MantraItem
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredEmerald
import com.example.ui.theme.SacredGold
import com.example.viewmodel.BhaktiViewModel

@Composable
fun OwnerDashboardScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val actionMessage by viewModel.actionMessage.collectAsState()
    val allDonationsState by viewModel.ownerAllDonationsState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = "ओनर डैशबोर्ड (Admin)",
                subtitle = "Bhakti Darshan Content & UPI Verification",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (!viewModel.isOwner) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp)
            ) {
                InfoNoticeBanner(
                    title = "प्रतिबंधित प्रवेश (Owner Only)",
                    message = "यह डैशबोर्ड केवल सत्यापित ओनर खाते (Firebase Auth + Firestore Admin Rules) के लिए सुरक्षित है।",
                    containerColor = Color(0xFFFFEBEE),
                    borderColor = Color(0xFFB71C1C),
                    contentColor = Color(0xFFB71C1C)
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("UPI दान सत्यापन") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("कथा व भजन जोड़ें") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("मंदिर व पर्व जोड़ें") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("मंत्र ऑडियो व API") })
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!actionMessage.isNullOrBlank()) {
                    InfoNoticeBanner(title = "अपडेट", message = actionMessage!!)
                }

                when (selectedTab) {
                    0 -> OwnerDonationVerificationTab(allDonationsState, viewModel)
                    1 -> OwnerKathaBhajanTab(viewModel)
                    2 -> OwnerTempleFestivalTab(viewModel)
                    3 -> OwnerMantraAudioAndStatusTab(viewModel)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OwnerDonationVerificationTab(
    state: UiState<List<com.example.data.model.DonationRecord>>,
    viewModel: BhaktiViewModel
) {
    Text("सभी दान रिकॉर्ड एवं मैन्युअल UPI सत्यापन", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
    when (state) {
        UiState.Loading, UiState.Idle -> BoxCenterLoader()
        is UiState.Error -> InfoNoticeBanner(title = "त्रुटि", message = state.message)
        is UiState.Success -> {
            if (state.data.isEmpty()) {
                Text("अभी कोई दान रिकॉर्ड प्राप्त नहीं हुआ है।")
            } else {
                state.data.forEach { rec ->
                    var note by remember(rec.id) { mutableStateOf(rec.ownerNote ?: "") }
                    val statusEnum = DonationStatus.fromRaw(rec.status)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("₹${rec.amountInr} • ${rec.donorName}", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
                                StatusPill(
                                    text = statusEnum.labelHi,
                                    bgColor = if (statusEnum == DonationStatus.VERIFIED_SUCCESS) SacredEmerald else RoyalSaffron
                                )
                            }
                            Text("UTR: ${rec.transactionRef} | ${rec.paymentApp} | रसीद: ${rec.receiptNumber}", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                label = { Text("सत्यापन टिप्पणी (बैंक UTR जाँच नोट)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.ownerVerifyDonation(rec.id, DonationStatus.VERIFIED_SUCCESS, note) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SacredEmerald),
                                    modifier = Modifier.weight(1f).testTag("verify_donation_${rec.id}")
                                ) {
                                    Text("सत्यापित करें")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.ownerVerifyDonation(rec.id, DonationStatus.FAILED, note) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("असफल")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.ownerVerifyDonation(rec.id, DonationStatus.REFUNDED, note) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("रिफंड")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerKathaBhajanTab(viewModel: BhaktiViewModel) {
    // Add Katha Form
    var kTitle by remember { mutableStateOf("") }
    var kDesc by remember { mutableStateOf("") }
    var kCat by remember { mutableStateOf("शिव कथा") }
    var kUrl by remember { mutableStateOf("") }
    var kThumb by remember { mutableStateOf("") }
    var kDur by remember { mutableStateOf("20:00") }
    var kSpeaker by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("नई भक्ति कथा वीडियो जोड़ें", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
            OutlinedTextField(value = kTitle, onValueChange = { kTitle = it }, label = { Text("कथा शीर्षक *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kDesc, onValueChange = { kDesc = it }, label = { Text("विवरण *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kUrl, onValueChange = { kUrl = it }, label = { Text("YouTube / वीडियो URL *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = kThumb, onValueChange = { kThumb = it }, label = { Text("थंबनेल URL (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = kDur, onValueChange = { kDur = it }, label = { Text("अवधि") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = kSpeaker, onValueChange = { kSpeaker = it }, label = { Text("कथावाचक") }, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DevotionalSeedCatalog.kathaCategories.drop(1).take(3).forEach { c ->
                    FilterChip(selected = kCat == c, onClick = { kCat = c }, label = { Text(c) })
                }
            }
            Button(
                onClick = {
                    if (kTitle.isNotBlank() && kDesc.isNotBlank() && kUrl.isNotBlank()) {
                        viewModel.ownerAddKatha(kTitle, kDesc, kCat, kUrl, kThumb, kDur, kSpeaker)
                        kTitle = ""; kDesc = ""; kUrl = ""; kThumb = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron),
                modifier = Modifier.fillMaxWidth().testTag("owner_add_katha_button")
            ) {
                Text("कथा प्रकाशित करें")
            }
        }
    }

    // Add Bhajan Form
    var bTitle by remember { mutableStateOf("") }
    var bArtist by remember { mutableStateOf("") }
    var bCat by remember { mutableStateOf("Shiva") }
    var bAudio by remember { mutableStateOf("") }
    var bLyrics by remember { mutableStateOf("") }
    var bDur by remember { mutableStateOf("05:00") }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("नया भजन / आरती / चालीसा जोड़ें", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
            OutlinedTextField(value = bTitle, onValueChange = { bTitle = it }, label = { Text("भजन शीर्षक *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bArtist, onValueChange = { bArtist = it }, label = { Text("गायक / रचनाकार") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bAudio, onValueChange = { bAudio = it }, label = { Text("अधिकृत MP3 ऑडियो URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bLyrics, onValueChange = { bLyrics = it }, label = { Text("भजन के बोल (Lyrics)") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = bCat, onValueChange = { bCat = it }, label = { Text("श्रेणी (Shiva/Ram/Aarti...)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bDur, onValueChange = { bDur = it }, label = { Text("अवधि") }, modifier = Modifier.weight(1f))
            }
            Button(
                onClick = {
                    if (bTitle.isNotBlank()) {
                        viewModel.ownerAddBhajan(bTitle, bArtist, bCat, bAudio, bLyrics, bDur)
                        bTitle = ""; bArtist = ""; bAudio = ""; bLyrics = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("भजन प्रकाशित करें")
            }
        }
    }
}

@Composable
private fun OwnerTempleFestivalTab(viewModel: BhaktiViewModel) {
    var tName by remember { mutableStateOf("") }
    var tLoc by remember { mutableStateOf("") }
    var tDeity by remember { mutableStateOf("") }
    var tHist by remember { mutableStateOf("") }
    var tTime by remember { mutableStateOf("") }
    var tImg by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("नया मंदिर जोड़ें", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
            OutlinedTextField(value = tName, onValueChange = { tName = it }, label = { Text("मंदिर का नाम *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tLoc, onValueChange = { tLoc = it }, label = { Text("स्थान *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tDeity, onValueChange = { tDeity = it }, label = { Text("प्रमुख देवता *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tHist, onValueChange = { tHist = it }, label = { Text("इतिहास व महत्व *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tTime, onValueChange = { tTime = it }, label = { Text("दर्शन समय *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = tImg, onValueChange = { tImg = it }, label = { Text("छवि URL (वैकल्पिक)") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (tName.isNotBlank() && tLoc.isNotBlank() && tDeity.isNotBlank() && tHist.isNotBlank() && tTime.isNotBlank()) {
                        viewModel.ownerAddTemple(tName, tLoc, tDeity, tHist, tTime, tImg, null)
                        tName = ""; tLoc = ""; tDeity = ""; tHist = ""; tTime = ""; tImg = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("मंदिर सहेजें")
            }
        }
    }

    var fName by remember { mutableStateOf("") }
    var fDate by remember { mutableStateOf("") }
    var fSig by remember { mutableStateOf("") }
    var fVidhi by remember { mutableStateOf("") }
    var fMantra by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("नया पर्व / त्योहार जोड़ें", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
            OutlinedTextField(value = fName, onValueChange = { fName = it }, label = { Text("पर्व का नाम *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fDate, onValueChange = { fDate = it }, label = { Text("तिथि / दिनांक *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fSig, onValueChange = { fSig = it }, label = { Text("महत्व *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fVidhi, onValueChange = { fVidhi = it }, label = { Text("पूजा विधि *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = fMantra, onValueChange = { fMantra = it }, label = { Text("मुख्य मंत्र *") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (fName.isNotBlank() && fDate.isNotBlank() && fSig.isNotBlank() && fVidhi.isNotBlank() && fMantra.isNotBlank()) {
                        viewModel.ownerAddFestival(fName, fDate, fSig, fVidhi, fMantra, null, null)
                        fName = ""; fDate = ""; fSig = ""; fVidhi = ""; fMantra = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("पर्व सहेजें")
            }
        }
    }
}

@Composable
private fun OwnerMantraAudioAndStatusTab(viewModel: BhaktiViewModel) {
    Text("मंत्र बबल गेम — कस्टम ऑडियो रिकॉर्डिंग मैपिंग (10 मंत्र)", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
    Text(
        text = "यदि कोई कस्टम MP3 URL खाली है तो डिवाइस का Hindi TTS उसी सटीक मंत्र को बोलता है। अपनी रिकॉर्डिंग का HTTPS URL जोड़कर सहेजें।",
        style = MaterialTheme.typography.bodySmall
    )
    MantraItem.entries.forEach { mantra ->
        var url by remember(mantra.id) { mutableStateOf("") }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${mantra.mantraHindi} (${mantra.deityHindi})", style = MaterialTheme.typography.titleSmall, color = DeepMaroon)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("कस्टम MP3/WAV HTTPS URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        viewModel.ownerSaveMantraAudio(
                            mantraId = mantra.id,
                            mantraText = mantra.mantraHindi,
                            audioUrl = url,
                            voiceLabel = if (url.isNotBlank()) "Owner Verified Recording" else "Hindi TTS Fallback"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron)
                ) {
                    Text("मैपिंग सहेजें")
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    Text("सिस्टम एवं तृतीय-पक्ष API स्थिति (Readiness Report)", style = MaterialTheme.typography.titleMedium, color = DeepMaroon)
    IntegrationStatusRegistry.getAllStatuses().forEach { item ->
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = DeepMaroon, modifier = Modifier.weight(1f))
                    StatusPill(
                        text = item.status,
                        bgColor = if (item.isConfigured) SacredEmerald else RoyalSaffron
                    )
                }
                Text(item.detailHi, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
