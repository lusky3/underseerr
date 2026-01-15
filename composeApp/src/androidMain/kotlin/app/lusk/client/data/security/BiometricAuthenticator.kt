package app.lusk.client.data.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
/**
 * Handles biometric authentication using Android BiometricPrompt API.
 * Feature: overseerr-android-client, Property 32: Biometric Authentication Requirement
 * Validates: Requirements 8.3
 */
class BiometricAuthenticator(
    private val context: Context
) {
    private val biometricManager = BiometricManager.from(context)
    
    /**
     * Result of biometric authentication attempt.
     */
    sealed class AuthResult {
        data object Success : AuthResult()
        data class Error(val errorCode: Int, val errorMessage: String) : AuthResult()
        data object Failed : AuthResult()
        data object Cancelled : AuthResult()
    }
    
    /**
     * Check if biometric authentication is available on this device.
     * 
     * @return true if biometric hardware is available and enrolled, false otherwise
     */
    fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> false
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
            else -> false
        }
    }
    
    /**
     * Check if biometric hardware exists but no biometrics are enrolled.
     * 
     * @return true if user needs to enroll biometrics, false otherwise
     */
    fun needsEnrollment(): Boolean {
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
    }
    
    /**
     * Authenticate user with biometric prompt.
     * 
     * @param activity The FragmentActivity to show the prompt on
     * @param title Title for the biometric prompt
     * @param subtitle Optional subtitle for the prompt
     * @param description Optional description for the prompt
     * @param negativeButtonText Text for the negative button (default: "Cancel")
     * @return Flow emitting the authentication result
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel"
    ): Flow<AuthResult> {
        val resultChannel = Channel<AuthResult>(Channel.BUFFERED)
        
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                resultChannel.trySend(AuthResult.Success)
                resultChannel.close()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        resultChannel.trySend(AuthResult.Cancelled)
                    }
                    else -> {
                        resultChannel.trySend(AuthResult.Error(errorCode, errString.toString()))
                    }
                }
                resultChannel.close()
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Don't close channel here - user can retry
                resultChannel.trySend(AuthResult.Failed)
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                subtitle?.let { setSubtitle(it) }
                description?.let { setDescription(it) }
            }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
        
        return resultChannel.receiveAsFlow()
    }
    
    /**
     * Get a user-friendly message for biometric availability status.
     * 
     * @return Human-readable message explaining biometric status
     */
    fun getBiometricStatusMessage(): String {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> 
                "Biometric authentication is available"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> 
                "This device does not have biometric hardware"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> 
                "Biometric hardware is currently unavailable"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> 
                "No biometrics enrolled. Please add a fingerprint or face in device settings"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> 
                "A security update is required for biometric authentication"
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> 
                "Biometric authentication is not supported on this device"
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> 
                "Biometric status is unknown"
            else -> 
                "Biometric authentication is not available"
        }
    }
}
