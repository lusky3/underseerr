#!/bin/bash

set -e

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
echo "Creating configuration directories..."
mkdir -p overseerr-config
mkdir -p radarr-config
mkdir -p sonarr-config
mkdir -p plex-mock/html

echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Stop any existing containers
echo "Stopping existing containers..."
$DOCKER_COMPOSE down 2>/dev/null || true

echo -e "${GREEN}✓ Cleaned up existing containers${NC}"
echo ""

# Start the services
echo "Starting Overseerr and mock services..."
$DOCKER_COMPOSE up -d

echo ""
echo -e "${GREEN}✓ Services started${NC}"
echo ""

# Function to check health status of a container
check_health() {
    local container=$1
    local status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")
    echo "$status"
}

# Function to wait for a container to be healthy
wait_for_healthy() {
    local container=$1
    local display_name=$2
    local max_attempts=60  # 60 attempts = 2 minutes max
    local attempt=0
    
    echo -e "${BLUE}Waiting for $display_name to be ready...${NC}"
    
    while [ $attempt -lt $max_attempts ]; do
        local health=$(check_health "$container")
        
        case "$health" in
            "healthy")
                echo -e "${GREEN}✓ $display_name is healthy${NC}"
                return 0
                ;;
            "none")
                # Container doesn't have health check, check if it's running
                if docker ps --filter "name=$container" --filter "status=running" | grep -q "$container"; then
                    echo -e "${GREEN}✓ $display_name is running${NC}"
                    return 0
                fi
                ;;
            "starting")
                echo -ne "${YELLOW}  $display_name is starting... (attempt $((attempt+1))/$max_attempts)\r${NC}"
                ;;
            "unhealthy")
                echo -e "${RED}✗ $display_name is unhealthy${NC}"
                echo "Check logs with: docker compose logs $container"
                return 1
                ;;
        esac
        
        sleep 2
        ((attempt++))
    done
    
    echo -e "${RED}✗ $display_name failed to become healthy after $max_attempts attempts${NC}"
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
echo ""
echo -e "${GREEN}Radarr:${NC}        http://localhost:7878"
echo -e "${GREEN}Sonarr:${NC}        http://localhost:8989"
echo -e "${GREEN}Plex Mock:${NC}     http://localhost:32400"
echo ""
echo "Next steps:"
echo "  1. Open http://localhost:5055 in your browser"
echo "  2. Follow ${BLUE}QUICK_SETUP_GUIDE.md${NC} (2 minutes)"
echo "  3. Configure your Android app with ${GREEN}http://YOUR_IP:5055${NC}"
echo ""
echo "Get your IP address:"
echo "  ${GREEN}hostname -I | awk '{print \$1}'${NC}"
echo ""
echo "Useful commands:"
echo "  View logs:    docker compose logs -f overseerr"
echo "  Stop:         docker compose down"
echo "  Restart:      docker compose restart"
echo ""
