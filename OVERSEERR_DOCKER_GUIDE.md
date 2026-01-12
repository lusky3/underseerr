# Overseerr Docker Test Environment

Complete Dockerized Overseerr setup for testing the Android app with all required services.

## Quick Start

### 1. Start the Environment

```bash
./setup-overseerr-test.sh
```

This will start:
- **Overseerr** on port 5055
- **Radarr** (movie management) on port 7878
- **Sonarr** (TV show management) on port 8989
- **Plex Mock** (authentication) on port 32400

### 2. Access Overseerr

Open your browser: http://localhost:5055

### 3. Initial Setup Wizard

Follow the setup wizard to configure Overseerr.

## Detailed Setup Instructions

### Step 1: Start Services

```bash
# Make script executable
chmod +x setup-overseerr-test.sh

# Run setup
./setup-overseerr-test.sh
```

### Step 2: Configure Overseerr

1. **Open Overseerr**: http://localhost:5055

2. **Sign in with Plex**:
   - Click "Sign in with Plex"
   - Use any credentials (mock server accepts all)
   - Or skip Plex and use local authentication

3. **Configure Services**:
   
   **Radarr (Movies)**:
   - URL: `http://radarr-mock:7878`
   - API Key: Get from http://localhost:7878/settings/general
   - Quality Profile: Any
   - Root Folder: `/movies`

   **Sonarr (TV Shows)**:
   - URL: `http://sonarr-mock:8989`
   - API Key: Get from http://localhost:8989/settings/general
   - Quality Profile: Any
   - Root Folder: `/tv`

4. **Complete Setup**

### Step 3: Configure Android App

Update your Android app to connect to Overseerr:

```kotlin
// Use your machine's IP address, not localhost
val overseerrUrl = "http://YOUR_IP_ADDRESS:5055"
```

**Find your IP address**:
```bash
# Linux/Mac
hostname -I | awk '{print $1}'

# Or
ip addr show | grep "inet " | grep -v 127.0.0.1
```

## Service URLs

### From Host Machine
- Overseerr: http://localhost:5055
- Radarr: http://localhost:7878
- Sonarr: http://localhost:8989
- Plex Mock: http://localhost:32400

### From Android Device/Emulator
- Overseerr: http://YOUR_IP:5055
- Example: http://192.168.1.100:5055

### From Docker Network (Internal)
- Overseerr: http://overseerr-test:5055
- Radarr: http://radarr-mock:7878
- Sonarr: http://sonarr-mock:8989
- Plex: http://plex-mock:32400

## Docker Commands

### Start Services
```bash
docker-compose up -d
```

### Stop Services
```bash
docker-compose down
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f overseerr
docker-compose logs -f radarr-mock
docker-compose logs -f sonarr-mock
```

### Restart Services
```bash
docker-compose restart
```

### Check Status
```bash
docker-compose ps
```

### Remove Everything (including data)
```bash
docker-compose down -v
rm -rf overseerr-config radarr-config sonarr-config
```

## Configuration Files

### docker-compose.yml
Main configuration file defining all services.

### Volumes (Data Persistence)
- `./overseerr-config` - Overseerr data
- `./radarr-config` - Radarr data
- `./sonarr-config` - Sonarr data

## Testing the Android App

### 1. Get Your IP Address
```bash
hostname -I | awk '{print $1}'
```

### 2. Update App Configuration
In your Android app, use: `http://YOUR_IP:5055`

### 3. Test Features

**Authentication**:
- Sign in with Plex (mock server)
- Or use local authentication

**Discovery**:
- Browse trending movies/TV shows
- Search for media
- View details

**Requests**:
- Request movies
- Request TV shows
- View request status

**Profile**:
- View user profile
- Check quota
- View statistics

## Troubleshooting

### Services Won't Start

**Check Docker**:
```bash
docker --version
docker-compose --version
```

**Check Ports**:
```bash
# Make sure ports are not in use
netstat -tuln | grep -E '5055|7878|8989|32400'
```

**View Logs**:
```bash
docker-compose logs
```

