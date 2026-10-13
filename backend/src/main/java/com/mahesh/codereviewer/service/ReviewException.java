package com.mahesh.codereviewer.service;

import org.springframework.http.HttpStatus;

public class ReviewException extends RuntimeException {

    private final HttpStatus status;

    public ReviewException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
