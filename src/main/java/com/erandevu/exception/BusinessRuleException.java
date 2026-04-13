package com.erandevu.exception;

import java.util.Map;

/**
 * Exception thrown when a business rule is violated.
 * Extends BaseException for standardized error handling.
 */
public class BusinessRuleException extends BaseException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, cause);
    }

    public BusinessRuleException(String message, Map<String, Object> details) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message, details);
    }
}
