package com.familygrowth.web;

import com.familygrowth.application.Stage3Service;
import com.familygrowth.domain.Stage3Models;
import com.familygrowth.domain.Stage3Models.Actor;
import com.familygrowth.domain.Stage3Models.AuthSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final Stage3Service service;

    AuthController(Stage3Service service) {
        this.service = service;
    }

    @PostMapping("/bootstrap")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Stage3Service.BootstrapResult> bootstrap(@Valid @RequestBody BootstrapRequest request) {
        return ApiResponse.ok(service.bootstrap(request.familyName(), request.parentName(), request.pin()));
    }

    @PostMapping("/login")
    ApiResponse<AuthSession> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(service.login(request.familyId(), request.parentId(), request.pin()));
    }

    @PostMapping("/child-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AuthSession> childSession(
        @RequestAttribute(Stage3Models.ACTOR_REQUEST_ATTRIBUTE) Actor actor,
        @Valid @RequestBody ChildSessionRequest request
    ) {
        return ApiResponse.ok(service.createChildSession(actor, request.childId()));
    }

    record BootstrapRequest(
        @NotBlank @Size(max = 100) String familyName,
        @NotBlank @Size(max = 80) String parentName,
        @NotBlank @Pattern(regexp = "\\d{6}") String pin
    ) {}

    record LoginRequest(
        @NotNull UUID familyId,
        @NotNull UUID parentId,
        @NotBlank @Pattern(regexp = "\\d{6}") String pin
    ) {}

    record ChildSessionRequest(@NotNull UUID childId) {}
}
