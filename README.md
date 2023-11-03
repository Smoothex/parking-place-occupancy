# Running in Docker
Currently available components:
* PostgreSQL (with PostGIS)
* PostgreSQL Admin Panel
* Java program connecting to the DB

To start everything, simply run `docker-compose up -d` - it might take a while for everything to build.

The PostgreSQL container starts first, the other two services depend on it.

The contents of the [db](https://github.com/Smoothex/parking-place-occupancy/tree/main/src/db) folder are mounted as a volume to the PostgreSQL container, so that the edited `pg_hpa.conf` file replaces the old one. 

The pg_hba.conf file is a configuration file for PostgreSQL that controls client authentication. Each record in the file specifies a connection type, a client IP address range, a database, a user, and an authentication method. The difference between the configuration is one line, which the edited script contains:
`host all all 0.0.0.0/0 md5`
* `host`: This specifies the connection type. In this case, host refers to a TCP/IP connection. Other possible values include local for a Unix-domain socket and hostssl for a TCP/IP connection secured by SSL.
* `all`: The first all specifies that this rule applies to all databases. If you wanted to restrict this rule to a specific database, you would replace this with the name of that database.
* `all`: The second all specifies that this rule applies to all users. If you wanted to restrict this rule to a specific user, you would replace this with the name of that user.
* `0.0.0.0/0`: This specifies the client IP address range that this rule applies to. 0.0.0.0/0 is a CIDR notation that means any IPv4 address can connect. For more restrictive access, you could specify a more limited IP address range.
* `md5`: This specifies the authentication method. md5 means that when a connection is made, the PostgreSQL server will request an MD5-hashed password from the client. It is more secure than plain text passwords (password), but less secure than scram-sha-256.
