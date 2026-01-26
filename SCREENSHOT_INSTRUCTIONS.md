# How to Generate Screenshots

The Android emulator environment requires a specific setup to bypass networking restrictions and capture high-quality screenshots with mock data.

## Prerequisites

1. Android Emulator running (API 35+ recommended)
2. Python 3 installed
3. ADB in your PATH

## Steps

1. **Start the Host Mock Server**
   This script runs a local server that mimics the Overseerr API, serving mock data and images.

   ```bash
   python3 host_mock_server.py
   ```

   *Note: This runs on port 5055.*

2. **Install the Debug App**
   Ensure the `androidApp-debug.apk` includes the `DebugUnderseerrApplication` changes.

   ```bash
   ./gradlew :androidApp:assembleDebug
   adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
   ```

3. **Run Automation Script**
   This script automates the UI flow: Permission -> Config (connects to 10.0.2.2) -> Auth -> Capture.

   ```bash
   python3 automate_screens.py
   ```

4. **Collect Screenshots**
   The screenshots will be saved in the `screenshots/` directory:
   - `01_home.png` (Discover)
   - `02_details.png` (Media Details)
   - `03_search.png` (Search/Requests)
   - `04_profile.png` (User Profile)

## Troubleshooting

- **Missing Images**: Ensure `host_mock_server.py` has access to `website/screenshots/issues.png` (used as placeholder).
- **Network Error**: The app is configured to look for `http://10.0.2.2:5055` when clicking "Debug: Use Mock Server". Ensure the python script is running on the host.
