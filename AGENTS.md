# Agent Instructions for Overseerr Android Client Development

This document provides guidance for AI agents working on the Overseerr Android Client project. It describes the available MCP (Model Context Protocol) servers and how to use them effectively during development.

## Available MCP Servers

This project has access to multiple MCP servers for development:

### Project-Specific Servers

#### 1. mcp-android

Android device automation server using uiautomator2 for comprehensive device interaction, UI testing, and app management. Provides direct control over Android devices for automated testing and debugging.

#### 2. mobile-dev (@cristianoaredes/mcp-mobile-server)

Comprehensive mobile development server with Android SDK and device management capabilities. Handles building, device management, and development workflows.

### Global Servers

#### 3. Context7

Documentation and code example retrieval for libraries and frameworks. Use this to get up-to-date documentation for Android, Kotlin, Jetpack Compose, and other dependencies.

#### 4. Augments

Framework-specific documentation, patterns, and code examples. Provides comprehensive guides for modern development frameworks including Android/Kotlin ecosystem.

## When to Use Each MCP Server

### Use `mcp-android` for

- **Device Connection**: Connect to Android devices via uiautomator2
- **Device Information**: Get comprehensive device info (serial, screen resolution, Android version, battery, WiFi, etc.)
- **App Management**: List installed apps, get current app, start/stop apps, clear app data
- **Screen Control**: Turn screen on/off, unlock screen, wait for screen state
- **UI Interaction**: Click elements by text/resource-id/description, long click, swipe, drag, send text input
- **UI Inspection**: Get element info, wait for elements, dump UI hierarchy as XML, scroll to elements
- **Hardware Keys**: Press hardware buttons (home, back, volume, power, enter, delete, etc.)
- **Screenshots**: Capture device screenshots to local files
- **Toast Messages**: Retrieve toast message text
- **Activity Monitoring**: Wait for specific activities to appear

### Use `mobile-dev` for

- **Environment Setup**: Checking Android SDK installation and configuration
- **Device Management**: Listing devices, creating AVDs, starting/stopping emulators
- **Build Operations**: Building APKs and app bundles (note: this is a Kotlin project, not Flutter)
- **APK Installation**: Installing APKs on devices with options like replace
- **Troubleshooting**: Checking health, fixing common issues
- **Logs**: Viewing Android logcat output

### Use `Context7` for

- **Library Documentation**: Get official documentation for Android libraries (Retrofit, Room, Hilt, etc.)
- **API References**: Look up specific API methods and their usage
- **Code Examples**: Find real-world code examples from official documentation
- **Version-Specific Info**: Get documentation for specific library versions
- **Best Practices**: Learn recommended patterns from official sources

### Use `Augments` for

- **Framework Guides**: Get comprehensive guides for Kotlin, Jetpack Compose, Material 3
- **Design Patterns**: Learn Android architecture patterns (MVVM, Clean Architecture)
- **Code Patterns**: Get framework-specific code examples and templates
- **Integration Help**: Learn how to integrate multiple frameworks together
- **Troubleshooting**: Find solutions to common framework-specific issues

## Common Development Workflows

### 1. Initial Environment Setup

**Check development environment:**

```
Use mobile-dev: health_check
- verbose: true
```

This will verify that all required tools (Android SDK, platform tools, build tools) are installed and properly configured.

**Check device status and ADB:**

```
Use mcp-android: get_device_status
```

Returns connection status, ADB availability, and basic device info.

**List available devices:**

```
Use mobile-dev: android_list_devices
OR
Use mcp-android: check_adb_and_list_devices
```

**If no devices available, create an Android emulator:**

```
Use mobile-dev: android_list_emulators (to see existing AVDs)
Use mobile-dev: android_create_avd (to create a new one if needed)
Use mobile-dev: android_start_emulator (to start it)
```

### 2. Building the Application

**Note**: This is a Kotlin/Gradle project, not Flutter. Build using Gradle commands:

```bash
./gradlew assembleDebug  # Build debug APK
./gradlew assembleRelease  # Build release APK
./gradlew bundleRelease  # Build app bundle
```

