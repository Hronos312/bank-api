package ru.bankapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dal.UserRepository;
import ru.bankapi.identity.InternalAccessToken;
import ru.bankapi.identity.InternalUserStatus;

@Hidden
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.identity.internal-enabled", havingValue = "true")
@RequestMapping("/internal/users")
class InternalUserController {

    private final UserRepository users;
    private final InternalAccessToken token;

    @GetMapping("/{id}")
    ResponseEntity<InternalUserStatus> status(@PathVariable long id,
            @RequestHeader(name = InternalAccessToken.HEADER, required = false) String suppliedToken) {
        if (!token.matches(suppliedToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return users.findById(id)
                .map(user -> new InternalUserStatus(user.getId(), user.getStatus(), user.getRole()))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
