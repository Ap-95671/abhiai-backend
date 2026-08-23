package com.abhiai.abhiai_backend.exception;

public class DuplicatePollVoteException extends RuntimeException {
    public DuplicatePollVoteException() { super("You have already voted in this poll"); }
}
