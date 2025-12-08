package com.vanderler.vanderler_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanderler.vanderler_backend.dto.AuthDtos.AuthResponse;
import com.vanderler.vanderler_backend.dto.AuthDtos.LoginRequest;
import com.vanderler.vanderler_backend.dto.AuthDtos.RegisterRequest;
import com.vanderler.vanderler_backend.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "https://vanderler.netlify.app")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest dto) {
        return ResponseEntity.ok(userService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest dto) {
        return ResponseEntity.ok(userService.login(dto));
    }
}
