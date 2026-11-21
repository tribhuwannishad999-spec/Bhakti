package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.authStateFlow
import com.example.core.configureAppCheck
import com.example.core.isVerifiedOwner
import com.example.core.signOutUser
import com.example.data.audio.BhajanPlayerManager
import com.example.data.audio.MantraAudioManager
import com.example.data.model.UiState
import com.example.data.repository.AiDevotionalRepository
import com.example.data.repository.BhaktiRepository
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BhaktiMusicScreen
import com.example.ui.screens.DhanpatraScreen
import com.example.ui.screens.FestivalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KathaScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.PalmistryScreen
import com.example.ui.screens.PanchangScreen
import com.example.ui.screens.RashifalKundliScreen
import com.example.ui.screens.Routes
import com.example.ui.screens.SpiritualNewsScreen
import com.example.ui.screens.TempleDarshanScreen
import com.example.ui.screens.VratUpvasScreen
import com.example.ui.theme.BhaktiDarshanTheme
import com.example.viewmodel.BhaktiViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent BEFORE UI / ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            BhaktiDarshanTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BhaktiDarshanAppRoot()
                }
            }
        }
    }
}

@Composable
fun BhaktiDarshanAppRoot() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedBhaktiShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "भक्त",
                isOwner = user.isVerifiedOwner()
            )
        }
    }
}

@Composable
private fun AuthenticatedBhaktiShell(
    currentUserId: String,
    displayName: String,
    isOwner: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: BhaktiViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) { "APPLICATION_KEY missing" }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                BhaktiViewModel(
                    repository = BhaktiRepository(db),
                    aiRepository = AiDevotionalRepository(),
                    currentUserId = currentUserId,
                    currentUserName = displayName,
                    isOwner = isOwner
                )
            }
        }
    )

    val mantraAudioManager = remember(context) { MantraAudioManager(context) }
    val bhajanPlayerManager = remember(context) { BhajanPlayerManager(context) }

    DisposableEffect(mantraAudioManager, bhajanPlayerManager) {
        onDispose {
            mantraAudioManager.release()
            bhajanPlayerManager.release()
        }
    }

    // Bind owner-configured Firestore mantra audio mappings directly into MantraAudioManager
    val mantraAudioState by viewModel.mantraAudioMappingsState.collectAsState()
    LaunchedEffect(mantraAudioState) {
        if (mantraAudioState is UiState.Success) {
            mantraAudioManager.updateCustomMappings((mantraAudioState as UiState.Success).data)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                audioManager = mantraAudioManager,
                onNavigate = { route ->
                    mantraAudioManager.stopAll()
                    navController.navigate(route)
                },
                onSignOut = {
                    mantraAudioManager.stopAll()
                    signOutUser(credentialManager, scope)
                }
            )
        }
        composable(Routes.DHANPATRA) {
            DhanpatraScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.RASHIFAL) {
            RashifalKundliScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.PALMISTRY) {
            PalmistryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.KATHA) {
            KathaScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.PANCHANG) {
            PanchangScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.VRAT) {
            VratUpvasScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.FESTIVALS) {
            FestivalsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.TEMPLES) {
            TempleDarshanScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.BHAJAN) {
            BhaktiMusicScreen(
                viewModel = viewModel,
                playerManager = bhajanPlayerManager,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AI_ASSISTANT) {
            AiAssistantScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.NEWS) {
            SpiritualNewsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.OWNER_DASHBOARD) {
            OwnerDashboardScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
