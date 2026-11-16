package ru.bankapi.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.bankapi.dto.auth.RegisterRequest;
import ru.bankapi.dto.user.UserResponse;
import ru.bankapi.service.admin.AdminUserService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Управление пользователями. Доступно только администраторам")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    @Operation(summary = "Создать клиента", description = "Создаёт нового пользователя с ролью CLIENT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "409", description = "Email или телефон уже используется")
    })
    public UserResponse createUser(@Valid @RequestBody RegisterRequest request) {
        return adminUserService.createUser(request);
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Возвращает список всех пользователей системы")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список пользователей получен"),
            @ApiResponse(responseCode = "401", description = "Пользователь не авторизован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    public List<UserResponse> getUsers() {
        return adminUserService.getUsers();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Получить пользователя", description = "Возвращает пользователя по его id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь найден"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public UserResponse getUser(@PathVariable Long userId) {
        return adminUserService.getUser(userId);
    }

    @PatchMapping("/{userId}/block")
    @Operation(summary = "Заблокировать пользователя", description = "Переводит пользователя в статус BLOCKED")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь заблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже заблокирован")
    })
    public UserResponse blockUser(@PathVariable Long userId) {
        return adminUserService.blockUser(userId);
    }

    @PatchMapping("/{userId}/unblock")
    @Operation(summary = "Разблокировать пользователя", description = "Переводит пользователя в статус ACTIVE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пользователь разблокирован"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
            @ApiResponse(responseCode = "409", description = "Пользователь уже активен")
    })
    public UserResponse unblockUser(@PathVariable Long userId) {
        return adminUserService.unblockUser(userId);
    }
}