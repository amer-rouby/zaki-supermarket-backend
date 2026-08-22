package com.zakisupermarket.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class FeatureDisabledException extends LocalizedException {

    private static final long serialVersionUID = 1L;

    public FeatureDisabledException(String errorCode, String message) {
        super(HttpStatus.FORBIDDEN, errorCode, message, Map.of());
    }
}
