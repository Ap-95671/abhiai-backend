package com.abhiai.abhiai_backend.exception;

public class MediaStorageUnavailableException extends RuntimeException {

    public MediaStorageUnavailableException() {
        super("Cloud media storage is temporarily unavailable");
    }
}
