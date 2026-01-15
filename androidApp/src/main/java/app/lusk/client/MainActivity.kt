package app.lusk.client

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import app.lusk.client.data.security.BiometricAuthenticator
import app.lusk.client.navigation.Screen
import app.lusk.client.presentation.main.MainScreen
import app.lusk.client.presentation.main.MainViewModel
import app.lusk.client.presentation.security.LockScreen
import app.lusk.client.ui.theme.OverseerrTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Main activity for the Overseerr Android Client.
 * Entry point for the application UI.
 */
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModel()
    
    private val biometricAuthenticator: BiometricAuthenticator by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle initial deep link if any
        handleIntent(intent)
        
        setContent {
            val themePreference by viewModel.themePreference.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(isBiometricEnabled) {
                viewModel.checkInitialLockState(isBiometricEnabled)
            }
            
            // Auto-trigger authentication when locked
            LaunchedEffect(isAppLocked) {
                if (isAppLocked) {
                    authenticate()
                }
            }
            
            OverseerrTheme(themePreference = themePreference) {
                if (isAppLocked) {
                    LockScreen(
                        onUnlockClick = {
                            scope.launch { authenticate() }
                        }
                    )
                } else {
                    val startRouteByIntent = intent?.data?.let { Screen.parseDeepLink(it.toString()) }
                    MainScreen(
                        startDestination = startRouteByIntent ?: Screen.Splash.route
                    )
                }
            }
        }
    }
    
    private suspend fun authenticate() {
        biometricAuthenticator.authenticate(
            activity = this,
            title = "Unlock Lusk",
            subtitle = "Verify your identity to access the app"
        ).collect { result ->
            if (result is BiometricAuthenticator.AuthResult.Success) {
                viewModel.setAppLocked(false)
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent) {
        val action = intent.action
        val data = intent.data
        if (android.content.Intent.ACTION_VIEW == action && data != null) {
            // Logic to handle deep link
        }
    }
}
