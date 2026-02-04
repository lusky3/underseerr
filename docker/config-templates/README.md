# Configuration Templates

This directory contains template configuration files for Radarr and Sonarr that are used to initialize the test environment.

## Structure

```text
config-templates/
├── overseerr/
│   ├── settings.json # Overseerr configuration with Plex/Radarr/Sonarr
│   └── README.md     # Detailed Overseerr template documentation
├── jellyseerr/
│   ├── settings.json # Jellyseerr configuration with Plex/Radarr/Sonarr
│   └── README.md     # Detailed Jellyseerr template documentation
├── radarr/
│   ├── config.xml    # Radarr configuration with API key
│   ├── radarr.db     # Pre-configured Radarr database
│   └── README.md     # Detailed Radarr template documentation
└── sonarr/
    ├── config.xml    # Sonarr configuration with API key
    ├── sonarr.db     # Pre-configured Sonarr database
    └── README.md     # Detailed Sonarr template documentation
```

Each service has its own README with detailed information about:

- Configuration details
- API access and examples
- Integration with other services
- Troubleshooting guides
- Customization instructions

## Purpose

These templates are copied to the runtime directories (`overseerr-config/`, `jellyseerr-config/`, `radarr-config/`, and `sonarr-config/`) when you run `setup-overseerr-test.sh`. This approach:

1. **Keeps the repo clean** - Runtime files (logs, PIDs, temp files) are not committed
2. **Provides consistent setup** - Everyone gets the same starting configuration
3. **Easy reset** - Delete runtime directories and re-run setup to start fresh
4. **Version controlled** - Only essential config files are tracked in git

## Files Included

### Overseerr

**settings.json** - Complete Overseerr configuration including:

- Plex server connection (plex-mock)
- Radarr integration with API key
- Sonarr integration with API key
- Scan schedules optimized for testing
- Dummy VAPID keys for web push notifications
- Application settings (title, permissions, etc.)

See `overseerr/README.md` for detailed information.

### Jellyseerr

**settings.json** - Complete Jellyseerr configuration including:

- Plex server connection (plex-mock)
- Radarr integration with API key
- Sonarr integration with API key
- Scan schedules optimized for testing
- Dummy VAPID keys for web push notifications
- Application settings (title, permissions, etc.)

See `jellyseerr/README.md` for detailed information.

### Radarr & Sonarr

**config.xml** - Service configuration including:

- API key: `1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x`
- Port settings
- Authentication settings
- Instance name

### Database Files

Pre-configured SQLite databases (Radarr/Sonarr) with:

- System settings
- Quality profiles
- Initial schema

## Usage

The setup script automatically copies these templates when needed:

```bash
cd docker
./setup-overseerr-test.sh
```

The script will:

1. Check if runtime directories exist
2. Copy templates if directories are missing or incomplete
3. Preserve existing configurations if already set up

## Resetting Configuration

To reset to template defaults:

```bash
cd docker
docker compose down
rm -rf overseerr-config jellyseerr-config radarr-config sonarr-config
./setup-overseerr-test.sh
```

## Modifying Templates

If you need to update the template configuration:

**For Overseerr:**

1. Make changes via the Overseerr web UI
2. Copy the updated settings back:

   ```bash
   cp overseerr-config/settings.json config-templates/overseerr/
   ```

3. Sanitize any sensitive data (tokens, keys, etc.)
4. Commit the template changes to git

**For Radarr/Sonarr:**

1. Make changes to the running service (e.g., via web UI)
2. Copy the updated files back to templates:

   ```bash
   cp radarr-config/config.xml config-templates/radarr/
   cp radarr-config/radarr.db config-templates/radarr/
   ```

3. Commit the template changes to git

## What's NOT Included

The following files are generated at runtime and should NOT be committed:

- `*.pid` - Process ID files
- `*.log` - Log files
- `logs/` - Log directories
- `logs.db*` - Log databases
- `*.db-shm`, `*.db-wal` - SQLite temporary files
- `asp/` - ASP.NET Core data protection keys
- `Sentry/` - Crash reporting data

These are automatically excluded via `.gitignore`.

## API Key

Both services use the same API key for simplicity:

```text
1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x
```

⚠️ **This is a test key only!** Never use this in production.

## Database Schema

The databases are pre-initialized with the necessary schema for Radarr v5.x and Sonarr v4.x. If you need to update to a newer version:

1. Let the service upgrade the database
2. Test thoroughly
3. Copy the upgraded database back to templates
4. Document the version change

## Documentation

Each service template includes detailed documentation:

- **[Overseerr Template Guide](overseerr/README.md)** - Complete Overseerr configuration reference
  - Pre-configured integrations
  - Scan schedules
  - Security notes
  - API examples

- **[Radarr Template Guide](radarr/README.md)** - Complete Radarr configuration reference
  - Quality profiles
  - API access examples
  - Integration testing
  - Troubleshooting

- **[Sonarr Template Guide](sonarr/README.md)** - Complete Sonarr configuration reference
  - Language profiles
  - Season management
  - API access examples
  - Integration testing

These guides provide comprehensive information about each service's configuration, usage, and integration with the test environment.

## Troubleshooting

### Templates Not Found

If you see "config-templates directory not found":

```bash
# Make sure you're in the docker directory
cd docker
ls config-templates/
```

### Permission Issues

If you get permission errors:

```bash
sudo chown -R $USER:$USER config-templates/
chmod -R 755 config-templates/
```

### Corrupted Database

If a database becomes corrupted:

```bash
cd docker
docker compose down
rm radarr-config/radarr.db sonarr-config/sonarr.db
cp config-templates/radarr/radarr.db radarr-config/
cp config-templates/sonarr/sonarr.db sonarr-config/
docker compose up -d
```
