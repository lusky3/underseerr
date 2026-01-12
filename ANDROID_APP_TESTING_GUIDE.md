# Android App Testing with Docker Overseerr

Complete guide for testing your Overseerr Android app with the Docker environment.

## Prerequisites

✅ Docker Overseerr environment running (see OVERSEERR_SETUP_COMPLETE.md)  
✅ Android device or emulator  
✅ App built and ready to install  

## Quick Setup

### 1. Start Overseerr (if not running)

```bash
docker compose up -d
```

### 2. Get Your IP Address

```bash
hostname -I | awk '{print $1}'
```

**Your IP**: `172.29.125.229`

### 3. Configure App

Use server URL: `http://172.29.125.229:5055`

## Testing Workflow

### Phase 1: Installation & First Launch

**Test Case 1.1: Install App**
```bash
# Install APK on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use Android Studio
# Run → Run 'app'
```

**Expected**: App installs successfully

**Test Case 1.2: First Launch**
1. Launch app
2. Observe splash screen
3. See server configuration screen

**Expected**: Smooth launch, no crashes

### Phase 2: Authentication

**Test Case 2.1: Server Configuration**
1. Enter server URL: `http://172.29.125.229:5055`
2. Click "Connect" or "Next"

**Expected**: Connection successful, server detected

**Test Case 2.2: Plex Authentication**
1. Click "Sign in with Plex"
2. Complete OAuth flow
3. Grant permissions

**Expected**: Successfully authenticated, redirected to home

**Test Case 2.3: Local Authentication** (Alternative)
1. Choose "Local Sign In"
2. Enter credentials from Overseerr setup
3. Sign in

**Expected**: Successfully authenticated

### Phase 3: Discovery & Browse

**Test Case 3.1: View Trending**
1. Navigate to "Discover" or "Home"
2. View trending movies
3. Scroll through list

**Expected**: 
- Movies load quickly
- Images display correctly
- Smooth scrolling
- Pagination works

**Test Case 3.2: Search**
1. Tap search icon
2. Enter "Inception"
3. View results

**Expected**:
- Search is responsive
- Results appear quickly
- Relevant matches shown

**Test Case 3.3: View Details**
1. Tap on a movie
2. View details screen
3. Check all information

**Expected**:
- Details load quickly
- Images display
- All metadata shown
- Availability status correct

### Phase 4: Request Management

**Test Case 4.1: Submit Movie Request**
1. Find unavailable movie
2. Tap "Request"
3. Select quality profile
4. Select root folder
5. Submit request

**Expected**:
- Form displays correctly
- Options load from Radarr
- Submission successful
- Confirmation shown

**Test Case 4.2: View Requests**
1. Navigate to "Requests"
2. View request list
3. Check status

**Expected**:
- Requests display correctly
- Status is accurate
- Can filter/sort

**Test Case 4.3: Request Details**
1. Tap on a request
2. View details

**Expected**:
- All information shown
- Status updates visible
- Can cancel if permitted

### Phase 5: Profile & Settings

**Test Case 5.1: View Profile**
1. Navigate to profile
2. View user information
3. Check quota

**Expected**:
- Profile loads correctly
- Quota information accurate
- Statistics displayed

**Test Case 5.2: Change Settings**
1. Open settings
2. Change theme (Light/Dark/System)
3. Toggle notifications
4. Adjust preferences

**Expected**:
- Settings save correctly
- Theme changes immediately
- Preferences persist

### Phase 6: Offline Mode

**Test Case 6.1: Cache Content**
1. Browse media while online
2. View several items
3. Enable airplane mode

**Expected**:
- Previously viewed content accessible
- Cached images display
- Graceful offline indicator

**Test Case 6.2: Sync on Reconnect**
1. Disable airplane mode
2. Wait for sync

**Expected**:
- Automatic sync occurs
- New content loads
- No data loss

### Phase 7: Edge Cases

**Test Case 7.1: Network Errors**
1. Disconnect from network
2. Try to load new content
3. Reconnect

**Expected**:
- Error message shown
- Retry option available
- Recovers gracefully

**Test Case 7.2: Invalid Server**
1. Enter invalid server URL
2. Try to connect

**Expected**:
- Clear error message
- Can retry with correct URL

**Test Case 7.3: Session Expiry**
1. Wait for session to expire
2. Try to make request

**Expected**:
- Prompted to re-authenticate
- Session refreshes automatically

## Performance Testing

### Test Case P.1: App Launch Time
```bash
# Measure cold start
adb shell am force-stop com.example.overseerr_client
adb shell am start -W com.example.overseerr_client/.MainActivity
```

**Target**: < 2 seconds

### Test Case P.2: Memory Usage
```bash
# Monitor memory
adb shell dumpsys meminfo com.example.overseerr_client
```

