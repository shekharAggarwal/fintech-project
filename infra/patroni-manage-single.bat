@echo off
REM Patroni Management Script for Windows (Single ETCD Setup)
REM Usage: patroni-manage-single.bat [status|help]

if "%1"=="status" goto status
if "%1"=="help" goto help
if "%1"=="" goto help
goto unknown

:status
echo 🔍 Patroni Cluster Status (Single ETCD)
echo =======================================
echo.

echo 📊 ETCD Status:
curl -s http://localhost:2379/health 2>nul || echo ETCD not responding
echo.

echo 🗄️  Patroni Clusters:
echo.

echo Main Cluster (fintech_main_cluster):
curl -s http://localhost:8008/cluster 2>nul || echo ❌ Main master not responding
echo.

echo Shard 1 Cluster (fintech_shard_1_cluster):
curl -s http://localhost:8010/cluster 2>nul || echo ❌ Shard 1 master not responding
echo.

echo Shard 2 Cluster (fintech_shard_2_cluster):
curl -s http://localhost:8012/cluster 2>nul || echo ❌ Shard 2 master not responding
echo.

echo Shard 3 Cluster (fintech_shard_3_cluster):
curl -s http://localhost:8014/cluster 2>nul || echo ❌ Shard 3 master not responding
echo.

goto end

:help
echo Patroni Management Script (Single ETCD)
echo ========================================
echo.
echo Usage: %0 [command]
echo.
echo Commands:
echo   status     - Show all cluster status
echo   help       - Show this help
echo.
echo 🔍 Cluster Endpoints:
echo   • Main Master: http://localhost:8008
echo   • Main Replica: http://localhost:8009
echo   • Shard 1 Master: http://localhost:8010
echo   • Shard 1 Replica: http://localhost:8011
echo   • Shard 2 Master: http://localhost:8012
echo   • Shard 2 Replica: http://localhost:8013
echo   • Shard 3 Master: http://localhost:8014
echo   • Shard 3 Replica: http://localhost:8015
echo.
echo 💡 For advanced operations, use Patroni REST API directly:
echo    curl http://localhost:8008/cluster
echo    curl -X POST http://localhost:8008/switchover
goto end

:unknown
echo ❌ Unknown command: %1
goto help

:end
