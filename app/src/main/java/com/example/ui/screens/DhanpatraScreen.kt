package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.integrations.UpiPaymentAdapter
import com.example.data.integrations.WhatsAppIntegrationManager
import com.example.data.model.DonationRecord
import com.example.data.model.DonationStatus
import com.example.data.model.UiState
import com.example.data.repository.DevotionalSeedCatalog
import com.example.ui.theme.DeepMaroon
import com.example.ui.theme.LuminousGold
import com.example.ui.theme.RoyalSaffron
import com.example.ui.theme.SacredEmerald
import com.example.ui.theme.SacredGold
import com.example.ui.theme.TempleCream
import com.example.viewmodel.BhaktiViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DhanpatraScreen(
    viewModel: BhaktiViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val donationsState by viewModel.userDonationsState.collectAsState()
    val lastReceipt by viewModel.lastReceipt.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val isHindi = language == "hi"

    val presetAmounts = listOf(11L, 21L, 51L, 101L, 501L)
    val paymentMethods = listOf("UPI", "Google Pay", "PhonePe", "Paytm", "Other UPI", "Gateway")
    val purposeOptions = listOf("मंदिर सेवा", "गौ सेवा", "अन्नदान सेवा", "भक्ति दर्शन मंच सहयोग")

    var selectedAmount by remember { mutableLongStateOf(51L) }
    var customAmountText by remember { mutableStateOf("") }
    var donorName by remember { mutableStateOf("") }
    var donorPhone by remember { mutableStateOf("") }
    var donorEmail by remember { mutableStateOf("") }
    var selectedPurpose by remember { mutableStateOf(purposeOptions.first()) }
    var selectedPaymentApp by remember { mutableStateOf("UPI") }
    var transactionRef by remember { mutableStateOf("") }
    var upiLaunchNote by remember { mutableStateOf<String?>(null) }

    val isLiveUpiConfigured = remember { UpiPaymentAdapter.isLiveUpiConfigured() }
    val effectiveAmount = customAmountText.toLongOrNull()?.takeIf { it > 0L } ?: selectedAmount

    Scaffold(
        topBar = {
            BhaktiTopBar(
                title = if (isHindi) "धनपात्र — पावन सहयोग" else "Dhanpatra Donations",
                subtitle = if (isHindi) "पारदर्शी एवं सुरक्षित दान प्रणाली" else "Verified Donation System",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            WhatsAppIntegrationManager.shareDevotionalMessage(
                                context,
                                "🙏 Bhakti Darshan के धनपात्र सेवा अभियान से जुड़ें और धार्मिक व मंदिर सेवा कार्यों में सहयोग करें।"
                            )
                        },
                        modifier = Modifier.testTag("share_donation_whatsapp")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "शेयर करें", tint = LuminousGold)
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
            // Honest Payment Readiness Status Banner (Never invents UPI ID or fakes payment)
            if (!isLiveUpiConfigured) {
                InfoNoticeBanner(
                    title = if (isHindi) "भुगतान कॉन्फ़िगरेशन प्रतीक्षारत (सुरक्षित रूप से निष्क्रिय)" else "Payment Integration Configurable & Inactive",
                    message = if (isHindi) {
                        "ओनर द्वारा वास्तविक UPI_MERCHANT_VPA / QR और पेमेंट गेटवे कुंजी अभी AI Studio Secrets में नहीं जोड़ी गई हैं। हम कभी नकली UPI ID या नकली सफल भुगतान नहीं दिखाते। वास्तविक UPI जुड़ते ही लाइव UPI ऐप बटन स्वतः सक्रिय हो जाएगा।"
                    } else {
                        "Merchant UPI_MERCHANT_VPA and Payment Gateway credentials are awaiting setup in AI Studio Secrets. Live UPI launching stays safely inactive until real credentials are provided. You can still submit a real UTR reference for manual Owner verification."
                    },
                    containerColor = Color(0xFFFFF8E1),
                    borderColor = RoyalSaffron,
                    contentColor = DeepMaroon
                )
            } else {
                InfoNoticeBanner(
                    title = if (isHindi) "अधिकृत UPI मर्चेंट सक्रिय" else "Verified Merchant UPI Configured",
                    message = if (isHindi) {
                        "प्राप्तकर्ता: ${UpiPaymentAdapter.getMerchantName()}। UPI ऐप खोलने से भुगतान स्वतः सफल नहीं माना जाता — भुगतान के बाद UTR नंबर दर्ज करें; ओनर द्वारा बैंक सत्यापन के पश्चात रसीद 'सत्यापित' होगी।"
                    } else {
                        "Merchant: ${UpiPaymentAdapter.getMerchantName()}. Opening a UPI app does NOT confirm payment; submit your UTR reference below for owner/server verification."
                    },
                    containerColor = Color(0xFFE8F5E9),
                    borderColor = SacredEmerald,
                    contentColor = SacredEmerald
                )
            }

            if (!actionMessage.isNullOrBlank()) {
                InfoNoticeBanner(
                    title = if (isHindi) "सूचना" else "Notice",
                    message = actionMessage!!,
                    containerColor = Color(0xFFF3E5F5),
                    borderColor = DeepMaroon,
                    contentColor = DeepMaroon
                )
            }

            // Receipt Card after submitting a reference
            if (lastReceipt != null) {
                DonationReceiptCard(
                    record = lastReceipt!!,
                    isHindi = isHindi,
                    onDismiss = { viewModel.clearLastReceipt() }
                )
            }

            // 1. Amount Selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SacredGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isHindi) "1. दान राशि चुनें (₹)" else "1. Select Donation Amount (₹)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetAmounts.forEach { amt ->
                            val isSelected = customAmountText.isBlank() && selectedAmount == amt
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedAmount = amt
                                    customAmountText = ""
                                },
                                label = { Text("₹$amt", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("amount_chip_$amt")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { input -> customAmountText = input.filter { it.isDigit() }.take(7) },
                        label = { Text(if (isHindi) "अन्य राशि (Custom ₹)" else "Custom Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_amount_input")
                    )

                    Text(
                        text = if (isHindi) "2. दान का उद्देश्य" else "2. Donation Purpose",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        purposeOptions.forEach { purpose ->
                            FilterChip(
                                selected = selectedPurpose == purpose,
                                onClick = { selectedPurpose = purpose },
                                label = { Text(purpose) }
                            )
                        }
                    }

                    Text(
                        text = if (isHindi) "3. भुगतान माध्यम चुनें" else "3. Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            FilterChip(
                                selected = selectedPaymentApp == method,
                                onClick = { selectedPaymentApp = method },
                                label = { Text(method) },
                                modifier = Modifier.testTag("payment_method_${method.replace(" ", "_").lowercase()}")
                            )
                        }
                    }

                    // Launch UPI App button (Strictly disabled until real UPI_MERCHANT_VPA is configured)
                    OutlinedButton(
                        onClick = {
                            val uri = UpiPaymentAdapter.buildUpiPaymentUri(
                                amountInr = effectiveAmount,
                                purpose = selectedPurpose,
                                transactionNote = "BD_${System.currentTimeMillis() % 100000}"
                            )
                            if (uri != null) {
                                val pkg = when (selectedPaymentApp) {
                                    "Google Pay" -> "com.google.android.apps.nbu.paisa.user"
                                    "PhonePe" -> "com.phonepe.app"
                                    "Paytm" -> "net.one97.paytm"
                                    else -> null
                                }
                                val launched = UpiPaymentAdapter.launchUpiApp(context, uri, pkg)
                                upiLaunchNote = if (launched) {
                                    "UPI ऐप खोला गया। ध्यान दें: ऐप खुलना भुगतान की पुष्टि नहीं है — भुगतान के बाद 12 अंकों का UTR नीचे दर्ज करें।"
                                } else {
                                    "इस डिवाइस पर कोई UPI ऐप नहीं मिला।"
                                }
                            }
                        },
                        enabled = isLiveUpiConfigured && effectiveAmount > 0L,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("launch_upi_button")
                    ) {
                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isLiveUpiConfigured) {
                                "$selectedPaymentApp से ₹$effectiveAmount भुगतान हेतु खोलें"
                            } else {
                                "UPI ऐप भुगतान (मर्चेंट UPI ID कॉन्फ़िगर होने पर सक्रिय)"
                            }
                        )
                    }

                    if (!upiLaunchNote.isNullOrBlank()) {
                        Text(
                            text = upiLaunchNote!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepMaroon
                        )
                    }

                    HorizontalDivider(color = SacredGold.copy(alpha = 0.3f))

                    Text(
                        text = if (isHindi) "4. दाता विवरण एवं UTR संदर्भ दर्ज करें" else "4. Donor Details & Transaction Reference (UTR)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )

                    OutlinedTextField(
                        value = donorName,
                        onValueChange = { donorName = it },
                        label = { Text(if (isHindi) "दाता का नाम *" else "Donor Name *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donor_name_input")
                    )
                    OutlinedTextField(
                        value = donorPhone,
                        onValueChange = { donorPhone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                        label = { Text(if (isHindi) "मोबाइल नंबर (वैकल्पिक)" else "Phone Number (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = donorEmail,
                        onValueChange = { donorEmail = it },
                        label = { Text(if (isHindi) "ईमेल (वैकल्पिक)" else "Email (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        label = { Text(if (isHindi) "UTR / Transaction Reference *" else "UTR / Transaction Reference *") },
                        supportingText = {
                            Text(
                                if (isHindi) "स्थिति 'सत्यापन लंबित' के रूप में दर्ज होगी और ओनर द्वारा सत्यापित की जाएगी।"
                                else "Recorded strictly as PENDING_VERIFICATION until verified by owner."
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_utr_input")
                    )

                    Button(
                        onClick = {
                            viewModel.submitDonation(
                                donorName = donorName,
                                donorPhone = donorPhone,
                                donorEmail = donorEmail,
                                amountInr = effectiveAmount,
                                purpose = selectedPurpose,
                                paymentApp = selectedPaymentApp,
                                transactionRef = transactionRef
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_donation_reference_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalSaffron)
                    ) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "सत्यापन हेतु ₹$effectiveAmount दान दर्ज करें" else "Submit ₹$effectiveAmount Reference for Verification",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Thank-you sacred pledge card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DeepMaroon),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, LuminousGold, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🙏 आभार संदेश", style = MaterialTheme.typography.titleMedium, color = LuminousGold)
                    Text(
                        text = DevotionalSeedCatalog.THANK_YOU_MESSAGE_HI,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TempleCream
                    )
                }
            }

            // Donation History (Scoped to logged-in donor)
            Text(
                text = if (isHindi) "आपका दान इतिहास" else "Your Donation History",
                style = MaterialTheme.typography.titleLarge,
                color = DeepMaroon
            )

            when (val state = donationsState) {
                UiState.Loading, UiState.Idle -> {
                    Text(if (isHindi) "इतिहास लोड हो रहा है..." else "Loading history...")
                }
                is UiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = if (isHindi) "अभी कोई दान रिकॉर्ड दर्ज नहीं है।" else "No donation records yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.data.forEach { rec ->
                            DonationHistoryItem(record = rec, isHindi = isHindi)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DonationReceiptCard(
    record: DonationRecord,
    isHindi: Boolean,
    onDismiss: () -> Unit
) {
    val statusEnum = DonationStatus.fromRaw(record.status)
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF2)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SacredGold, RoundedCornerShape(20.dp))
            .testTag("donation_receipt_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = RoyalSaffron, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "दान पावती (Receipt)" else "Donation Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepMaroon
                    )
                }
                StatusPill(
                    text = if (isHindi) statusEnum.labelHi else statusEnum.labelEn,
                    bgColor = when (statusEnum) {
                        DonationStatus.VERIFIED_SUCCESS -> SacredEmerald
                        DonationStatus.PENDING_VERIFICATION -> RoyalSaffron
                        DonationStatus.FAILED -> Color(0xFFB71C1C)
                        DonationStatus.REFUNDED -> Color(0xFF455A64)
                    }
                )
            }
            Text("रसीद संख्या: ${record.receiptNumber}", style = MaterialTheme.typography.bodyMedium)
            Text("दाता: ${record.donorName} • राशि: ₹${record.amountInr}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("उद्देश्य: ${record.purpose} • माध्यम: ${record.paymentApp}", style = MaterialTheme.typography.bodySmall)
            Text("UTR / Reference: ${record.transactionRef}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = SacredGold.copy(alpha = 0.4f))
            Text(
                text = DevotionalSeedCatalog.THANK_YOU_MESSAGE_HI,
                style = MaterialTheme.typography.bodySmall,
                color = DeepMaroon
            )
            OutlinedButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(if (isHindi) "बंद करें" else "Close")
            }
        }
    }
}

@Composable
private fun DonationHistoryItem(record: DonationRecord, isHindi: Boolean) {
    val statusEnum = DonationStatus.fromRaw(record.status)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SacredGold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${record.amountInr} • ${record.purpose}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepMaroon
                )
                StatusPill(
                    text = if (isHindi) statusEnum.labelHi else statusEnum.labelEn,
                    bgColor = when (statusEnum) {
                        DonationStatus.VERIFIED_SUCCESS -> SacredEmerald
                        DonationStatus.PENDING_VERIFICATION -> RoyalSaffron
                        DonationStatus.FAILED -> Color(0xFFB71C1C)
                        DonationStatus.REFUNDED -> Color(0xFF455A64)
                    }
                )
            }
            Text("रसीद: ${record.receiptNumber} | ${record.paymentApp}", style = MaterialTheme.typography.bodySmall)
            Text("संदर्भ (UTR): ${record.transactionRef}", style = MaterialTheme.typography.bodySmall)
            if (!record.ownerNote.isNullOrBlank()) {
                Text(
                    text = "ओनर टिप्पणी: ${record.ownerNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SacredEmerald
                )
            }
        }
    }
}
