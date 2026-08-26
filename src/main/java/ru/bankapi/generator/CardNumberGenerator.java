package ru.bankapi.generator;


import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CardNumberGenerator {

    private static final String PREFIX = "2200";
    private static final int CARD_NUMBER_LENGTH = 16;

    private static final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder number = new StringBuilder(PREFIX);

        while (number.length() < CARD_NUMBER_LENGTH - 1) {
            number.append(random.nextInt(10));
        }

        int checkDigit = calculateLuhnCheckDigit(number.toString());
        number.append(checkDigit);

        return number.toString();
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean doubleDigit = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return (10 - (sum % 10)) % 10;
    }

}
