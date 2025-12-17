# Read Me First
All the information about the code challenge is in [CODE_CHALLENGE.md](./CODE_CHALLENGE.md)

## ⭐ Implementation Details
For a complete overview of the solution architecture, design decisions, and implementation details, please read **[IMPLEMENTATION_CHANGES.md](./IMPLEMENTATION_CHANGES.md)**

# Getting Started
1. Start docker-compose with postgres:
   ```bash
   docker-compose up -d
   ```
2. Start the app:
   ```bash
   ./gradlew bootRun
   ```
3. Hit the following endpoints to test the service:
   ```bash
   curl -X POST -H "Content-Type: application/json" localhost:8080/register -d '{ "id": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notifications": ["type1","type5"]}'
   curl -X POST -H "Content-Type: application/json" localhost:8080/notify -d '{ "userId": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notificationType": "type5", "message": "your app rocks!"}'
   ```

## 📚 API Documentation (Swagger)

Once the application is running, access the Swagger UI at:
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 🛠️ Code Quality

### Spotless (Code Formatting)
This project uses [Spotless](https://github.com/diffplug/spotless) with [ktlint](https://ktlint.github.io/) for consistent code formatting.

```bash
# Check code formatting
./gradlew spotlessCheck

# Apply code formatting automatically
./gradlew spotlessApply
```

**Note:** The build will fail if code is not properly formatted. Run `spotlessApply` before committing.
