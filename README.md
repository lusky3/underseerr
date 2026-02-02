# Underseerr - Native Overseerr Client

A native, multiplatform client for [Overseerr](https://overseerr.dev/) built with **Kotlin Multiplatform** and **Compose Multiplatform**.

## ✨ Features

* **Media Discovery**: Browse trending media and discover new content.
* **Subscription Gating**: (Pro) Integrated billing and premium feature management.
* **Secure Push**: Real-time alerts via Cloudflare Worker relay.
* **Modern UI**: Built with Jetpack Compose for a premium look and feel.

## 📖 Documentation

We maintain comprehensive documentation in the `docs/` folder (synced with our [GitHub Wiki](https://github.com/lusky3/underseerr/wiki)).

### 🚀 Getting Started

* **[Setup Guide](wiki/Setup.md)**: Build the app from source.
* **[User Guide](wiki/User_Guide.md)**: How to use Underseerr.
* **[Architecture](wiki/Architecture.md)**: Tech stack and module details.

### 🛡 Infrastructure

* **[Notification Relay](notification-relay/README.md)**: Self-host your own notification bridge.
* **[Pro Backend](pro-backend/README.md)**: Documentation for the billing/gating worker.

## 🚀 Quick Setup

```bash
git clone https://github.com/lusky3/underseerr.git
./gradlew :androidApp:assembleDebug
```

## 📄 License

Copyright 2026 Underseerr Team. Licensed under the Apache License, Version 2.0.
