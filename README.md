# Underseerr - Overseerr Client

A native, multiplatform client for [Overseerr](https://overseerr.dev/) built with **Kotlin Multiplatform** and **Jetpack Compose**. This app provides a seamless mobile experience for managing your media requests on Android.

## ✨ Features

* **Media Discovery**: Browse trending movies and TV shows across platforms.
* **Request Management**: Submit requests, select specific seasons/quality profiles, and monitor status.
* **Offline First**: Built with local caching to work seamlessly without an internet connection.
* **Push Notifications**: Real-time alerts for request approvals and media availability (via Firebase).
* **Material Design**: Modern "Material You" interface with dynamic theming.

## 🔔 Notifications & Self-Hosting

Underseerr uses a secure relay for push notifications. By default, the app uses a hosted relay (included with a trial/subscription), but users are encouraged to self-host their own relay for maximum privacy and control.

[![Deploy to Cloudflare Workers](https://deploy.workers.cloudflare.com/button)](https://deploy.workers.cloudflare.com/?url=https://github.com/lusky3/underseerr/tree/main/notification-relay)

See the [Notification Relay Guide](./notification-relay/README.md) for detailed instructions.

## 🛠️ Tech Stack

* **Language**: Kotlin 2.3+ (Multiplatform)
* **UI**: Jetpack Compose Multiplatform
* **Architecture**: MVVM / Clean Architecture
* **Dependency Injection**: Koin
* **Networking**: Ktor
* **Database**: Room
* **Styles**: Material 3

## 🚀 Getting Started

For detailed setup instructions, please see the [Project Wiki](https://github.com/lusky3/overseerr-requests/wiki).

### Prerequisites

* Android Studio Ladybug or newer
* JDK 24+
* A running Overseerr instance

### Quick Setup

1. **Clone the Repository**

    ```bash
    git clone https://github.com/lusky3/overseerr-requests.git
    cd overseerr-requests
    ```

2. **Configure Environment**
    * We support [sdkman](https://sdkman.io/) for environment management (`.sdkmanrc` included).
    * `google-services.json` files are required for compilation. See [Wiki: Setup](https://github.com/lusky3/overseerr-requests/wiki/Setup) for details.

3. **Build and Run**

    ```bash
    ./gradlew :androidApp:assembleDebug
    ```

## 🏗️ Project Structure

* `composeApp`: The shared core logic and UI (Kotlin Multiplatform).
  * `commonMain`: Code shared across all platforms.
  * `androidMain`: Android-specific implementations.
  * `iosMain`: iOS-specific implementations.
* `androidApp`: Thin Android entry point configuration.
* `iosApp`: Thin iOS entry point configuration.

## 📄 License

Copyright 2026 lusky3

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