### 3. Installing and Running the App

**Install APK on device:**

```
Use mobile-dev: android_install_apk
- serial: <device-id>
- apkPath: app/build/outputs/apk/debug/app-debug.apk
- options: { replace: true }
```

**Launch the app:**

```
Use mcp-android: start_app
- package_name: com.example.overseerr_client
- device_id: <device-id> (optional, uses first device if not specified)
- wait: true
```

**Stop the app:**

```
Use mcp-android: stop_app
- package_name: com.example.overseerr_client
- device_id: <device-id> (optional)
```

**Clear app data:**

```
Use mcp-android: clear_app_data
- package_name: com.example.overseerr_client
- device_id: <device-id> (optional)
```

### 4. Testing and Debugging

**Connect to device (recommended first step):**

```
Use mcp-android: connect_device
- device_id: <device-id> (optional, connects to first device if not specified)
```

**Get device information:**

```
Use mcp-android: get_device_info
- device_id: <device-id> (optional)
```

Returns: serial, screen resolution, Android version, SDK level, battery status, WiFi IP, manufacturer, model, screen state

**Take a screenshot:**

```
Use mcp-android: screenshot
- filename: ./screenshots/test.png
- device_id: <device-id> (optional)
```

**Dump UI hierarchy:**

```
Use mcp-android: dump_hierarchy
- device_id: <device-id> (optional)
- pretty: true
- compressed: false
- max_depth: 50
```

Returns the complete UI hierarchy as XML for understanding screen structure and finding elements.

**Interact with UI elements:**

```
# Click on element by text
Use mcp-android: click
- selector: "Login"
- selector_type: "text"
- device_id: <device-id> (optional)
- timeout: 10

# Click by resource ID
Use mcp-android: click
- selector: "com.example.overseerr_client:id/login_button"
- selector_type: "resource_id"

# Long click
Use mcp-android: long_click
- selector: "Settings"
- selector_type: "text"
- duration: 1

# Send text input
Use mcp-android: send_text
- text: "search query"
- clear: true
- device_id: <device-id> (optional)

# Swipe gesture
Use mcp-android: swipe
- start_x: 500
- start_y: 1500
- end_x: 500
- end_y: 500
- duration: 0.5
- device_id: <device-id> (optional)

# Drag element to location
Use mcp-android: drag
- selector: "Item"
- selector_type: "text"
- to_x: 500
- to_y: 300
```

**Wait for elements and get info:**

```
# Wait for element to appear
Use mcp-android: wait_for_element
- selector: "Welcome"
- selector_type: "text"
- timeout: 10

# Get element information
Use mcp-android: get_element_info
- selector: "Login"
- selector_type: "text"
- timeout: 10

# Scroll to element
Use mcp-android: scroll_to
- selector: "Settings"
- selector_type: "text"
```

**View Android logs:**

```
Use mobile-dev: android_logcat
- serial: <device-id>
- lines: 100
- filter: "*:E" (for errors only, or "com.example.overseerr_client:*" for app logs)
```

**Screen control:**

```
# Turn screen on
Use mcp-android: screen_on
- device_id: <device-id> (optional)

# Turn screen off
Use mcp-android: screen_off
- device_id: <device-id> (optional)

# Unlock screen
Use mcp-android: unlock_screen
- device_id: <device-id> (optional)
```

**Press hardware keys:**

```
Use mcp-android: press_key
- key: "back" (or "home", "menu", "volume_up", "volume_down", "power", "enter", "delete")
- device_id: <device-id> (optional)
```

**Get toast messages:**

```
Use mcp-android: get_toast
- device_id: <device-id> (optional)
```

**Wait for activity:**

```
Use mcp-android: wait_activity
- activity: ".MainActivity"
- timeout: 10
- device_id: <device-id> (optional)
```

### 5. Device Management

**List all connected devices:**

```
Use mobile-dev: android_list_devices
OR
Use mcp-android: check_adb_and_list_devices
```

**Get installed apps:**

```
Use mcp-android: get_installed_apps
- device_id: <device-id> (optional)
```

