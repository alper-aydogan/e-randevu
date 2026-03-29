package com.erandevu.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.CONFLICT)
public class OptimisticLockException extends RuntimeException {
    private final String entityType;
    private final Long entityId;
    private final Long currentVersion;
    
    public OptimisticLockException(String message, String entityType, Long entityId, Long currentVersion) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentVersion = currentVersion;
    }
    
    public OptimisticLockException(String message, Throwable cause, String entityType, Long entityId, Long currentVersion) {
        super(message, cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentVersion = currentVersion;
    }
}
