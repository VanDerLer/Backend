package com.vanderler.vanderler_backend.service;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.model.WebAuthnCredential;
import com.vanderler.vanderler_backend.repository.WebAuthnCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;

@Service
public class WebAuthnService {

    private final WebAuthnCredentialRepository credentialRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Decoder urlDecoder = Base64.getUrlDecoder();

    public WebAuthnService(WebAuthnCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    private String randomBase64Url(int size) {
        byte[] bytes = new byte[size];
        secureRandom.nextBytes(bytes);
        return urlEncoder.encodeToString(bytes);
    }

    private String encodeToBase64Url(byte[] bytes) {
        return urlEncoder.encodeToString(bytes);
    }

    private byte[] decodeBase64Url(String value) {
        return urlDecoder.decode(value);
    }

    /**
     * Gera as opções de registro (PublicKeyCredentialCreationOptions)
     * em formato JSON-friendly.
     */
    public Map<String, Object> gerarOpcoesRegistro(User user) {
        // Challenge aleatório
        String challenge = randomBase64Url(32);

        // ID do usuário em base64url
        byte[] userIdBytes = user.getId().toString().getBytes(StandardCharsets.UTF_8);
        String userId = encodeToBase64Url(userIdBytes);

        Map<String, Object> rp = new HashMap<>();
        rp.put("name", "Vanderler");
        rp.put("id", "localhost"); // host de desenvolvimento

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("name", user.getEmail());
        userMap.put("displayName", user.getNome());

        // Algoritmos aceitos (ES256)
        List<Map<String, Object>> pubKeyCredParams = List.of(
                Map.of("type", "public-key", "alg", -7)
        );

        // Excluir credenciais já cadastradas (pra não duplicar)
        List<WebAuthnCredential> creds = credentialRepository.findByUser(user);
        List<Map<String, Object>> excludeCredentials = creds.stream()
                .map(c -> Map.<String, Object>of(
                        "type", "public-key",
                        "id", encodeToBase64Url(c.getCredentialId())
                ))
                .toList();

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("rp", rp);
        options.put("user", userMap);
        options.put("pubKeyCredParams", pubKeyCredParams);
        options.put("timeout", 60000);
        options.put("excludeCredentials", excludeCredentials);
        options.put("attestation", "direct");

        return options;
    }

    /**
     * Finaliza o registro WebAuthn: guarda a credencial no banco.
     * Aqui está simplificado – não fazemos validação criptográfica completa.
     */
    @Transactional
    public void finalizarRegistro(User user, Map<String, Object> credentialResponse) {
        // Espera receber: { id, rawId, type, response{...}, ... }
        String rawIdBase64 = (String) credentialResponse.get("rawId");
        if (rawIdBase64 == null) {
            throw new IllegalArgumentException("rawId não informado");
        }

        byte[] credentialId = decodeBase64Url(rawIdBase64);

        // Evitar duplicados
        if (credentialRepository.existsByUser(user)) {
            // Se quiser, você pode sobrescrever; aqui só impede múltiplos
            return;
        }

        WebAuthnCredential cred = new WebAuthnCredential();
        cred.setUser(user);
        cred.setCredentialId(credentialId);

        // Por simplicidade, guarda o JSON da resposta como "publicKey"
        cred.setPublicKey("Cadastrado via WebAuthn simplificado");
        cred.setSignCount(0L);

        credentialRepository.save(cred);
    }

    /**
     * Gera as opções de autenticação (PublicKeyCredentialRequestOptions).
     */
    public Map<String, Object> gerarOpcoesAutenticacao(User user) {
        List<WebAuthnCredential> creds = credentialRepository.findByUser(user);
        if (creds.isEmpty()) {
            throw new IllegalStateException("Usuário não possui credencial biométrica cadastrada");
        }

        String challenge = randomBase64Url(32);

        List<Map<String, Object>> allowCredentials = creds.stream()
                .map(c -> Map.<String, Object>of(
                        "type", "public-key",
                        "id", encodeToBase64Url(c.getCredentialId())
                ))
                .toList();

        Map<String, Object> options = new HashMap<>();
        options.put("challenge", challenge);
        options.put("timeout", 60000);
        options.put("rpId", "localhost");
        options.put("allowCredentials", allowCredentials);
        options.put("userVerification", "preferred");

        return options;
    }

    /**
     * Finaliza a autenticação. Aqui só checamos se a credencial usada
     * pertence ao usuário logado e atualizamos signCount de forma simples.
     */
    @Transactional
    public void finalizarAutenticacao(User user, Map<String, Object> assertionResponse) {
        String rawIdBase64 = (String) assertionResponse.get("rawId");
        if (rawIdBase64 == null) {
            throw new IllegalArgumentException("rawId não informado");
        }

        byte[] credentialId = decodeBase64Url(rawIdBase64);

        WebAuthnCredential cred = credentialRepository.findByCredentialId(credentialId)
                .orElseThrow(() -> new IllegalStateException("Credencial WebAuthn não encontrada"));

        if (!cred.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Credencial não pertence ao usuário logado");
        }

        // Incremento simples só pra ter um histórico
        cred.setSignCount(cred.getSignCount() + 1);
        credentialRepository.save(cred);
    }
}
