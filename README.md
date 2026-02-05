## Purpose

This repository is not a product. It is a showcase project to practice and demonstrate Spring Boot best practices, production-ready patterns, and infrastructure-friendly setup.

## Architecture

### Layered (N-tier) Architecture

- **Controller**: Handles HTTP requests and responses
- **Service**: Contains business logic
- **Repository**: Data access layer (Spring Data JPA)
- **Entity/DTO/Mapper**: Data modeling and transformation
- **Exception Handling**: Centralized with `@ControllerAdvice`
- **Separation of Concerns**: Each layer has a single responsibility, making the codebase maintainable and testable.
- **Security**: Spring Security for authentication and authorization

## Testing

- **Unit Tests**:  
  Written with JUnit 5 and Mockito
- **Integration Tests**:  
  Using Spring Boot Test and MockMvc for end-to-end API testing.
- **Test Utilities**:  
  Utility classes for dummy data and JSON serialization.

## DevOps & Deployment

- **GitHub Actions**:  
  Automated pipeline for build, test, Docker image creation, and deployment to GKE.  
  Runs on every push to `main` branch.
- **Docker**:  
  Secure, minimal image using non-root user.
- **Kubernetes**:  
    - Deployment, Service, and Horizontal Pod Autoscaler manifests  
    - Cloud SQL Proxy sidecar for secure database connectivity  
    - Liveness and readiness probes for health checks  
    - Resource requests/limits and autoscaling for production readiness  
    - Kubernetes Secrets for sensitive configuration

## Getting Started

1. **Start MySQL (Local)**
    ```sh
    docker compose -f docker-compose-local.yml up -d
    ```

2. **Run the App**
    ```sh
    ./runLocal.sh
    ```

**Defaults / Local Profile**
- Profile: `local`
- DB: `worktrack_db` on `localhost:3307`
- Credentials: `worktrack / worktrack` (override with `db.username` / `db.password`)
- JWT: set `jwt.secret` via env or config (must be Base64; local default provided)

## What’s Demonstrated

- User registration with validation and duplicate checks
- Centralized error handling with meaningful error codes
- Secure password storage (hashing)
- Role-based access control
- Caching, retry, and observability hooks
- Automated CI/CD pipeline and cloud-native deployment

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security](https://docs.spring.io/spring-security/site/docs/current/reference/html5/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [GitHub Actions](https://docs.github.com/en/actions)

---

> **Note:**  
> This repository intentionally prioritizes best-practice patterns over business value or domain complexity.
