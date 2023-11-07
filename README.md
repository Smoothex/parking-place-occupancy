# Parking Space Occupancy Visualization System

## Overview
The Parking Space Occupancy Visualization System project is a system designed for visualizing and editing the availability of parking lots. It integrates various components, including a PostgreSQL database with PostGIS extension, a PostgreSQL admin panel, and a Java program that connects to the database.

<p align="center">
  <img src="https://github.com/Smoothex/parking-place-occupancy/assets/79105432/2a2c5d69-48dc-4e78-93fa-b332ee555aed" />
</p>

## Components
- **[PostgreSQL](https://www.postgresql.org/) with [PostGIS](https://postgis.net/)**: A spatial database extender for PostgreSQL object-relational database. It adds support for geographic objects allowing location queries to be run in SQL.
- **PostgreSQL [Admin Panel](https://www.pgadmin.org/)**: A web-based administration tool for PostgreSQL that facilitates database management.
- **Java Program**: Connects to the database and handles the logic for updating and retrieving parking place availability.

## Getting Started
To run the system, execute the following command:
```
docker-compose up -d
```
Please note that the initial build process may take some time. The PostgreSQL container will start first as the other two services depend on it.

## Configuration Details
The `db` folder contains configuration files for PostgreSQL, including the `pg_hba.conf` - a configuration file for PostgreSQL that controls client authentication. Each record in the file specifies a connection type, a client IP address range, a database, a user, and an authentication method. The difference between the configuration is one line, which the edited script contains:
`host all all 0.0.0.0/0 md5`
* `host`: This specifies the connection type. In this case, host refers to a TCP/IP connection. Other possible values include local for a Unix-domain socket and hostssl for a TCP/IP connection secured by SSL.
* `all`: The first all specifies that this rule applies to all databases. If you wanted to restrict this rule to a specific database, you would replace this with the name of that database.
* `all`: The second all specifies that this rule applies to all users. If you wanted to restrict this rule to a specific user, you would replace this with the name of that user.
* `0.0.0.0/0`: This specifies the client IP address range that this rule applies to. 0.0.0.0/0 is a CIDR notation that means any IPv4 address can connect. For more restrictive access, you could specify a more limited IP address range.
* `md5`: This specifies the authentication method. md5 means that when a connection is made, the PostgreSQL server will request an MD5-hashed password from the client. It is more secure than plain text passwords (password), but less secure than scram-sha-256.

The contents of the [db](https://github.com/Smoothex/parking-place-occupancy/tree/main/src/db) folder are mounted as a volume to the PostgreSQL container, so that the edited `pg_hpa.conf` file replaces the old one. 


