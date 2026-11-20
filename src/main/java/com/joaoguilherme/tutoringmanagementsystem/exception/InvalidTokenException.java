package com.joaoguilherme.tutoringmanagementsystem.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
