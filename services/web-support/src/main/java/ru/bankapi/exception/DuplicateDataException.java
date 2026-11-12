package ru.bankapi.exception;

public class DuplicateDataException extends RuntimeException {

    private final String code;

    public DuplicateDataException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}