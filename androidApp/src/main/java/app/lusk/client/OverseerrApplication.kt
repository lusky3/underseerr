package app.lusk.client

import android.app.Application
import app.lusk.client.di.initKoin
import app.lusk.client.util.PlatformContext
import io.sentry.android.core.SentryAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory

/**
 * Application class for Overseerr Android Client.
 */
class OverseerrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Sentry only if DSN is configured
        // This allows the open source version to run without Sentry
        initSentryIfConfigured()
        
        initKoin(PlatformContext(this)) {
            androidLogger()
            androidContext(this@OverseerrApplication)
            workManagerFactory()
            modules(app.lusk.client.di.androidAppModule)
        }
    }
    
    /**
     * Initialize Sentry for crash reporting and error monitoring.
     * Only initializes if SENTRY_DSN is configured at build time.
     * 
     * For contributors: To enable Sentry in your builds, set the SENTRY_DSN
     * environment variable before building.
     */
    private fun initSentryIfConfigured() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isNotBlank()) {
            SentryAndroid.init(this) { options ->
                options.dsn = dsn
                options.isEnableAutoSessionTracking = true
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
                // Set release version for tracking
                options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}"
                
                // Enable Compose user interaction tracking
                options.isEnableUserInteractionTracing = true
                options.isEnableUserInteractionBreadcrumbs = true
                
                // Performance monitoring sample rate (10% in production, 100% in debug)
                options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
                
                // Profile performance issues (10% in production, 100% in debug)
                options.profilesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.1
            }
        }
    }
}