Returns list of all installed package names.

**Get current foreground app:**

```
Use mcp-android: get_current_app
- device_id: <device-id> (optional)
```

Returns package name, activity, and version info of the current app.

**Stop all apps:**

```
Use mcp-android: stop_all_apps
- device_id: <device-id> (optional)
```

### 6. Troubleshooting

**Check MCP server health:**

```
Use mcp-android: mcp_health
```

**View device status:**

```
Use mcp-android: get_device_status
```

**Clean build artifacts (Gradle):**

```bash
./gradlew clean
```

### 7. Documentation and Code Examples

**Look up library documentation:**

```
Use Context7: resolve-library-id
- libraryName: "retrofit" (or "jetpack compose", "hilt", "room", etc.)
- query: "your question about the library"

Then use Context7: query-docs
- libraryId: "/square/retrofit" (from resolve-library-id result)
- query: "how to make POST requests with JSON body"
```

**Get framework guides and patterns:**

```
Use Augments: search_frameworks
- query: "kotlin coroutines" (or "jetpack compose", "material design", etc.)

Use Augments: get_framework_docs
- framework: "kotlin"
- section: "coroutines" (optional)

Use Augments: get_framework_examples
- framework: "jetpack-compose"
- pattern: "navigation" (or "state management", "theming", etc.)
```

**Get context for multiple frameworks:**

```
Use Augments: get_framework_context
- frameworks: ["kotlin", "jetpack-compose", "material-design"]
- task_description: "implementing authentication with biometric support"
```

## Task-Specific Guidance

### Task 1: Project Setup and Infrastructure

- Use `mobile-dev: health_check` to verify environment
- Ensure Android SDK is properly configured
- Use `Context7` to look up latest Gradle, Kotlin, and Compose versions
- Use `Augments: get_framework_docs` for Kotlin and Jetpack Compose setup guides

### Tasks 2-6: Core Implementation (Models, Security, Networking, Database)

- Use `Context7` to look up documentation for:
  - Kotlinx Serialization (data models)
  - Android Keystore (security)
  - Retrofit and OkHttp (networking)
  - Room Database (local storage)
  - Hilt/Dagger (dependency injection)
- Use `Augments: get_framework_examples` for:
  - MVVM architecture patterns
  - Kotlin Coroutines and Flow
  - Clean Architecture implementation
- Focus on code implementation and unit tests
- Use standard Kotlin/Android development practices

### Task 7: Authentication Module

- Use `Context7` to look up:
  - Android Biometric API documentation
  - OAuth implementation patterns
  - Secure storage best practices
- Use `Augments` for authentication flow patterns
- Use `mcp-android` to test OAuth flow on real devices
- Take screenshots of authentication screens for verification
- Test biometric authentication on physical devices

### Task 8: Checkpoint - Authentication Complete

- Build APK using Gradle: `./gradlew assembleDebug`
- Use `mobile-dev: android_install_apk` to install on test devices
- Use `mcp-android: start_app` to test authentication flow
- Use `mcp-android: screenshot` to document test results

### Tasks 9-10: Discovery and Request Modules

- Use `Context7` to look up:
  - Paging 3 library documentation
  - Coil image loading library
  - Compose LazyColumn and LazyGrid
- Use `Augments` for pagination and infinite scroll patterns
- Use `mcp-android` to test search functionality
- Test infinite scrolling by swiping
- Verify request submission flows
- Take screenshots of different states

### Task 11: Checkpoint - Core Features Complete

- Build and install app on multiple device types
- Test end-to-end flows using `mcp-android` interaction tools
- Verify on both phone and tablet layouts

### Tasks 12-13: Profile, Settings, and Notifications

- Use `Context7` to look up:
  - DataStore documentation
  - Firebase Cloud Messaging
  - Android notification channels
- Use `Augments` for Material 3 theming patterns
- Test theme changes on devices
- Verify notification delivery
- Test deep link navigation

### Task 14: Offline Support

- Use `Context7` to look up:
  - Room Database caching strategies
  - WorkManager for background sync
  - ConnectivityManager API
