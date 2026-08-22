package com.recruitment.candidateservice.exception;

public class DuplicateCandidateEmailException extends RuntimeException {

    public DuplicateCandidateEmailException(String email) {
        super("A candidate already exists for " + email);
    }
}
