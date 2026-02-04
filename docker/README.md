# Overseerr Docker Test Environment

Complete Dockerized Overseerr setup for testing the Android app.

## 📚 Documentation

**New here?** Check the [Documentation Index](DOCUMENTATION_INDEX.md) for a complete guide to all available documentation.

**Quick links:**

- [Configuration Templates](config-templates/README.md) - Pre-configured service templates for Overseerr and Jellyseerr
- [Plex Mock API](plex-mock/API_REFERENCE.md) - Complete API reference
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues and solutions

## 🚀 Quick Start (2 minutes)

```bash
cd docker
./setup-overseerr-test.sh
```

Then follow the [Quick Setup Guide](QUICK_SETUP_GUIDE.md) for 2-minute configuration.

## 📁 Directory Structure

```text
docker/
├── compose.yml                 # Docker Compose configuration
├── setup-overseerr-test.sh     # Setup script
├── config-templates/           # Template configurations (committed to git)
│   ├── radarr/                 # Radarr templates
│   │   ├── config.xml          # Pre-configured settings
│   │   └── radarr.db           # Pre-configured database
│   └── sonarr/                 # Sonarr templates
│       ├── config.xml          # Pre-configured settings
│       └── sonarr.db           # Pre-configured database
│   └── jellyseerr/             # Jellyseerr templates
│       └── settings.json       # Pre-configured settings
├── plex-mock/                  # Mock Plex server
│   ├── nginx.conf              # Nginx configuration
│   ├── html/                   # Static files
│   └── API_REFERENCE.md        # Plex API documentation
├── overseerr-config/           # Runtime data for Overseerr (not in git)
├── jellyseerr-config/          # Runtime data for Jellyseerr (not in git)
├── radarr-config/              # Runtime data for Radarr (not in git)
├── sonarr-config/              # Runtime data for Sonarr (not in git)

## 🎯 What's Included

### Services

- **Overseerr** (port 5055) - Main API server (based on Overseerr)
- **Jellyseerr** (port 5056) - Main API server (fork of Overseerr for Jellyfin)
- **Radarr** (port 7878) - Movie management (pre-configured with API key)
- **Sonarr** (port 8989) - TV show management (pre-configured with API key)
- **Plex Mock** (port 32400) - Authentication server (comprehensive API mock)

### Default Credentials

| Service | URL | Username / Email | Password |
|---------|-----|------------------|----------|
| **Overseerr** | http://localhost:5055 | `admin@overseerr.local` | `admin123` |
| **Jellyseerr** | http://localhost:5056 | `admin@overseerr.local` | `admin123` |
| **Plex Mock** | http://localhost:32400 | N/A (Mocked) | N/A |
| **Radarr** | http://localhost:7878 | N/A (API Only) | API Key: `1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x` |
| **Sonarr** | http://localhost:8989 | N/A (API Only) | API Key: `1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x` |

> **Note**: Both Overseerr and Jellyseerr are pre-configured to point to the mock Radarr, Sonarr, and Plex services.

### Features

- ✅ One-command startup
- ✅ All services networked together
- ✅ Health checks configured
- ✅ Data persistence
- ✅ **Pre-configured Radarr & Sonarr** with databases and API keys
- ✅ **Comprehensive Plex API mock** with 50+ endpoints
- ✅ Clear documentation

### Pre-Configured API Keys

Both Radarr and Sonarr come pre-configured with the same API key:

```text
1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
```

No manual configuration needed - services are ready to use immediately!

### Clean Repository

The setup uses a template-based approach:

- ✅ Only essential config files are committed (`config-templates/`)
- ✅ Runtime directories are created from templates
- ✅ Logs, PIDs, and temp files stay out of git
- ✅ Easy reset: just delete runtime directories and re-run setup

## 📖 Documentation

- **[plex-mock/API_REFERENCE.md](plex-mock/API_REFERENCE.md)** - Complete Plex API documentation

## 🔧 Usage

### First Time Setup (2 minutes)

1. **Start services**:

   ```bash
   ./setup-overseerr-test.sh
   ```

2. **Configure Overseerr**:

   - Open <http://localhost:5055> (Overseerr) or <http://localhost:5056> (Jellyseerr)
   - Follow instructions

3. **Done!** Ready for Android app testing

### Subsequent Starts (Instant)

```bash
docker compose up -d
```

Configuration is preserved in `overseerr-config/`, `jellyseerr-config/`, `radarr-config/`, and `sonarr-config/`.

## 📱 For Android App

### Get Your IP

```bash
hostname -I | awk '{print $1}'
```

### Configure App

- Server URL: `http://YOUR_IP:5055` (Overseerr) or `http://YOUR_IP:5056` (Jellyseerr)
- Username: `admin@overseerr.local`
- Password: `admin123`

## 🔑 Default Credentials

After setup:

- **Username**: `admin@overseerr.local` (or your synced account)
- **Password**: `admin123`
- **API Key**: Available in Settings → General

## 🛠️ Commands

```bash
# Start services
docker compose up -d

# Stop services
docker compose down

# View logs
docker compose logs -f overseerr

# Restart services
docker compose restart

# Check status
docker compose ps

# Reset everything
docker compose down
rm -rf overseerr-config jellyseerr-config radarr-config sonarr-config
./setup-overseerr-test.sh
```

## 🔄 Backup & Restore

### Backup Configuration

```bash
tar -czf overseerr-backup.tar.gz overseerr-config jellyseerr-config radarr-config sonarr-config
```

### Restore Configuration

```bash
tar -xzf overseerr-backup.tar.gz
docker compose up -d
```

## 🐛 Troubleshooting

### Services won't start

```bash
docker compose logs
```

### Can't connect from Android

```bash
# Check firewall
sudo ufw allow 5055
sudo ufw allow 5056

# Verify IP
hostname -I
```

### Overseerr shows setup wizard again

Your configuration was reset. Either:

1. Complete the 2-minute setup again
2. Restore from backup

### Port already in use

```bash
# Check what's using the port
sudo lsof -i :5055
sudo lsof -i :5056

# Or change the port in compose.yml
```

## 📊 Resource Usage

- **CPU**: < 5% idle
- **Memory**: ~800 MB total
- **Disk**: ~2 GB
- **Startup**: 30-40 seconds

## 🔐 Security Note

**This is a TEST environment!**

For production:

- Change all passwords
- Enable HTTPS
- Use real Plex server
- Configure proper authentication
- Set up firewall rules

## 🎓 Learn More

- [Overseerr Documentation](https://docs.overseerr.dev/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Radarr Wiki](https://wiki.servarr.com/radarr)
- [Sonarr Wiki](https://wiki.servarr.com/sonarr)

## ✨ Summary

This Docker environment provides:

✅ **Fast Setup** - 2 minutes to fully configured  
✅ **Complete Integration** - All services connected  
✅ **Data Persistence** - Configuration survives restarts  
✅ **Clear Documentation** - Step-by-step guides  
✅ **Easy Reset** - One command to start fresh  

Perfect for Android app development and testing!
