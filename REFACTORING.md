# Clean Architecture Refactoring Documentation

## Overview

This document describes the comprehensive refactoring of the E-Randevu Spring Boot project to follow Clean Architecture principles.

## Changes Summary

### 1. Application Layer Abstraction ✅

**Created:** `src/main/java/com/erandevu/application/UseCase.java`

- Generic `UseCase<I, O>` interface for all application use cases
- Support for Command, Query, and Void use cases
- Standardizes the application layer contract

```java
public interface UseCase<I, O> {
    O execute(I input);
}
```

### 2. Use Case Implementation ✅

**Refactored:** `CreateAppointmentUseCase.java`

- Implements `UseCase<CreateAppointmentCommand, AppointmentResponse>`
- Contains ALL business logic (validation, conflict checking, persistence)
- No dependency on Service layer
- Inlined validation rules for Clean Architecture

**Created:** `CreateAppointmentCommand.java`

- Immutable record for use case input
- Factory method with built-in validation
- Clean separation of input data

### 3. Controller Refactoring ✅

**Refactored:** `AppointmentController.java`

- Controller now ONLY calls UseCases
- Removed dependency on `AppointmentService`
- Security: Patient ID extracted from JWT token
- Clean flow: Controller → UseCase → Repository

```java
@PostMapping
public ResponseEntity<AppointmentResponse> createAppointment(
        @Valid @RequestBody AppointmentRequest request,
        @AuthenticationPrincipal User user) {
    
    CreateAppointmentCommand command = CreateAppointmentCommand.of(
        request.getDoctorId(),
        user.getId(),  // From JWT
        request.getAppointmentDateTime(),
        request.getNotes()
    );
    
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(createAppointmentUseCase.execute(command));
}
```

### 4. Domain Model Enhancement ✅

**Enhanced:** `Appointment.java`

Added rich domain behavior methods:

| Method | Description |
|--------|-------------|
| `cancel(String reason)` | Cancel with business rules |
| `reschedule(LocalDateTime)` | Reschedule with validation |
| `markCompleted()` | Mark as completed |
| `markNoShow()` | Mark as no-show |
| `canBeCancelled()` | Check if cancellation allowed |
| `isWithinCancellationWindow()` | Check time window |
| `isUpcoming()` | Check if scheduled and future |
| `getDurationMinutes()` | Get appointment duration |
| `getStatusDescription()` | Human-readable status |

### 5. Security Configuration Fix ✅

**Refactored:** `SecurityConfig.java`

- Replaced wildcard patterns with explicit origins
- Environment-based CORS configuration
- Security validation: Rejects wildcards in production
- Default: `http://localhost:3000, http://localhost:8080`

```java
@Value("${app.security.allowed-origins:http://localhost:3000}")
private List<String> allowedOrigins;

// Validation in corsConfigurationSource()
if (hasWildcards) {
    throw new IllegalStateException("Wildcard patterns not allowed");
}
```

### 6. Configuration Updates ✅

**Updated:** `application.properties`

```properties
# SECURITY CONFIGURATION
app.security.allowed-origins=http://localhost:3000,http://localhost:8080
```

**Created:** `application-test.properties`

- H2 in-memory database for tests
- Test-specific JWT configuration
- Debug logging for tests

### 7. Integration Tests ✅

**Created:** `AppointmentIntegrationTest.java`

Tests full Clean Architecture flow:
- Controller → UseCase → Domain → Repository → Database
- Tests for valid/invalid appointment creation
- Security: Uses `@WithMockUser` for authentication

Test cases:
- ✅ Should create appointment successfully
- ✅ Should reject appointment in the past
- ✅ Should reject appointment on weekend
- ✅ Should reject appointment outside business hours
- ✅ Should reject appointment less than 2 hours in advance

## Clean Architecture Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        PRESENTATION LAYER                     │
│                   (Controller, DTOs, Request/Response)          │
│                                                               │
│  AppointmentController                                        │
│       ↓                                                       │
│  CreateAppointmentCommand (Input)                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                      │
│                   (Use Cases, Application Services)           │
│                                                               │
│  CreateAppointmentUseCase                                     │
│  - Business logic validation                                  │
│  - Conflict checking                                          │
│  - Entity creation                                            │
│  - Persistence                                                │
│  - Event publishing                                           │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        DOMAIN LAYER                         │
│                   (Entities, Value Objects, Domain Events)    │
│                                                               │
│  Appointment                                                  │
│  - cancel()                                                   │
│  - reschedule()                                               │
│  - markCompleted()                                            │
│  - Business rules enforcement                                 │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                     │
│          (Repositories, External Services, Security)         │
│                                                               │
│  AppointmentRepository                                        │
│  UserRepository                                               │
│  SecurityConfig                                               │
└─────────────────────────────────────────────────────────────┘
```

## Package Structure

```
src/main/java/com/erandevu/
├── application/              # Application Layer
│   ├── UseCase.java          # Core abstraction
│   └── appointment/
│       ├── CreateAppointmentCommand.java
│       ├── CreateAppointmentUseCase.java
│       └── ...
├── controller/               # Presentation Layer
│   └── AppointmentController.java
├── domain/                   # Domain Layer (events)
│   └── event/
│       └── AppointmentCreatedEvent.java
├── entity/                   # Domain Layer (entities)
│   ├── Appointment.java
│   ├── User.java
│   └── ...
├── repository/               # Infrastructure Layer
│   └── AppointmentRepository.java
├── config/                   # Infrastructure Layer
│   └── SecurityConfig.java
└── service/                  # Deprecated (to be removed)
    └── validation/           # Keep validators for now
```

## Benefits of This Refactoring

### 1. **Separation of Concerns**
- Each layer has a single responsibility
- Business logic is centralized in UseCases
- Domain logic is in Entities

### 2. **Testability**
- UseCases can be unit tested independently
- Integration tests verify full flow
- No dependency on framework in domain layer

### 3. **Maintainability**
- Clear architecture boundaries
- Easy to locate and modify code
- Business rules are explicit

### 4. **Security**
- Patient ID extracted from JWT (not request body)
- CORS with explicit origins (no wildcards)
- Input validation in multiple layers

### 5. **Scalability**
- New features follow established patterns
- Easy to add new UseCases
- Domain logic is reusable

## Next Steps

1. **Complete UseCase Migration**
   - Refactor `CancelAppointmentUseCase`
   - Refactor `UpdateAppointmentUseCase`
   - Refactor `GetAppointmentUseCase`

2. **Remove Service Layer**
   - Delete `AppointmentService.java`
   - Update any remaining dependencies

3. **Add More Integration Tests**
   - Cancel appointment flow
   - Update appointment flow
   - Conflict scenarios

4. **Documentation**
   - API documentation (OpenAPI)
   - Architecture decision records (ADRs)

## Migration Guide

### For New Features

1. Create Command object for input
2. Create UseCase implementing `UseCase<Command, Response>`
3. Add business logic to UseCase
4. Update Controller to use UseCase
5. Add integration test

### For Existing Features

1. Identify duplicated logic in Service
2. Move logic to appropriate UseCase
3. Update Controller
4. Remove Service method
5. Verify with tests

## References

- [Clean Architecture by Robert C. Martin](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Spring Boot Clean Architecture](https://spring.io/blog)
- [Domain-Driven Design](https://dddcommunity.org/)
