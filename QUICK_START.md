# Overseerr Docker - Quick Start

## 1. Start Services (30 seconds)

```bash
./setup-overseerr-test.sh
```

## 2. Open Overseerr

http://localhost:5055

## 3. Complete Setup Wizard

- Sign in with Plex (or skip)
- Configure Radarr: http://radarr-mock:7878
- Configure Sonarr: http://sonarr-mock:8989
- Finish setup

## 4. Get Your IP Address

```bash
hostname -I | awk '{print $1}'
```

## 5. Configure Android App

Use: `http://YOUR_IP:5055`

Example: `http://192.168.1.100:5055`

## 6. Test the App!

- Sign in
- Browse media
- Submit requests
- Test all features

## Common Commands

```bash
# View logs
docker-compose logs -f overseerr

# Restart
docker-compose restart

# Stop
docker-compose down

# Check status
docker-compose ps
```

## Troubleshooting

**Can't connect from Android?**
- Check firewall: `sudo ufw allow 5055`
- Verify IP address
- Make sure services are running

**Need to reset?**
```bash
docker-compose down
rm -rf overseerr-config radarr-config sonarr-config
docker-compose up -d
```

## URLs

- Overseerr: http://localhost:5055
- Radarr: http://localhost:7878
- Sonarr: http://localhost:8989

That's it! 🚀
