package com.api.meteorologia.app.common.exception;

import org.springframework.http.HttpStatus;

public class CacheException extends BusinessException {
    public CacheException(String message) {
        super(message, "CACHE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}