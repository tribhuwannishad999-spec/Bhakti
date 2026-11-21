package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.PalmistryAnalysis
import com.example.data.model.UiState
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredEmerald
import com.example.ui.theme.SacredGold
import com.example.ui.theme.SpiritualPurple
import com.example.viewmodel.BhaktiViewModel
import java.io.File

data class PalmQualityCheck(
    val isAcceptable: Boolean,
    val message: String
)

fun validatePalmBitmapQuality(bitmap: Bitmap): PalmQualityCheck {
    if (bitmap.width < 240 || bitmap.height < 240) {
        return PalmQualityCheck(
            false,
            "छवि का रिज़ॉल्यूशन बहुत कम है (${bitmap.width}x${bitmap.height})। कृपया स्पष्ट और नज़दीकी हथेली की फोटो लें।"
        )
    }
    // Sample brightness across 25 grid points to detect extremely dark/overexposed shots
    var lumaSum = 0.0
    var count = 0
    for (gx in 1..5) {
        for (gy in 1..5) {
            val px = bitmap.getPixel((bitmap.width * gx) / 6, (bitmap.height * gy) / 6)
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            lumaSum += (0.299 * r + 0.587 * g + 0.114 * b)
            count++
        }
    }
    val avgLuma = lumaSum / count
    return when {
        avgLuma < 32.0 -> PalmQualityCheck(false, "फोटो बहुत अंधेरी है। कृपया अच्छी रोशनी में हथेली की स्पष्ट तस्वीर लें।")
        avgLuma > 246.0 -> PalmQualityCheck(false, "फोटो में अत्यधिक चमक है जिससे रेखाएँ स्पष्ट नहीं दिख रहीं।")
        else -> PalmQualityCheck(true, "छवि गुणवत्ता प्रारंभिक जांच में उपयुक्त है (${bitmap.width}x${bitmap.height})।")
    }
}

