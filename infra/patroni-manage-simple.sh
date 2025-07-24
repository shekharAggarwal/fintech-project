#!/bin/bash

# Patroni Management Script for Single ETCD Setup  
# Usage: ./patroni-manage.sh [status|help]

ETCD_ENDPOINT="http://localhost:2379"

show_status() {
  echo "🔍 Patroni Cluster Status (Single ETCD)"
  echo "======================================="
  echo ""
  
  echo "📊 ETCD Status:"
  curl -s $ETCD_ENDPOINT/health | jq '.' 2>/dev/null || echo "ETCD not responding"
  echo ""
  
  echo "🗄️  Patroni Clusters:"
  
  # Main Cluster
  echo "Main Cluster (fintech_main_cluster):"
  curl -s http://localhost:8008/cluster 2>/dev/null | jq '.' || echo "❌ Main master not responding"
  echo ""
  
  # Shard 1 Cluster  
  echo "Shard 1 Cluster (fintech_shard_1_cluster):"
  curl -s http://localhost:8010/cluster 2>/dev/null | jq '.' || echo "❌ Shard 1 master not responding"
  echo ""
  
  # Shard 2 Cluster
  echo "Shard 2 Cluster (fintech_shard_2_cluster):"
  curl -s http://localhost:8012/cluster 2>/dev/null | jq '.' || echo "❌ Shard 2 master not responding"
  echo ""
  
  # Shard 3 Cluster
  echo "Shard 3 Cluster (fintech_shard_3_cluster):"
  curl -s http://localhost:8014/cluster 2>/dev/null | jq '.' || echo "❌ Shard 3 master not responding"
  echo ""
}

show_help() {
  echo "Patroni Management Script (Single ETCD)"
  echo "========================================"
  echo ""
  echo "Usage: $0 [command]"
  echo ""
  echo "Commands:"
  echo "  status     - Show all cluster status"
  echo "  help       - Show this help"
  echo ""
  echo "🔍 Cluster Endpoints:"
  echo "  • Main Master: http://localhost:8008"
  echo "  • Main Replica: http://localhost:8009"
  echo "  • Shard 1 Master: http://localhost:8010"
  echo "  • Shard 1 Replica: http://localhost:8011"
  echo "  • Shard 2 Master: http://localhost:8012"
  echo "  • Shard 2 Replica: http://localhost:8013"
  echo "  • Shard 3 Master: http://localhost:8014"
  echo "  • Shard 3 Replica: http://localhost:8015"
  echo ""
  echo "💡 For advanced operations, use Patroni REST API directly:"
  echo "   curl http://localhost:8008/cluster"
  echo "   curl -X POST http://localhost:8008/switchover"
}

case "$1" in
  status)
    show_status
    ;;
  help|--help|-h|"")
    show_help
    ;;
  *)
    echo "❌ Unknown command: $1"
    show_help
    exit 1
    ;;
esac
