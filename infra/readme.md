Connection Details:

Host: localhost

Port: 3307

Database: fintech_db

Username: fintech_user

Password: fintech_password

JDBC URL: jdbc:postgresql://localhost:3307/fintech_db

Option 2: Direct Database Connections
Connect directly to individual PostgreSQL databases for development/debugging:

Database	Host	Port	Database Name	Purpose
Main Database	localhost	5432	fintech_main	Primary data
Shard 1	localhost	5433	fintech_shard1	User data shard
Shard 2	localhost	5434	fintech_shard2	User data shard
Shard 3	localhost	5435	fintech_shard3	User data shard
Auth Database	localhost	5436	fintech_auth	Authentication
Scheduler Database	localhost	5437	fintech_scheduler	Scheduled tasks
Retry Database	localhost	5438	fintech_retry	Retry logic