### Can't Connect from Android

**Check Firewall**:
```bash
# Allow port 5055
sudo ufw allow 5055
```

**Verify IP Address**:
```bash
# Make sure you're using the correct IP
ip addr show
```

**Test Connection**:
```bash
# From another terminal
curl http://localhost:5055/api/v1/status
```

### Overseerr Not Responding

**Restart Service**:
```bash
docker-compose restart overseerr
```

**Check Logs**:
```bash
docker-compose logs -f overseerr
```

**Reset Configuration**:
```bash
docker-compose down
rm -rf overseerr-config
docker-compose up -d
```

### Radarr/Sonarr Connection Issues

**Get API Keys**:
1. Open http://localhost:7878 (Radarr)
2. Go to Settings → General
3. Copy API Key
4. Use in Overseerr configuration

**Test Connection**:
```bash
# Test Radarr
curl http://localhost:7878/api/v3/system/status

# Test Sonarr
curl http://localhost:8989/api/v3/system/status
```

## Advanced Configuration

### Custom Ports

Edit `docker-compose.yml`:
```yaml
ports:
  - "8080:5055"  # Use port 8080 instead of 5055
```

### Persistent Data

Data is stored in:
- `./overseerr-config`
- `./radarr-config`
- `./sonarr-config`

Backup these directories to preserve your configuration.

### Environment Variables

Edit `docker-compose.yml`:
```yaml
environment:
  - PUID=1000          # User ID
  - PGID=1000          # Group ID
  - TZ=America/New_York # Timezone
```

## API Testing

### Test Overseerr API

```bash
# Get server status
curl http://localhost:5055/api/v1/status

# Get trending (requires authentication)
curl -H "X-Api-Key: YOUR_API_KEY" \
  http://localhost:5055/api/v1/discover/trending

# Search
curl -H "X-Api-Key: YOUR_API_KEY" \
  "http://localhost:5055/api/v1/search?query=inception"
```

### Get API Key

1. Open Overseerr: http://localhost:5055
2. Go to Settings → General
3. Copy API Key
4. Use in requests

## Integration with Android App

### Update Base URL

In your app's network configuration:

```kotlin
// Debug build
buildConfigField("String", "OVERSEERR_URL", "\"http://192.168.1.100:5055\"")

// Or use local.properties
overseerr.url=http://192.168.1.100:5055
```

### Test Connection

```kotlin
// In your app
val client = OkHttpClient()
val request = Request.Builder()
    .url("http://YOUR_IP:5055/api/v1/status")
    .build()

client.newCall(request).execute().use { response ->
    println("Status: ${response.code}")
    println("Body: ${response.body?.string()}")
}
```

## Production Considerations

### Security

For production use:
1. Enable HTTPS
2. Use strong passwords
3. Configure proper authentication
4. Set up reverse proxy (nginx/traefik)
5. Use real Plex server
6. Configure firewall rules

### Performance

1. Allocate sufficient resources
2. Use SSD for storage
3. Configure caching
4. Monitor resource usage

### Backup

```bash
# Backup configuration
tar -czf overseerr-backup.tar.gz \
  overseerr-config \
  radarr-config \
  sonarr-config

# Restore
tar -xzf overseerr-backup.tar.gz
```

## Resources

- [Overseerr Documentation](https://docs.overseerr.dev/)
- [Radarr Documentation](https://wiki.servarr.com/radarr)
- [Sonarr Documentation](https://wiki.servarr.com/sonarr)
- [Docker Documentation](https://docs.docker.com/)

## Support

For issues:
1. Check logs: `docker-compose logs`
2. Verify configuration
3. Test connectivity
4. Check firewall settings
5. Review documentation

## Summary

You now have a complete Overseerr test environment running in Docker with:
- ✅ Overseerr server
- ✅ Radarr (movies)
- ✅ Sonarr (TV shows)
- ✅ Plex mock server
- ✅ Network isolation
- ✅ Data persistence

Use this environment to test your Android app with a real Overseerr instance!