- Use `Augments` for offline-first architecture patterns
- Use `mcp-android` to test offline scenarios
- Disable network and verify cached content
- Re-enable network and verify sync

### Task 15: Material You UI and Theming

- Use `Context7` to look up Material 3 Compose documentation
- Use `Augments: get_framework_docs` for Material Design 3 guidelines
- Use `Augments: get_framework_examples` for:
  - Dynamic color theming
  - Adaptive layouts
  - Responsive design patterns
- Test on devices with different Android versions
- Verify dynamic theming on Android 12+
- Test adaptive layouts on tablets and foldables
- Test landscape mode by rotating device or using emulator controls

### Task 16: Image Loading

- Use `Context7` to look up Coil library documentation
- Use `Augments` for image loading optimization patterns

### Task 17: Error Handling and Crash Reporting

- Use `Context7` to look up exception handling in Kotlin Coroutines
- Use `Augments` for error handling patterns in Android

### Task 18: Navigation

- Use `Context7` to look up Jetpack Navigation Compose documentation
- Use `Augments` for navigation patterns and deep linking

### Task 19: Final Integration and Polish

- Run all tests
- Build release APK/AAB
- Test on multiple devices using `mcp-android`

### Task 20: Final Checkpoint

- Use `mobile-dev: health_check` to verify environment
- Build release APK/AAB using Gradle
- Test on all target devices
- Capture screenshots for documentation

## Best Practices

### 1. Always Check Device Availability First

Before running any device-specific commands, check device status and list available devices:

```
Use mcp-android: get_device_status
OR
Use mcp-android: check_adb_and_list_devices
```

### 2. Connect to Device Before Testing

For best results, explicitly connect to the device before running UI automation:

```
Use mcp-android: connect_device
- device_id: <device-id> (optional)
```

### 3. Use Appropriate Build Modes

- **debug**: For development and testing (includes debug symbols)
- **release**: For production builds (optimized, obfuscated)

### 4. Capture Evidence

Take screenshots during testing to document:

- UI states
- Error conditions
- Successful flows
- Different device layouts

### 5. Test on Multiple Devices

- Test on at least one phone and one tablet
- Test on different Android versions (API 26+)
- Test both portrait and landscape orientations

### 6. Monitor Logs

Use `android_logcat` to monitor app behavior and catch errors:

```
Use mobile-dev: android_logcat
- serial: <device-id>
- filter: "com.example.overseerr_client:*"
```

### 7. Automate Testing Flows

Use `mcp-android` to automate repetitive testing:

1. Connect to device with `connect_device`
2. Launch app with `start_app`
3. Wait for elements with `wait_for_element`
4. Interact with UI using `click`, `send_text`, `swipe`
5. Verify results with `get_element_info` or `dump_hierarchy`
6. Take screenshots with `screenshot`

### 8. Use Documentation Tools Effectively

When implementing new features:

1. Use `Context7: resolve-library-id` to find the library
2. Use `Context7: query-docs` to get specific API documentation
3. Use `Augments: get_framework_examples` to see implementation patterns
4. Combine official docs with practical examples for best results

### 9. Learn Framework Patterns

Before implementing complex features:

1. Use `Augments: search_frameworks` to find relevant frameworks
2. Use `Augments: get_framework_context` with multiple frameworks for integration guidance
3. Use `Augments: get_framework_examples` for specific patterns
4. Apply learned patterns to your implementation

### 10. Selector Types in mcp-android

When using UI interaction commands, you can select elements by:

- `text`: Visible text on the element
- `resource_id`: Android resource ID (e.g., "com.example.app:id/button")
- `description`: Content description for accessibility

## Troubleshooting Common Issues

### "No devices found"

```
1. Use mcp-android: check_adb_and_list_devices
2. If empty, use mobile-dev: android_list_emulators
3. Start an emulator or connect a physical device
4. Verify with mcp-android: get_device_status
```

### "Build failed"

```
1. Run: ./gradlew clean
2. Check dependencies in build.gradle files
3. Retry build: ./gradlew assembleDebug
```

