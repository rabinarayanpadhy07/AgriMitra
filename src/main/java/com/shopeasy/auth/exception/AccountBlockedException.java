package com.shopeasy.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountBlockedException extends ApiException {
    public AccountBlockedException() {
        super("Your account has been blocked. Please contact support.", HttpStatus.FORBIDDEN);
    }
}
