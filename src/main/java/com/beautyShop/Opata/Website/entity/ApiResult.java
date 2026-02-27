package com.beautyShop.Opata.Website.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper used by all controllers.
 *
 * Success:  { "success": true,  "message": "...", "data": {...},  "timestamp": "..." }
 * Error:    { "success": false, "message": "...", "data": null,   "timestamp": "..." }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private final boolean       success;
    private final String        message;
    private final T             data;
    private final LocalDateTime timestamp;

    private ApiResult(boolean success, String message, T data) {
        this.success   = success;
        this.message   = message;
        this.data      = data;
        this.timestamp = LocalDateTime.now();
    }

    // ── Static factories ──────────────────────────────────────

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, "Success", data);
    }

    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(true, message, data);
    }

    public static ApiResult<String> success(String message) {
        return new ApiResult<>(true, message, null);
    }

    public static <T> ApiResult<T> error(String message) {
        return new ApiResult<>(false, message, null);
    }
}