### "App won't install"

```
1. Stop the app using mcp-android: stop_app
2. Clear app data using mcp-android: clear_app_data
3. Rebuild APK
4. Reinstall using mobile-dev: android_install_apk with replace: true
```

### "Element not found"

```
1. Use mcp-android: dump_hierarchy to see all UI elements
2. Check the exact text, resource_id, or description
3. Use mcp-android: wait_for_element with longer timeout
4. Ensure the screen is in the correct state
```

### "How do I implement X feature?"

```
1. Use Context7: resolve-library-id to find relevant libraries
2. Use Context7: query-docs to get API documentation
3. Use Augments: get_framework_examples for implementation patterns
4. Combine documentation with examples to implement the feature
```

### "Library version conflicts"

```
1. Use Context7 to check latest compatible versions
2. Use Augments to find migration guides if updating versions
3. Update dependencies in build.gradle
4. Run ./gradlew clean and rebuild
```

## Project-Specific Notes

### Package Name

The app package name should be: `com.example.overseerr_client` (or your chosen package name)

### Minimum SDK

API 26 (Android 8.0) - ensure test devices meet this requirement

### Target SDK

API 35 (Android 15) - test on latest Android versions when possible

### Build Outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- App Bundle: `app/build/outputs/bundle/release/app-release.aab`

### Test Directories

- Unit tests: `app/src/test/`
- Instrumented tests: `app/src/androidTest/`

### Commit Message Style

Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.

