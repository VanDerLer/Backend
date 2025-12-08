package com.vanderler.vanderler_backend.controller;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.service.UserService;
import com.vanderler.vanderler_backend.service.WebAuthnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webauthn")
@CrossOrigin(origins = "https://vanderler.netlify.app") // 🔓 libera CORS pro Vite
public class WebAuthnController {

    private final WebAuthnService webAuthnService;
    private final UserService userService;

    public WebAuthnController(WebAuthnService webAuthnService,
                              UserService userService) {
        this.webAuthnService = webAuthnService;
        this.userService = userService;
    }

    /**
     * Opções de registro (PublicKeyCredentialCreationOptions)
     * Chamado pelo front em:
     * GET /webauthn/register/options
     */
    @GetMapping("/register/options")
    public Map<String, Object> getRegisterOptions() {
        // pega o usuário logado do JWT
        User user = userService.getUsuarioLogado();

        // delega pro seu service
        return webAuthnService.gerarOpcoesRegistro(user);
    }

    /**
     * Finaliza o registro WebAuthn.
     * Chamado pelo front em:
     * POST /webauthn/register/finish
     */
    @PostMapping("/register/finish")
    public ResponseEntity<Void> finishRegister(@RequestBody Map<String, Object> credentialResponse) {
        User user = userService.getUsuarioLogado();

        // salva a credencial no banco
        webAuthnService.finalizarRegistro(user, credentialResponse);

        return ResponseEntity.ok().build();
    }

    // SE QUISER DEIXAR PRONTO PARA LOGIN BIOMÉTRICO NO FUTURO:

    /**
     * Opções de autenticação (PublicKeyCredentialRequestOptions)
     * GET /webauthn/authenticate/options
     */
    @GetMapping("/authenticate/options")
    public Map<String, Object> getAuthOptions() {
        User user = userService.getUsuarioLogado();
        return webAuthnService.gerarOpcoesAutenticacao(user);
    }

    /**
     * Finaliza autenticação WebAuthn
     * POST /webauthn/authenticate/finish
     */
    @PostMapping("/authenticate/finish")
    public ResponseEntity<Void> finishAuth(@RequestBody Map<String, Object> assertionResponse) {
        User user = userService.getUsuarioLogado();
        webAuthnService.finalizarAutenticacao(user, assertionResponse);
        return ResponseEntity.ok().build();
    }
}
