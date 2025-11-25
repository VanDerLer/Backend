package com.vanderler.vanderler_backend.service;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.vanderler.vanderler_backend.dto.AuthDtos;
import com.vanderler.vanderler_backend.model.Role;
import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.model.WebAuthnCredential;
import com.vanderler.vanderler_backend.repository.UserRepository;
import com.vanderler.vanderler_backend.repository.WebAuthnCredentialRepository;
import com.vanderler.vanderler_backend.security.CustomUserDetails;
import com.vanderler.vanderler_backend.security.JwtService;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final WebAuthnCredentialRepository webAuthnCredentialRepository;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authManager,
                       JwtService jwtService,
                       WebAuthnCredentialRepository webAuthnCredentialRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.webAuthnCredentialRepository = webAuthnCredentialRepository;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(BAD_REQUEST, "E-mail já cadastrado");
        }

        Role role = userRepository.count() == 0 ? Role.ADMIN : Role.USER;

        User user = new User();
        user.setNome(dto.nome());
        user.setEmail(dto.email());
        user.setSenha(passwordEncoder.encode(dto.senha()));
        user.setRole(role);

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        // ✅ Após cadastrar, ainda não tem biometria
        boolean biometriaCadastrada = false;

        return new AuthDtos.AuthResponse(
                token,
                user.getNome(),
                user.getEmail(),
                user.getRole().name(),
                biometriaCadastrada
        );
    }

    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest dto) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            CustomUserDetails cud = (CustomUserDetails) auth.getPrincipal();
            User user = cud.getDomainUser();
            String token = jwtService.generateToken(user);

            // ✅ Verifica se já existe credencial WebAuthn pra esse usuário
            boolean biometriaCadastrada = webAuthnCredentialRepository.existsByUser(user);

            return new AuthDtos.AuthResponse(
                    token,
                    user.getNome(),
                    user.getEmail(),
                    user.getRole().name(),
                    biometriaCadastrada
            );
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(UNAUTHORIZED, "Credenciais inválidas");
        }
    }

    public User getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails cud)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Não autenticado");
        }
        return cud.getDomainUser();
    }
}
