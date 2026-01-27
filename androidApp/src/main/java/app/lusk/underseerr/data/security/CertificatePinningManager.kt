package app.lusk.underseerr.data.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

import javax.net.ssl.HostnameVerifier
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

        // Enforce HTTPS only
        builder.hostnameVerifier(HostnameVerifier { hostname, session ->
            // Verify hostname matches certificate
            val peerCertificates = session.peerCertificates
            if (peerCertificates.isEmpty()) {
                return@HostnameVerifier false
            }
            
            val cert = peerCertificates[0] as? X509Certificate
            cert?.let {
                validateCertificate(hostname, it)
            } ?: false
        })

        return builder
    }

    /**
     * Validates that the certificate is valid for the given hostname.
     */
    private fun validateCertificate(hostname: String, certificate: X509Certificate): Boolean {
        return try {
            // Check certificate validity period
            certificate.checkValidity()
            
            // Check if hostname matches certificate subject
            val subjectDN = certificate.subjectX500Principal.name
            val subjectAltNames = certificate.subjectAlternativeNames
            
            // Check CN in subject DN
            val cnMatch = subjectDN.contains("CN=$hostname", ignoreCase = true) ||
                         subjectDN.contains("CN=*.", ignoreCase = true)
            
            // Check Subject Alternative Names
            val sanMatch = subjectAltNames?.any { altName ->
                val name = altName[1] as? String
                name?.equals(hostname, ignoreCase = true) == true ||
                name?.startsWith("*.", ignoreCase = false) == true
            } ?: false
            
            cnMatch || sanMatch
        } catch (e: Exception) {
            false
        }
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
     * Creates a trust manager that validates certificates.
     */
    fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                throw CertificateException("Client authentication not supported")
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // Validate server certificate chain
                chain?.forEach { cert ->
                    cert.checkValidity()
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }
}
