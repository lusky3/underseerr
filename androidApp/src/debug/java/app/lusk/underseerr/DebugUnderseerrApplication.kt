package app.lusk.underseerr

import app.lusk.underseerr.mock.MockOverseerrServer
import android.util.Log

/**
 * Debug application class that starts a mock Overseerr server.
 * This runs locally on the device/emulator on port 8080.
 */
class DebugUnderseerrApplication : UnderseerrApplication() {
    
    companion object {
        var mockServer: MockOverseerrServer? = null
        private const val TAG = "DebugApp"
        private const val MOCK_PORT = 5055
    }

    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d(TAG, "Starting Mock Overseerr Server on port $MOCK_PORT...")
            mockServer = MockOverseerrServer().apply {
                start(MOCK_PORT)
            }
            Log.d(TAG, "Mock Server running at: ${mockServer?.baseUrl}")
            Log.d(TAG, "Use URL: http://localhost:$MOCK_PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start mock server", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        mockServer?.shutdown()
    }
}
