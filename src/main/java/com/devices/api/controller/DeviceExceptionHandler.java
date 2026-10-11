package com.devices.api.controller;

import com.devices.api.service.DeviceInUseException;
import com.devices.api.service.DeviceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DeviceExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ProblemDetail handleNotFound(DeviceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(DeviceInUseException.class)
    public ProblemDetail handleInUse(DeviceInUseException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
