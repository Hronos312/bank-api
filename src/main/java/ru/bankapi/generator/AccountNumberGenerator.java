package ru.bankapi.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AccountNumberGenerator {

    private static final String PREFIX = "40817810";
    private static final int ACCOUNT_NUMBER_LENGTH = 20;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder number = new StringBuilder(PREFIX);

        while (number.length() < ACCOUNT_NUMBER_LENGTH) {
            number.append(random.nextInt(10));
        }

        return number.toString();
    }
}