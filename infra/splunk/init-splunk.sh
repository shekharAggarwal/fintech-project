#!/bin/bash
set -e

echo "Setting up Splunk with HEC..."

# Start Splunk in background
/sbin/entrypoint.sh start-service &

# Wait for Splunk to be ready
echo "Waiting for Splunk to initialize..."
sleep 120

# Create the fintech index
echo "Creating fintech index..."
/opt/splunk/bin/splunk add index fintech -auth admin:${SPLUNK_PASSWORD}

# Enable HEC globally
echo "Enabling HEC globally..."
/opt/splunk/bin/splunk http-event-collector enable -auth admin:${SPLUNK_PASSWORD}

# Create HEC token
echo "Creating HEC token..."
/opt/splunk/bin/splunk http-event-collector create fintech-hec \
  -uri https://localhost:8889 \
  -token ${SPLUNK_HEC_TOKEN} \
  -indexes fintech,main \
  -sourcetype json \
  -auth admin:${SPLUNK_PASSWORD}

echo "Splunk setup completed successfully!"

# Keep the container running
wait
