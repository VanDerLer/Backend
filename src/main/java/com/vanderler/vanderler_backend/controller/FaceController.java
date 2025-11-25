package com.vanderler.vanderler_backend.controller;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.repository.UserRepository;
import com.vanderler.vanderler_backend.service.AzureFaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/face")
public class FaceController {

    private final AzureFaceService azureFaceService;
    private final UserRepository userRepository;

    public FaceController(AzureFaceService azureFaceService, UserRepository userRepository) {
        this.azureFaceService = azureFaceService;
        this.userRepository = userRepository;
    }

    public record FaceImageRequest(String imageBase64) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody FaceImageRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (request.imageBase64 == null || request.imageBase64().isBlank()) {
            return ResponseEntity.badRequest().body("Imagem base64 não enviada.");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.imageBase64());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Imagem base64 inválida.");
        }

        azureFaceService.registerFace(user, imageBytes);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @AuthenticationPrincipal UserDetails principal,
            @RequestBody FaceImageRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado.");
        }

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (request.imageBase64 == null || request.imageBase64().isBlank()) {
            return ResponseEntity.badRequest().body("Imagem base64 não enviada.");
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.imageBase64());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Imagem base64 inválida.");
        }

        boolean match = azureFaceService.verifyFace(user, imageBytes);
        return ResponseEntity.ok(Map.of("match", match));
    }
}
