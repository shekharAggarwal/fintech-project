#!/bin/bash
set -e

echo "Setting up PostgreSQL replica database..."

# This script is called during replica initialization
# The actual replication setup is done via pg_basebackup in the docker-compose command

# Wait for the replica to be ready
until pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"; do
    echo "Waiting for replica database to be ready..."
    sleep 2
done

echo "PostgreSQL replica database setup completed successfully"
