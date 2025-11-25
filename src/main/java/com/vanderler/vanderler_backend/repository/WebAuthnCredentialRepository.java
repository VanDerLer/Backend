package com.vanderler.vanderler_backend.repository;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.model.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {

    List<WebAuthnCredential> findByUser(User user);

    Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId);

    boolean existsByUser(User user);
}
