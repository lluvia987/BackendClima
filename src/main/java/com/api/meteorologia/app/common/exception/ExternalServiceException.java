package com.api.meteorologia.app.common.exception;

import org.springframework.http.HttpStatus;

public class ExternalServiceException extends BusinessException {
    private final String serviceName;

    public ExternalServiceException(String message, String serviceName) {
        super(message, "EXTERNAL_SERVICE_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
