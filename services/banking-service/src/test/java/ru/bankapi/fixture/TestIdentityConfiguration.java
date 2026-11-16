package ru.bankapi.fixture;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestIdentityConfiguration {

    @Bean
    @Primary
    TestIdentityDirectory testIdentityDirectory() {
        return new TestIdentityDirectory();
    }
}
