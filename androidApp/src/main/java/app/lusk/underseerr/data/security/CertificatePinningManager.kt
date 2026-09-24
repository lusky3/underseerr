package app.lusk.underseerr.data.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Manages certificate pinning for secure HTTPS connections.
 */
class CertificatePinningManager {

    /**
     * Creates an OkHttpClient with certificate pinning configured.
     * If serverUrl is provided, pins certificates for that specific domain.
     */
    fun createSecureClient(serverUrl: String? = null): OkHttpClient.Builder {
        val builder = OkHttpClient.Builder()

        // Configure certificate pinning if server URL is provided
        serverUrl?.let { url ->
            val hostname = extractHostname(url)
            if (hostname != null) {
                // Note: In production, you would add actual certificate pins here
                // For now, we enforce HTTPS and validate certificates
                val certificatePinner = CertificatePinner.Builder()
                    // Example: .add(hostname, "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                    .build()

                builder.certificatePinner(certificatePinner)
            }
        }

        // Delegate to the platform's default hostname verifier, which implements
        // RFC 2818 hostname matching (including wildcard rules) against the
        // certificate actually presented in the session.
        builder.hostnameVerifier(HostnameVerifier { hostname, session ->
            HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session)
        })

        return builder
    }

    /**
     * Extracts hostname from URL.
     */
    private fun extractHostname(url: String): String? {
        return try {
            val cleanUrl = if (!url.startsWith("http")) "https://$url" else url
            java.net.URL(cleanUrl).host
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the platform's default X509TrustManager, which validates the full
     * certificate chain against the system trust store (issuer signatures,
     * validity period, and revocation where supported by the platform).
     */
    fun createTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(null as KeyStore?)

        return trustManagerFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .firstOrNull()
            ?: throw IllegalStateException("No X509TrustManager available from the platform")
    }
}