**Target**: < 200 MB normal usage

### Test Case P.3: Network Usage
```bash
# Monitor network
adb shell dumpsys netstats | grep overseerr
```

**Target**: Reasonable data usage, efficient caching

## Automated Testing

### Using mcp-android Tools

**Connect to Device**:
```bash
# Will use the MCP Android tools
```

**Take Screenshots**:
```bash
# Capture key screens for documentation
```

**Test UI Flows**:
```bash
# Automate common user flows
```

## Test Data

### Sample Searches
- "Inception"
- "The Matrix"
- "Breaking Bad"
- "Game of Thrones"
- "Stranger Things"

### Test Scenarios
1. Request popular movie
2. Request TV show with multiple seasons
3. Search for non-existent media
4. Browse different categories
5. Check quota limits

## Verification Checklist

### Functionality
- [ ] Authentication works
- [ ] Can browse media
- [ ] Search returns results
- [ ] Can view details
- [ ] Can submit requests
- [ ] Requests appear in list
- [ ] Profile displays correctly
- [ ] Settings save properly
- [ ] Offline mode works
- [ ] Notifications work

### UI/UX
- [ ] Material 3 design
- [ ] Smooth animations
- [ ] Responsive layout
- [ ] Proper error messages
- [ ] Loading indicators
- [ ] Empty states
- [ ] Dark/Light themes
- [ ] Accessibility labels

### Performance
- [ ] Fast app launch
- [ ] Smooth scrolling
- [ ] Quick image loading
- [ ] Responsive search
- [ ] Efficient caching
- [ ] Low memory usage
- [ ] Reasonable battery usage

### Compatibility
- [ ] Works on phone
- [ ] Works on tablet
- [ ] Portrait orientation
- [ ] Landscape orientation
- [ ] Different Android versions
- [ ] Different screen sizes

## Debugging

### View App Logs
```bash
# Filter app logs
adb logcat | grep overseerr

# Or specific tag
adb logcat -s OverseerrClient
```

### View Network Traffic
```bash
# Use Charles Proxy or similar
# Configure proxy on device
# Monitor API calls
```

### Check Database
```bash
# Pull database from device
adb pull /data/data/com.example.overseerr_client/databases/overseerr.db

# Inspect with SQLite browser
```

## Common Issues

### Can't Connect to Server

**Problem**: App can't reach Overseerr

**Solutions**:
1. Verify IP address: `hostname -I`
2. Check firewall: `sudo ufw allow 5055`
3. Test from device: Open browser, go to `http://172.29.125.229:5055`
4. Verify services running: `docker compose ps`

### Authentication Fails

**Problem**: Can't sign in

**Solutions**:
1. Check Overseerr is configured
2. Verify credentials
3. Check API key if using direct auth
4. View Overseerr logs: `docker compose logs -f overseerr`

### Images Not Loading

**Problem**: Poster images don't display

**Solutions**:
1. Check network connection
2. Verify image URLs in API response
3. Check Coil configuration
4. Test image URL in browser

### Requests Not Submitting

**Problem**: Can't submit requests

**Solutions**:
1. Verify Radarr/Sonarr configured in Overseerr
2. Check quality profiles available
3. Check root folders configured
4. View API response for errors

## Test Report Template

```markdown
# Test Report - [Date]

## Environment
- Device: [Model]
- Android Version: [Version]
- App Version: [Version]
- Overseerr Version: 1.34.0

## Test Results

### Authentication: ✅ PASS / ❌ FAIL
- Notes: [Details]

### Discovery: ✅ PASS / ❌ FAIL
- Notes: [Details]

### Requests: ✅ PASS / ❌ FAIL
- Notes: [Details]

### Profile: ✅ PASS / ❌ FAIL
- Notes: [Details]

### Performance: ✅ PASS / ❌ FAIL
- Launch Time: [X] seconds
- Memory Usage: [X] MB
- Notes: [Details]

## Issues Found
1. [Issue description]
2. [Issue description]

## Recommendations
1. [Recommendation]
2. [Recommendation]
```

## Next Steps

After testing:

1. **Document Issues**: Create issue tickets for bugs
2. **Performance Optimization**: Address any performance issues
3. **UI Polish**: Refine UI based on testing
4. **Beta Testing**: Release to beta testers
5. **Production Release**: Prepare for Play Store

## Resources

- [Overseerr API Docs](https://api-docs.overseerr.dev/)
- [Android Testing Guide](https://developer.android.com/training/testing)
- [Material Design Guidelines](https://m3.material.io/)

## Summary

With the Docker Overseerr environment, you can:

✅ Test all app features with real API  
✅ Verify authentication flows  
✅ Test request submission  
✅ Validate offline mode  
✅ Check performance  
✅ Test on multiple devices  

**Ready to test!** 🚀
