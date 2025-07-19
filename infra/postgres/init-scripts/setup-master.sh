#!/bin/bash
set -e

echo "Setting up PostgreSQL master database for replication..."

# Create replication user
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'replicator') THEN
            CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD 'replicator_password';
            RAISE NOTICE 'Created replicator role';
        ELSE
            RAISE NOTICE 'Replicator role already exists';
        END IF;
    END
    \$\$;

    -- Grant necessary permissions
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_USER};
    GRANT ALL PRIVILEGES ON DATABASE ${POSTGRES_DB} TO ${POSTGRES_USER};
    
    -- Enable replication for the main user
    ALTER ROLE ${POSTGRES_USER} WITH REPLICATION;
    
    -- Grant replication permissions to replicator
    GRANT CONNECT ON DATABASE ${POSTGRES_DB} TO replicator;
EOSQL

echo "PostgreSQL master database setup completed successfully"
