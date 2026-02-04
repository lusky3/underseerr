#!/bin/bash

# Function to safely check if container is running
container_is_running() {
    docker ps --filter "name=$1" --filter "status=running" --format '{{.Names}}' | grep -q "^$1$"
    return $?
}

echo "=========================================="
echo "Overseerr Test Environment Setup"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    echo "Please install Docker first: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    echo "Please install Docker Compose first"
    exit 1
fi

# Use docker compose v2 if available
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo -e "${GREEN}✓ Docker is installed${NC}"
echo ""

# Create necessary directories
echo "Setting up configuration directories..."

# Create base directories
mkdir -p plex-mock/html

# Setup Overseerr with template
if [ ! -d "overseerr-config" ]; then
    echo "  Creating Overseerr configuration from template..."
    mkdir -p overseerr-config/db
    cp config-templates/overseerr/settings.json overseerr-config/
    if [ -f "config-templates/overseerr/db.sqlite3" ]; then
        cp config-templates/overseerr/db.sqlite3 overseerr-config/db/
    fi
    echo -e "  ${GREEN}✓ Overseerr configured${NC}"
elif [ ! -f "overseerr-config/settings.json" ]; then
    echo -e "  ${YELLOW}⚠ Overseerr config incomplete, restoring from template...${NC}"
    mkdir -p overseerr-config/db
    cp config-templates/overseerr/settings.json overseerr-config/
    if [ -f "config-templates/overseerr/db.sqlite3" ]; then
        cp config-templates/overseerr/db.sqlite3 overseerr-config/db/
    fi
    echo -e "  ${GREEN}✓ Overseerr restored${NC}"
else
    echo -e "  ${GREEN}✓ Overseerr already configured${NC}"
fi

# Setup Jellyseerr with template
if [ ! -d "jellyseerr-config" ]; then
    echo "  Creating Jellyseerr configuration from template..."
    mkdir -p jellyseerr-config/db
    cp config-templates/jellyseerr/settings.json jellyseerr-config/
    if [ -f "config-templates/jellyseerr/db.sqlite3" ]; then
        cp config-templates/jellyseerr/db.sqlite3 jellyseerr-config/db/
    fi
    echo -e "  ${GREEN}✓ Jellyseerr configured${NC}"
elif [ ! -f "jellyseerr-config/settings.json" ]; then
    echo -e "  ${YELLOW}⚠ Jellyseerr config incomplete, restoring from template...${NC}"
    mkdir -p jellyseerr-config/db
    cp config-templates/jellyseerr/settings.json jellyseerr-config/
    if [ -f "config-templates/jellyseerr/db.sqlite3" ]; then
        cp config-templates/jellyseerr/db.sqlite3 jellyseerr-config/db/
    fi
    echo -e "  ${GREEN}✓ Jellyseerr restored${NC}"
else
    echo -e "  ${GREEN}✓ Jellyseerr already configured${NC}"
fi

# Setup Radarr with template
if [ ! -d "radarr-config" ]; then
    echo "  Creating Radarr configuration from template..."
    mkdir -p radarr-config
    cp config-templates/radarr/config.xml radarr-config/
    cp config-templates/radarr/radarr.db radarr-config/
    echo -e "  ${GREEN}✓ Radarr configured${NC}"
elif [ ! -f "radarr-config/config.xml" ] || [ ! -f "radarr-config/radarr.db" ]; then
    echo -e "  ${YELLOW}⚠ Radarr config incomplete, restoring from template...${NC}"
    cp config-templates/radarr/config.xml radarr-config/
    cp config-templates/radarr/radarr.db radarr-config/
    echo -e "  ${GREEN}✓ Radarr restored${NC}"
else
    echo -e "  ${GREEN}✓ Radarr already configured${NC}"
fi

# Setup Sonarr with template
if [ ! -d "sonarr-config" ]; then
    echo "  Creating Sonarr configuration from template..."
    mkdir -p sonarr-config
    cp config-templates/sonarr/config.xml sonarr-config/
    cp config-templates/sonarr/sonarr.db sonarr-config/
    echo -e "  ${GREEN}✓ Sonarr configured${NC}"
elif [ ! -f "sonarr-config/config.xml" ] || [ ! -f "sonarr-config/sonarr.db" ]; then
    echo -e "  ${YELLOW}⚠ Sonarr config incomplete, restoring from template...${NC}"
    cp config-templates/sonarr/config.xml sonarr-config/
    cp config-templates/sonarr/sonarr.db sonarr-config/
    echo -e "  ${GREEN}✓ Sonarr restored${NC}"
else
    echo -e "  ${GREEN}✓ Sonarr already configured${NC}"
fi

echo -e "${GREEN}✓ All configurations ready${NC}"
echo ""

# Stop any existing containers
echo "Stopping existing containers..."
$DOCKER_COMPOSE down 2>/dev/null || true

echo -e "${GREEN}✓ Cleaned up existing containers${NC}"
echo ""

# Start the services
echo "Starting Overseerr, Jellyseerr and mock services..."
$DOCKER_COMPOSE up -d

echo ""
echo -e "${GREEN}✓ Services started${NC}"
echo ""

# Function to check health status of a container
check_health() {
    local container="$1"
    docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none"
}

