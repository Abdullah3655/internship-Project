package com.recruitment.candidateservice.exception;

public class CandidateNotFoundException extends RuntimeException {

    public CandidateNotFoundException(String id) {
        super("Candidate not found: " + id);
    }
}
