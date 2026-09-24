package com.neueda.leap.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard API response envelope for all endpoints
 * Provides consistent success/error handling across the platform
 */
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private List<ErrorObject> errors;
    private ResponseMeta meta;

    // Constructors
    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, List<ErrorObject> errors) {
        this.success = success;
        this.data = data;
        this.errors = errors;
        this.meta = new ResponseMeta();
    }

    public ApiResponse(boolean success, T data) {
        this(success, data, List.of());
    }

    // Factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, List.of());
    }

    public static <T> ApiResponse<T> error(List<ErrorObject> errors) {
        return new ApiResponse<>(false, null, errors);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<ErrorObject> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorObject> errors) {
        this.errors = errors;
    }

    public ResponseMeta getMeta() {
        return meta;
    }

    public void setMeta(ResponseMeta meta) {
        this.meta = meta;
    }

    // Inner class for error details
    public static class ErrorObject {
        private String code;
        private String message;
        private String field;
        private boolean retryable;

        public ErrorObject(String code, String message, String field, boolean retryable) {
            this.code = code;
            this.message = message;
            this.field = field;
            this.retryable = retryable;
        }

        public ErrorObject(String code, String message) {
            this(code, message, null, false);
        }

        // Getters and Setters
        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public void setRetryable(boolean retryable) {
            this.retryable = retryable;
        }
    }

    // Inner class for response metadata
    public static class ResponseMeta {
        private String requestId;
        private OffsetDateTime timestamp;
        private String version;
        private Integer page;
        private Integer totalCount;

        public ResponseMeta() {
            this.requestId = "req_" + System.nanoTime();
            this.timestamp = OffsetDateTime.now();
            this.version = "1.0.0";
        }

        // Getters and Setters
        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Integer getPage() {
            return page;
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }
    }
}
