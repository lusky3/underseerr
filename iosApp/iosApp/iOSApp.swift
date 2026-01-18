import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Initialize Koin or other dependencies if needed
        // MainViewControllerKt.initKoin() // Uncomment if initKoin is exposed and needed
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