# Function to wait for a container to be healthy
wait_for_healthy() {
    local container=$1
    local display_name=$2
    local max_attempts=60  # 60 attempts = 2 minutes max
    local attempt=0
    
    echo -e "${BLUE}Waiting for $display_name to be ready...${NC}"
    
    # Give containers a moment to initialize
    sleep 1
    
    while [ $attempt -lt $max_attempts ]; do
        local health
        health=$(check_health "$container")
        
        # Debug output every 10 attempts
        if [ $((attempt % 10)) -eq 0 ] && [ $attempt -gt 0 ]; then
            echo -e "${YELLOW}  Debug: Container=$container, Health=[$health], Attempt=$attempt${NC}"
        fi
        
        case "$health" in
            "healthy")
                echo -e "${GREEN}✓ $display_name is healthy${NC}"
                return 0
                ;;
            "none")
                # Container doesn't have health check, check if it's running
                if container_is_running "$container"; then
                    echo -e "${GREEN}✓ $display_name is running${NC}"
                    return 0
                fi
                ;;
            "starting")
                if [ $((attempt % 5)) -eq 0 ]; then
                    echo -e "${YELLOW}  $display_name is starting... (attempt $((attempt+1))/$max_attempts)${NC}"
                fi
                ;;
            "unhealthy")
                echo -e "${RED}✗ $display_name is unhealthy${NC}"
                echo "Check logs with: docker compose logs $container"
                return 1
                ;;
            *)
                # Unknown status, treat as starting
                if [ $((attempt % 5)) -eq 0 ]; then
                    echo -e "${YELLOW}  $display_name status: [$health] (attempt $((attempt+1))/$max_attempts)${NC}"
                fi
                ;;
        esac
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}✗ $display_name failed to become healthy after $max_attempts attempts${NC}"
    echo "Last status: [$health]"
    return 1
}

# Wait for all services to be healthy
echo "Waiting for services to be ready..."
echo ""

# Plex Mock (fastest to start)
wait_for_healthy "plex-mock" "Plex Mock"

# Radarr and Sonarr (can start in parallel, but we check sequentially)
wait_for_healthy "radarr-mock" "Radarr"
wait_for_healthy "sonarr-mock" "Sonarr"

# Overseerr (depends on others)
wait_for_healthy "overseerr-test" "Overseerr"

# Jellyseerr (depends on others)
wait_for_healthy "jellyseerr-test" "Jellyseerr"

echo ""
echo -e "${GREEN}✓ All services are healthy!${NC}"
echo ""

# Check service status
echo "Service Status:"
echo "----------------------------------------"
$DOCKER_COMPOSE ps

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Services are now running:"
echo ""
echo -e "${GREEN}Overseerr:${NC}     http://localhost:5055"
echo -e "  ${YELLOW}⚠ Requires 2-minute setup (see QUICK_SETUP_GUIDE.md)${NC}"
echo -e "${GREEN}Jellyseerr:${NC}    http://localhost:5056"
echo -e "  ${YELLOW}⚠ Requires 2-minute setup (see QUICK_SETUP_GUIDE.md)${NC}"
echo ""
echo -e "${GREEN}Radarr:${NC}        http://localhost:7878"
echo -e "  ${BLUE}API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x${NC}"
echo -e "${GREEN}Sonarr:${NC}        http://localhost:8989"
echo -e "  ${BLUE}API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x${NC}"
echo -e "${GREEN}Plex Mock:${NC}     http://localhost:32400"
echo ""
echo -e "${BLUE}ℹ️  Radarr and Sonarr are pre-configured with databases and API keys!${NC}"
echo ""
echo "Next steps:"
echo "  1. Open http://localhost:5055 (Overseerr) or http://localhost:5056 (Jellyseerr) in your browser"
echo -e "  2. Follow steps"
echo -e "  3. Configure your Android app with ${GREEN}http://YOUR_IP:5055${NC} or ${GREEN}:5056${NC}"
echo ""
echo "Get your IP address:"
echo -e "  ${GREEN}hostname -I | awk '{print \$1}'${NC}"
echo ""
echo "Useful commands:"
echo "  View logs:    docker compose logs -f overseerr"
echo "  Stop:         docker compose down"
echo "  Restart:      docker compose restart"
echo "  Clean reset:  docker compose down && rm -rf overseerr-config jellyseerr-config radarr-config sonarr-config && ./setup-overseerr-test.sh"
echo "  Note: Clean reset will restore all services from templates with pre-configured settings"
echo ""
echo -e "${RED}=========================================="
echo "⚠️  SECURITY WARNING"
echo -e "==========================================${NC}"
echo -e "${YELLOW}All services are exposed on 0.0.0.0 (all network interfaces)${NC}"
echo -e "${YELLOW}This means they are accessible from ANY device on your network!${NC}"
echo ""
echo -e "${RED}DO NOT use this setup in production or on untrusted networks!${NC}"
echo -e "${RED}This is for DEVELOPMENT and TESTING purposes ONLY!${NC}"
echo ""
echo "Exposed ports:"
echo "  - 5055  (Overseerr)"
echo "  - 5056  (Jellyseerr)"
echo "  - 7878  (Radarr - API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x)"
echo "  - 8989  (Sonarr - API Key: 1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x1x)"
echo "  - 32400 (Plex Mock (nginx))"
echo ""
echo "To restrict access, edit docker/compose.yml and change:"
echo -e "  ${YELLOW}0.0.0.0:PORT:PORT${NC} to ${GREEN}127.0.0.1:PORT:PORT${NC}"
echo ""