@Composable
fun PalmistryScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val palmistryState by viewModel.palmistryState.collectAsState()
    val isHindi = language == "hi"

    var selectedHand by remember { mutableStateOf("Right Hand (दायाँ हाथ)") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qualityCheck by remember { mutableStateOf<PalmQualityCheck?>(null) }
    var showLineGuideOverlay by remember { mutableStateOf(true) }
    var readingLangHindi by remember { mutableStateOf(isHindi) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    fun loadBitmapFromUri(uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    selectedBitmap = bmp
                    qualityCheck = validatePalmBitmapQuality(bmp)
                    viewModel.resetPalmistry()
                    cameraError = null
                } else {
                    cameraError = "चयनित छवि पढ़ी नहीं जा सकी।"
                }
            }
        } catch (e: Exception) {
            cameraError = "फोटो लोड करने में त्रुटि: ${e.localizedMessage ?: "Error"}"
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            loadBitmapFromUri(uri)
        }
    }

    fun launchCameraCapture() {
        try {
            val dir = File(context.cacheDir, "palm_captures").apply { mkdirs() }
            val file = File(dir, "palm_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            pendingCaptureUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            cameraError = "कैमरा प्रारंभ नहीं हो सका: ${e.localizedMessage ?: "Error"}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            cameraError = "कैमरा अनुमति अस्वीकार की गई। आप नीचे 'गैलरी से चुनें' का उपयोग कर सकते हैं।"
        }
    }

    // Zero-permission Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            loadBitmapFromUri(uri)
        }
    }

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "हस्तरेखा AI विश्लेषण" else "AI Palmistry (Hastarekha)",
                subtitle = if (isHindi) "पारंपरिक हस्तरेखा शास्त्र एवं विज़न AI" else "Traditional Hastarekha Vision Analysis",
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
            // Mandatory Traditional Practice Disclaimer
            InfoNoticeBanner(
                title = if (isHindi) "महत्वपूर्ण अस्वीकरण (Disclaimer)" else "Traditional Practice Notice",
                message = if (isHindi) {
                    "हस्तरेखा शास्त्र एक पारंपरिक एवं सांस्कृतिक व्याख्यात्मक पद्धति है, वैज्ञानिक रूप से सिद्ध भविष्यवाणी नहीं। यदि फोटो अस्पष्ट होगी तो AI कभी मनगढ़ंत परिणाम नहीं बनाएगा।"
                } else {
                    "Palmistry (Hastarekha) is a traditional cultural interpretive practice, not scientifically proven prediction. If your photo is unclear, AI will honestly decline rather than fabricate a reading."
                }
            )

            // Hand Selection (Left / Right)
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SacredGold.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isHindi) "1. हाथ चुनें (बायाँ / दायाँ)" else "1. Select Hand (Left / Right)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Left Hand (बायाँ हाथ)", "Right Hand (दायाँ हाथ)").forEach { hand ->
                            FilterChip(
                                selected = selectedHand == hand,
                                onClick = { selectedHand = hand },
                                label = { Text(hand) },
                                modifier = Modifier.testTag("hand_select_${hand.take(4).lowercase()}")
                            )
                        }
                    }

                    Text(
                        text = if (isHindi) "2. स्पष्ट फोटो लें या अपलोड करें" else "2. Capture or Upload Clear Palm Photo",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    Text(
                        text = if (isHindi) {
                            "• उज्ज्वल प्राकृतिक रोशनी में खुली हथेली की सीधी फोटो लें\n• सभी प्रमुख रेखाएँ (जीवन, मस्तिष्क, हृदय) स्पष्ट दिखें"
                        } else {
                            "• Use bright, even lighting with fingers relaxed and palm flat\n• Ensure major palm lines are clearly visible"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) launchCameraCapture()
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("palmistry_camera_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isHindi) "कैमरा" else "Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("palmistry_gallery_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isHindi) "गैलरी" else "Gallery")
                        }
                    }

                    if (!cameraError.isNullOrBlank()) {
                        Text(cameraError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Image Preview + Interactive Palm Line Visualization Overlay
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = LuminousGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "हस्तरेखा गाइड ओवरले" else "Palm Line Guide Overlay",
                                style = MaterialTheme.typography.titleSmall,
                                color = LuminousGold
                            )
                        }
                        Switch(
                            checked = showLineGuideOverlay,
                            onCheckedChange = { showLineGuideOverlay = it }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF28080F)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Selected palm preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_palmistry_guide),
                                contentDescription = "Hastarekha reference illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (showLineGuideOverlay) {
                            PalmLineVisualizationCanvas(modifier = Modifier.fillMaxSize())
                        }
                    }

                    if (qualityCheck != null) {
                        StatusPill(
                            text = qualityCheck!!.message,
                            bgColor = if (qualityCheck!!.isAcceptable) SacredEmerald else Color(0xFFB71C1C)
                        )
                    }

                    // Legend for Palm Line Overlay
                    Text(
                        text = "सुनहरी = हृदय रेखा • केसरिया = मस्तिष्क रेखा • लाल = जीवन रेखा • बैंगनी = भाग्य रेखा • सफेद = सूर्य रेखा",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFF1C7)
                    )

                    Button(
                        onClick = {
                            selectedBitmap?.let { bmp ->
                                viewModel.analyzePalm(bmp, selectedHand)
                            }
                        },
                        enabled = selectedBitmap != null && qualityCheck?.isAcceptable == true && palmistryState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("analyze_palm_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron)
                    ) {
                        if (palmistryState is UiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(if (isHindi) "Gemini Vision विश्लेषण कर रहा है..." else "Analyzing Palm with Gemini Vision...")
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isHindi) "AI हस्तरेखा विश्लेषण प्रारंभ करें" else "Analyze Palm with Gemini AI",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Results Section
            when (val st = palmistryState) {
                is UiState.Error -> {
                    InfoNoticeBanner(
                        title = if (isHindi) "विश्लेषण त्रुटि" else "Analysis Error",
                        message = st.message,
                        containerColor = Color(0xFFFFEBEE),
                        borderColor = Color(0xFFB71C1C),
                        contentColor = Color(0xFFB71C1C)
                    )
                }
                is UiState.Success -> {
                    PalmistryResultView(
                        analysis = st.data,
                        showHindi = readingLangHindi,
                        onToggleLang = { readingLangHindi = !readingLangHindi }
                    )
                }
                else -> Unit
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PalmLineVisualizationCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Heart Line (Gold)
        val heartPath = Path().apply {
            moveTo(w * 0.24f, h * 0.42f)
            quadraticTo(w * 0.50f, h * 0.36f, w * 0.76f, h * 0.44f)
        }
        drawPath(heartPath, Color(0xFFFFD54F), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Head Line (Saffron)
        val headPath = Path().apply {
            moveTo(w * 0.26f, h * 0.54f)
            quadraticTo(w * 0.52f, h * 0.50f, w * 0.73f, h * 0.58f)
        }
        drawPath(headPath, Color(0xFFFF7043), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Life Line (Crimson)
        val lifePath = Path().apply {
            moveTo(w * 0.27f, h * 0.55f)
            quadraticTo(w * 0.46f, h * 0.67f, w * 0.42f, h * 0.88f)
        }
        drawPath(lifePath, Color(0xFFEF5350), style = Stroke(width = 5.5f, cap = StrokeCap.Round))

        // Fate Line (Purple-Lavender)
        val fatePath = Path().apply {
            moveTo(w * 0.52f, h * 0.88f)
            quadraticTo(w * 0.51f, h * 0.62f, w * 0.53f, h * 0.36f)
        }
        drawPath(fatePath, Color(0xFFCE93D8), style = Stroke(width = 4f, cap = StrokeCap.Round))

        // Sun Line (Ivory)
        drawLine(
            color = Color(0xCCFFFFFF),
            start = Offset(w * 0.64f, h * 0.68f),
            end = Offset(w * 0.65f, h * 0.38f),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PalmistryResultView(
    analysis: PalmistryAnalysis,
    showHindi: Boolean,
    onToggleLang: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            .testTag("palmistry_result_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showHindi) "हस्तरेखा विवरण (${analysis.handAnalyzed})" else "Palm Reading (${analysis.handAnalyzed})",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepMaroon
                )
                FilterChip(
                    selected = true,
                    onClick = onToggleLang,
                    label = { Text(if (showHindi) "English देखें" else "हिन्दी देखें") }
                )
            }

            if (!analysis.isClearPalm) {
                InfoNoticeBanner(
                    title = if (showHindi) "हथेली की तस्वीर स्पष्ट नहीं है" else "Palm Image Not Clear Enough",
                    message = analysis.qualityFeedback.ifBlank {
                        "कृपया बेहतर रोशनी में स्पष्ट हथेली की फोटो पुनः अपलोड करें। हम अस्पष्ट फोटो पर मनगढ़ंत परिणाम नहीं दिखाते।"
                    },
                    containerColor = Color(0xFFFFEBEE),
                    borderColor = Color(0xFFB71C1C),
                    contentColor = Color(0xFFB71C1C)
                )
            } else {
                Text(
                    text = analysis.qualityFeedback,
                    style = MaterialTheme.typography.bodySmall,
                    color = SacredEmerald
                )
                ReadingBlock(
                    title = if (showHindi) "जीवन रेखा (Life Line)" else "Life Line",
                    body = if (showHindi) analysis.lifeLineHi else analysis.lifeLineEn
                )
                ReadingBlock(
                    title = if (showHindi) "मस्तिष्क रेखा (Head Line)" else "Head Line",
                    body = if (showHindi) analysis.headLineHi else analysis.headLineEn
                )
                ReadingBlock(
                    title = if (showHindi) "हृदय रेखा (Heart Line)" else "Heart Line",
                    body = if (showHindi) analysis.heartLineHi else analysis.heartLineEn
                )
                ReadingBlock(
                    title = if (showHindi) "भाग्य रेखा (Fate Line)" else "Fate Line",
                    body = if (showHindi) analysis.fateLineHi else analysis.fateLineEn
                )
                ReadingBlock(
                    title = if (showHindi) "सूर्य रेखा (Sun Line)" else "Sun Line",
                    body = if (showHindi) analysis.sunLineHi else analysis.sunLineEn
                )
                ReadingBlock(
                    title = if (showHindi) "प्रमुख पर्वत (Major Palm Mounts)" else "Major Palm Mounts",
                    body = if (showHindi) analysis.majorMountsHi else analysis.majorMountsEn
                )
                ReadingBlock(
                    title = if (showHindi) "समग्र पारंपरिक सार" else "Overall Traditional Summary",
                    body = if (showHindi) analysis.overallSummaryHi else analysis.overallSummaryEn,
                    accent = SpiritualPurple
                )
            }
        }
    }
}

@Composable
private fun ReadingBlock(title: String, body: String, accent: Color = DeepMaroon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8EE), RoundedCornerShape(12.dp))
            .border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = accent)
        Text(body.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
    }
}
