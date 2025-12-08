package com.vanderler.vanderler_backend.controller;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.repository.UserRepository;
import com.vanderler.vanderler_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "https://vanderler.netlify.app")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    // DTOs simples só pro perfil
    public record UserProfileResponse(
            Long id,
            String nome,
            String email,
            String role
    ) {}

    public record UpdateNameRequest(
            String nome,
            String email
    ) {}

    public UserController(UserService userService,
                          UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // ====== GET /api/users/me  -> usado para montar a tela de perfil ======
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile() {
        User user = userService.getUsuarioLogado();

        UserProfileResponse body = new UserProfileResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(body);
    }

    // ====== PUT /api/users/me  -> atualizar dados básicos (nome + email) ======
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> atualizarPerfil(
            @RequestBody UpdateNameRequest body
    ) {
        User user = userService.getUsuarioLogado();

        // Atualiza nome se veio preenchido
        if (body.nome() != null && !body.nome().isBlank()) {
            user.setNome(body.nome());
        }

        // Atualiza email se veio preenchido
        if (body.email() != null && !body.email().isBlank()) {
            user.setEmail(body.email());
        }

        userRepository.save(user);

        UserProfileResponse resp = new UserProfileResponse(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(resp);
    }
}
