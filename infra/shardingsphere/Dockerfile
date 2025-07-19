FROM apache/shardingsphere-proxy:5.4.1

# Copy PostgreSQL JDBC driver
COPY lib/postgresql-42.7.2.jar /opt/shardingsphere-proxy/lib/

# Copy configuration files
COPY config/ /opt/shardingsphere-proxy/conf/

# Fix permissions and create necessary directories
USER root
RUN chmod -R 755 /opt/shardingsphere-proxy/conf/ && \
    chmod -R 755 /opt/shardingsphere-proxy/lib/ && \
    mkdir -p /opt/shardingsphere-proxy/logs && \
    chmod 755 /opt/shardingsphere-proxy/logs && \
    groupadd -f shardingsphere && \
    useradd -g shardingsphere shardingsphere 2>/dev/null || true && \
    chown -R shardingsphere:shardingsphere /opt/shardingsphere-proxy/

USER shardingsphere

EXPOSE 3307

CMD ["sh", "/opt/shardingsphere-proxy/bin/start.sh", "3307"]
