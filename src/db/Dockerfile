FROM postgis/postgis:latest

# Copy the scripts to docker-entrypoint-initdb.d so it gets run automatically
COPY config-update.sh /docker-entrypoint-initdb.d/
COPY pg_hba.conf /tmp/

# Make the script executable
RUN chmod +x /docker-entrypoint-initdb.d/config-update.sh

# Give access rights to the postgres user over pg_hba.conf, so it can be moved by the script
RUN chown postgres:postgres /tmp/pg_hba.conf
RUN chmod 644 /tmp/pg_hba.conf

RUN sleep 20
