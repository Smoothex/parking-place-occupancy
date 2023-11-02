# Running in Docker
Currently available components:
* PostgreSQL (with PostGIS)
* PostgreSQL Admin Panel
* Java program connecting to the DB

To start everything, simply run `docker-compose up -d` - it might take a while for everything to build.

The PostgreSQL container starts first, the other two services depend on it.

Additionally, the Java program waits 10s before connecting to the DB. The reason for this is that in the PostgreSQL container, the `pg_hba.conf` gets replaced with the one in the _db_ folder **after** the container has started and this takes a bit of time. The reason for replacing this configuration file is to configure PostgreSQL to accept connection from all IPs (essentially, the `pg_hba.cong` file in _db_ differs from the original only by the last line: `host  all  all  0.0.0.0/0  md5`. Replacing the whole file is a bit clumsy, but I couldn't edit the file directly, since it requires the container to be restarted in order for the changes to be applied - and I couldn't find an easy way to restart the container once after it's been started.
