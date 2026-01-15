# Overseerr KMP Client

A native, multiplatform client for [Overseerr](https://overseerr.dev/) built with **Kotlin Multiplatform** and **Jetpack Compose**. This app provides a seamless mobile experience for managing your media requests on Android.

## ✨ Features

* **Media Discovery**: Browse trending movies and TV shows across platforms.
* **Request Management**: Submit requests, select specific seasons/quality profiles, and monitor status.
* **Offline First**: Built with local caching to work seamlessly without an internet connection.
* **Push Notifications**: Real-time alerts for request approvals and media availability (via Firebase).
* **Material Design**: Modern "Material You" interface with dynamic theming.

## 🛠️ Tech Stack

* **Language**: Kotlin (Multiplatform)
* **UI**: Jetpack Compose Multiplatform
* **Architecture**: MVVM / Clean Architecture
* **Dependency Injection**: Koin
* **Networking**: Ktor
* **Database**: Room
* **Styles**: Material 3

## 🚀 Getting Started

### Prerequisites

* Android Studio Hedgehog or newer
* JDK 17+
* A running Overseerr instance

### Quick Setup

1. **Clone the Repository**

    ```bash
    git clone https://github.com/your-username/stream-app.git
    cd stream-app
    ```

2. **Configure Firebase (Push Notifications)**
    * The project requires `google-services.json` for compilation.
    * **Debug**: Place your debug config in `androidApp/src/debug/google-services.json`.
    * **Release**: Place your release config in `androidApp/src/release/google-services.json`.
    * **CI/CD**: Use `scripts/ci-setup-firebase.sh` to inject these from environment variables.
    * *Note: These files are git-ignored for security.*

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

This project is for demonstration purposes.