- Format: `<type>(<scope>): <subject>`
- Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci`, `build`, `revert`
- Example: `feat(auth): implement biometric login`

## Quick Reference

| Task | MCP Server | Command |
|------|-----------|---------|
| Check environment | mobile-dev | health_check |
| Check device status | mcp-android | get_device_status |
| List devices | mcp-android | check_adb_and_list_devices |
| Connect to device | mcp-android | connect_device |
| Get device info | mcp-android | get_device_info |
| Install APK | mobile-dev | android_install_apk |
| Launch app | mcp-android | start_app |
| Stop app | mcp-android | stop_app |
| Get current app | mcp-android | get_current_app |
| List installed apps | mcp-android | get_installed_apps |
| Clear app data | mcp-android | clear_app_data |
| Click element | mcp-android | click |
| Long click | mcp-android | long_click |
| Send text | mcp-android | send_text |
| Swipe | mcp-android | swipe |
| Drag | mcp-android | drag |
| Press key | mcp-android | press_key |
| Screen on/off | mcp-android | screen_on / screen_off |
| Unlock screen | mcp-android | unlock_screen |
| Take screenshot | mcp-android | screenshot |
| Dump UI hierarchy | mcp-android | dump_hierarchy |
| Wait for element | mcp-android | wait_for_element |
| Get element info | mcp-android | get_element_info |
| Scroll to element | mcp-android | scroll_to |
| Get toast | mcp-android | get_toast |
| Wait for activity | mcp-android | wait_activity |
| View logs | mobile-dev | android_logcat |
| Find library docs | Context7 | resolve-library-id + query-docs |
| Get code examples | Context7 | query-docs |
| Search frameworks | Augments | search_frameworks |
| Get framework guide | Augments | get_framework_docs |
| Get code patterns | Augments | get_framework_examples |
| Multi-framework help | Augments | get_framework_context |

## Example Workflows with All MCP Servers

### Implementing a New Feature (e.g., Biometric Authentication)

1. **Research** (Context7 + Augments):

   ```
   Context7: resolve-library-id
   - libraryName: "android biometric"
   - query: "biometric authentication implementation"
   
   Context7: query-docs
   - libraryId: "/androidx/biometric"
   - query: "how to implement fingerprint authentication"
   
   Augments: get_framework_examples
   - framework: "android"
   - pattern: "biometric-authentication"
   ```

2. **Implement** (Code):
   - Write BiometricAuthenticator class based on documentation
   - Integrate with SecurityManager
   - Add UI components

3. **Test** (mcp-android + mobile-dev):

   ```
   Build APK: ./gradlew assembleDebug
   mobile-dev: android_install_apk (install on device)
   mcp-android: start_app (launch app)
   Test biometric prompt on physical device
   mcp-android: screenshot (document results)
   ```

### Debugging a Network Issue

1. **Check Implementation** (Context7):

   ```
   Context7: query-docs
   - libraryId: "/square/retrofit"
   - query: "debugging network requests and responses"
   ```

2. **Review Patterns** (Augments):

   ```
   Augments: get_framework_examples
   - framework: "retrofit"
   - pattern: "error-handling"
   ```

3. **Test and Debug** (mobile-dev + mcp-android):

   ```
   mobile-dev: android_logcat (view network logs)
   mcp-android: dump_hierarchy (check UI state)
   mcp-android: screenshot (capture error state)
   ```

### Testing a Complete User Flow

1. **Setup**:

   ```
   mcp-android: connect_device
   mcp-android: start_app
   - package_name: com.example.overseerr_client
   ```

2. **Navigate and Interact**:

   ```
   mcp-android: wait_for_element
   - selector: "Login"
   - selector_type: "text"
   
   mcp-android: click
   - selector: "Login"
   - selector_type: "text"
   
   mcp-android: send_text
   - text: "user@example.com"
   
   mcp-android: press_key
   - key: "enter"
   ```

3. **Verify and Document**:

   ```
   mcp-android: wait_for_element
   - selector: "Welcome"
   - selector_type: "text"
   
   mcp-android: screenshot
   - filename: ./screenshots/login_success.png
   ```

### Setting Up Material 3 Theming

1. **Learn Patterns** (Augments):

   ```
   Augments: get_framework_context
   - frameworks: ["jetpack-compose", "material-design"]
   - task_description: "implementing Material You dynamic theming"
   ```

2. **Get Specific Examples** (Context7 + Augments):

   ```
   Context7: query-docs
   - libraryId: "/androidx/compose-material3"
   - query: "dynamic color scheme implementation"
   
   Augments: get_framework_examples
   - framework: "material-design"
   - pattern: "dynamic-theming"
   ```

3. **Implement and Test** (Code + mcp-android):
   - Implement theme configuration
   - Test on Android 12+ device with mcp-android
   - Verify dynamic colors change with wallpaper

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Overseerr API Documentation](https://api-docs.overseerr.dev/)
- [uiautomator2 Documentation](https://github.com/openatx/uiautomator2)

## Key Libraries to Look Up with Context7

When implementing features, use Context7 to get documentation for these key libraries:

- **Networking**: `retrofit`, `okhttp`, `kotlinx-serialization`
- **Dependency Injection**: `hilt`, `dagger`
- **Database**: `room`, `datastore`
- **UI**: `jetpack-compose`, `compose-material3`, `compose-navigation`
- **Image Loading**: `coil`
- **Async**: `kotlin-coroutines`, `kotlin-flow`
- **Testing**: `junit5`, `mockk`, `kotest`
- **Security**: `androidx-biometric`, `androidx-security`
- **Pagination**: `paging3`

## Framework Topics to Explore with Augments

Use Augments to get comprehensive guides and patterns for:

- **Architecture**: MVVM, Clean Architecture, Repository Pattern
- **UI Patterns**: Compose state management, navigation, theming
- **Android Specifics**: Material Design 3, adaptive layouts, dynamic colors
- **Best Practices**: Kotlin idioms, coroutine patterns, error handling
- **Testing**: Unit testing, property-based testing, UI testing
- **Performance**: Memory management, image optimization, lazy loading

---

**Note**: This project uses Kotlin and Jetpack Compose for native Android development. The mobile-dev MCP server provides useful Android SDK and device management capabilities. The mcp-android server uses uiautomator2 for comprehensive device automation and UI testing.

**Pro Tip**: Combine all four MCP servers for maximum effectiveness:

1. Use **Context7** for official API documentation
2. Use **Augments** for framework patterns and best practices  
3. Use **mobile-dev** for building and environment management
4. Use **mcp-android** for device automation and UI testing with uiautomator2
