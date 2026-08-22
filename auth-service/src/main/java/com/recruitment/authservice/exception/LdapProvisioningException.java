package com.recruitment.authservice.exception;

public class LdapProvisioningException extends RuntimeException {

    public LdapProvisioningException(String message) {
        super(message);
    }

    public LdapProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
