#!/bin/bash

# Wait for Overseerr to start
sleep 10

# Initialize Overseerr with test data
# This script will be executed after Overseerr starts

echo "Overseerr initialization script started"

# Start the main Overseerr process
exec /init
