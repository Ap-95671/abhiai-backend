package com.abhiai.abhiai_backend.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("An account already exists for email: " + email);
    }
}
