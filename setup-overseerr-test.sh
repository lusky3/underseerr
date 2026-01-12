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

# Wait for services to be ready
echo "Waiting for services to start..."
sleep 10

# Check service status
echo ""
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
echo -e "${GREEN}Radarr:${NC}        http://localhost:7878"
echo -e "${GREEN}Sonarr:${NC}        http://localhost:8989"
echo -e "${GREEN}Plex Mock:${NC}     http://localhost:32400"
echo ""
echo "Next steps:"
echo "1. Open http://localhost:5055 in your browser"
echo "2. Complete the initial setup wizard"
echo "3. Configure Plex connection (use mock server at localhost:32400)"
echo "4. Configure Radarr (http://localhost:7878)"
echo "5. Configure Sonarr (http://localhost:8989)"
echo "6. Update your Android app to use http://YOUR_IP:5055"
echo ""
echo "To view logs:"
echo "  docker compose logs -f overseerr"
echo ""
echo "To stop services:"
echo "  docker compose down"
echo ""
echo "To restart services:"
echo "  docker compose restart"
echo ""
