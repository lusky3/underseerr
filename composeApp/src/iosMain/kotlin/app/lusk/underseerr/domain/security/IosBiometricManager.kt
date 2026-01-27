package app.lusk.underseerr.domain.security

class IosBiometricManager : BiometricManager {
    override fun isBiometricAvailable(): Boolean {
        // TODO: Implement using LocalAuthentication context
        return false
    }
}
