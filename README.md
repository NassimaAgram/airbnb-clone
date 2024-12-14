# Airbnb clone (fullstack project)

## Key Features:
- 📅 Booking management for travelers
- 🏠 Landlord reservation management
- 🔍 Search for houses by criteria (location, date, guests, beds, etc)
- 🔐 Authentication and Authorization (Role management) with Auth0 (OAuth2)
- 🏢 Domain-driven design

## Prerequisites
- [NodeJS 20.11 LTS](https://nodejs.org/dist/v20.11.1/node-v20.11.1.pkg)
- [Angular CLI v17](https://www.npmjs.com/package/@angular/cli)
- [JDK 21](https://adoptium.net/temurin/releases/)
- [PostgreSQL](https://www.postgresql.org/download/)
- IDE ([VSCode](https://code.visualstudio.com/download), [IntelliJ](https://www.jetbrains.com/idea/download/))

## Frontend
### Fetch dependencies
``npm install --legacy-peer-deps``

### Launch dev server
Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build
Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Backend
### Launch
#### Maven
``./mvnw spring-boot:run  -Dspring-boot.run.arguments="--AUTH0_CLIENT_ID=<client-id> --AUTH0_CLIENT_SECRET=<client-secret>"``

#### IntelliJ
Go in IntelliJ add the environment variables and then run it.

