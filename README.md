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


# Local Build and execution:

# Frontend

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 16.2.8.

# Frontend angular

Pre-Requisite:

- node version ~ 21.1.0
- Npm version ~ 10.2.3

### Install angular locally

- check that npm is installed by running

```bash
npm --version 
```

- install Angular cli using  version 16.2.10.

```bash
sudo npm i @angular/cli@16.2.10
```

- install the required packages

```bash
npm install 
```

- for running the application

```tsx
ng serve
```

The frontend should be visible on : http://localhost:4200/


## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.




