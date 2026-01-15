package app.lusk.client

import android.app.Application
import app.lusk.client.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory

/**
 * Application class for Overseerr Android Client.
 */
class OverseerrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        initKoin(this) {
            androidLogger()
            androidContext(this@OverseerrApplication)
            workManagerFactory()
            modules(app.lusk.client.di.androidAppModule)
        }
        
        // Mock server disabled to prevent confusion with real server connections
        // if (app.lusk.client.BuildConfig.DEBUG) {
        //     startMockServer()
        // }
    }
    
    private fun startMockServer() {
        Thread {
            try {
                app.lusk.client.mock.MockOverseerrServer().apply {
                    start(5055)
                }
                android.util.Log.d("OverseerrApp", "Mock server started on port 5055")
            } catch (e: Exception) {
                android.util.Log.e("OverseerrApp", "Failed to start mock server", e)
            }
        }.start()
    }
}
