package com.verysmartbus.email;

/** An error that retrying will not fix, such as invalid template data or SMTP authentication. */
public class PermanentEmailDeliveryException extends RuntimeException {

    public PermanentEmailDeliveryException(String message) {
        super(message);
    }
}
