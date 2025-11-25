package com.vanderler.vanderler_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank String senha
    ) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String senha
    ) {}

    public record AuthResponse(
            String token,
            String nome,
            String email,
            String role,
            boolean biometriaCadastrada
    ) {}
}
