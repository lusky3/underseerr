package app.lusk.underseerr

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.lusk.underseerr.data.security.BiometricAuthenticator
import app.lusk.underseerr.navigation.Screen
import app.lusk.underseerr.presentation.main.MainScreen
import app.lusk.underseerr.presentation.main.MainViewModel
import app.lusk.underseerr.presentation.security.LockScreen
import app.lusk.underseerr.ui.theme.UnderseerrTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Main activity for the Underseerr.
 * Entry point for the application UI.
 */
class MainActivity : FragmentActivity() {
    private val viewModel: MainViewModel by viewModel()
    
    private val biometricAuthenticator: BiometricAuthenticator by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        app.lusk.underseerr.util.CurrentActivityHolder.set(this)
        enableEdgeToEdge()
        
        // Handle initial deep link if any
        // Handle initial deep link if any
        handleIntent(intent)
        
        // Check for notification permission on startup
        viewModel.requestNotificationPermission()

        // Fetch and log current FCM token for testing
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                println("FCM TOKEN: ${task.result}")
            }
        }
        
        setContent {
            val themePreference by viewModel.themePreference.collectAsState()
            val isAppLocked by viewModel.isAppLocked.collectAsState()
            val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(isBiometricEnabled) {
                isBiometricEnabled?.let { enabled ->
                    viewModel.checkInitialLockState(enabled)
                }
            }
            
            // Auto-trigger authentication when locked
            LaunchedEffect(isAppLocked) {
                if (isAppLocked) {
                    authenticate()
                }
            }
            
            UnderseerrTheme(themePreference = themePreference) {
                if (isAppLocked) {
                    LockScreen(
                        onUnlockClick = {
                            scope.launch { authenticate() }
                        }
                    )
                } else {
                    val startRouteByIntent = intent?.data?.let { Screen.parseDeepLink(it.toString()) }
                    MainScreen(
                        startDestination = startRouteByIntent ?: Screen.Splash
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

    override fun onResume() {
        super.onResume()
        app.lusk.underseerr.util.CurrentActivityHolder.set(this)
        viewModel.syncNotificationState()
    }

    override fun onDestroy() {
        super.onDestroy()
        app.lusk.underseerr.util.CurrentActivityHolder.clear()
    }
}
