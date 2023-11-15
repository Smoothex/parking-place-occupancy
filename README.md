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



