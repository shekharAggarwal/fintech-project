#!/bin/bash

# Wait for Splunk to be ready
echo "Waiting for Splunk to be ready..."
sleep 60

# Set up the HTTP Event Collector
curl -k -u admin:splunkpassword123 \
  -X POST \
  "http://localhost:8889/services/data/inputs/http" \
  -d name="fintech-hec" \
  -d token="fintech-hec-token-2024" \
  -d disabled=0 \
  -d index="fintech" \
  -d sourcetype="json"

echo "Splunk HEC setup completed"
