package ru.bankapi.notification.event;

public class InvalidBankEventException extends RuntimeException {

    public InvalidBankEventException(String message) {
        super(message);
    }
}
