package org.example.api.utils;

import org.example.api.exception.ValidationException;

/**
 * Utility class for request validation.
 * Provides common validation methods to ensure request parameters are valid.
 */
public class ValidationUtils {

    /**
     * Validates that a string parameter is not null or empty.
     *
     * @param value The value to validate
     * @param fieldName The name of the field for error messages
     * @throws ValidationException if validation fails
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName, "must not be empty");
        }
    }

    /**
     * Validates that a URL parameter is not null or empty.
     *
     * @param url The URL to validate
     * @throws ValidationException if validation fails
     */
    public static void requireValidUrl(String url) {
        requireNonEmpty(url, "URL");

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new ValidationException("URL", "must start with http:// or https://");
        }
    }

    /**
     * Validates that a service ID is valid (non-negative).
     *
     * @param serviceId The service ID to validate
     * @throws ValidationException if validation fails
     */
    public static void requireValidServiceId(int serviceId) {
        if (serviceId < 0) {
            throw new ValidationException("serviceId", "must be non-negative");
        }
    }

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
}
