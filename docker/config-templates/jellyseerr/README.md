# Jellyseerr Configuration Template

This directory contains a pre-configured `settings.json` template for Jellyseerr that sets up a complete test environment. Jellyseerr is a fork of Overseerr with support for Jellyfin and Emby.

## What's Included

The template provides a fully configured Jellyseerr instance with:

### Plex Configuration (Used for Mock Auth)

- **Server Name**: Mock Plex Server
- **Host**: plex-mock (Docker network)
- **Port**: 32400
- **Libraries**:
  - Movies (ID: 1)
  - TV Shows (ID: 2)

### Radarr Configuration

- **Host**: radarr-mock
- **Port**: 7878
- **API Key**: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
- **Profile**: Any
- **Directory**: /tmp
- **Search Prevention**: Enabled (for testing)

### Sonarr Configuration

- **Host**: sonarr-mock
- **Port**: 8989
- **API Key**: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
- **Profile**: Any
- **Directory**: /tmp
- **Search Prevention**: Enabled (for testing)

### Application Settings

- **Title**: Jellyseerr (Test Environment)
- **Local Login**: Enabled
- **Plex Login**: Enabled
- **CSRF Protection**: Disabled (for easier testing)
- **Image Caching**: Disabled (saves disk space)
- **Default Permissions**: 32 (Request)

### Scan Schedules

- **Plex Recently Added**: Every 5 minutes
- **Plex Full Scan**: Every hour
- **Plex Watchlist Sync**: Every minute
- **Radarr/Sonarr Sync**: Every hour
- **Download Sync**: Every minute

## Security Notes

⚠️ **This configuration is for TESTING ONLY!**

The template includes:

- Dummy VAPID keys (for web push notifications)
- Generic API keys (1x1x1x...)
- Disabled CSRF protection
- All notifications disabled

**DO NOT use this configuration in production!**

## Usage

The setup script (`setup-overseerr-test.sh`) automatically:

1. Creates the `jellyseerr-config` directory if it doesn't exist
2. Copies `settings.json` from this template
3. Lets Jellyseerr create its own database on first run

## First Run

When Jellyseerr starts for the first time:

1. It reads the `settings.json` configuration
2. Creates a new SQLite database (`db/db.sqlite3`)
3. Waits for the first user to sign in
4. Creates that user as the admin account

## Resetting Configuration

To reset Jellyseerr to the template configuration:

```bash
cd docker
docker compose down
rm -rf jellyseerr-config
./setup-overseerr-test.sh
```

This will:

- Remove all Jellyseerr data (including users and requests)
- Restore the template configuration
- Start fresh with a new database

## Customization

To customize the template:

1. Edit `config-templates/jellyseerr/settings.json`
2. Run the setup script to apply changes to new instances
3. Existing instances won't be affected (they keep their current config)

## Files Created

When Jellyseerr runs, it creates:

- `jellyseerr-config/settings.json` - Configuration (from template)
- `jellyseerr-config/db/db.sqlite3` - Database (auto-created)
- `jellyseerr-config/logs/` - Application logs

## Template Fields

### Sensitive Fields (Dummy Values)

- `clientId`: 00000000-0000-0000-0000-000000000000
- `vapidPrivate`: AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
- `vapidPublic`: BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB

These are placeholder values that maintain the correct format but contain no real secrets.

### API Keys

- Jellyseerr API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
- Radarr API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
- Sonarr API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x

These match the pre-configured API keys in the Radarr and Sonarr templates.

## Integration with Mock Services

The template is designed to work with:

- **plex-mock**: Nginx-based Plex API mock
- **radarr-mock**: Real Radarr instance with pre-configured database
- **sonarr-mock**: Real Sonarr instance with pre-configured database

All services are connected via the `overseerr-network` Docker network.

## Troubleshooting

### Jellyseerr won't start

- Check logs: `docker compose logs jellyseerr`
- Verify settings.json is valid JSON
- Ensure no port conflicts on 5056

### Can't connect to Plex

- Verify plex-mock is running: `docker compose ps plex-mock`
- Test connection: `curl http://localhost:32400/`
- Check Jellyseerr logs for connection errors

### Radarr/Sonarr not working

- Verify API keys match between services
- Check that radarr-mock and sonarr-mock are healthy
- Test API: `curl http://localhost:7878/api/v3/system/status?apikey=1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x`

## References

- [Jellyseerr Documentation](https://docs.jellyseerr.dev/)
- [Jellyseerr GitHub](https://github.com/fallenbagel/jellyseerr)
