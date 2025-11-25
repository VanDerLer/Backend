package com.vanderler.vanderler_backend.service;

import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Map;

@Service
public class AzureFaceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userRepository;

    @Value("${azure.face.endpoint}")
    private String endpoint;

    @Value("${azure.face.key}")
    private String apiKey;

    @Value("${azure.face.threshold:0.75}")
    private double threshold;

    // 🔧 se não tiver no application.properties, fica false por padrão
    @Value("${azure.face.mock:false}")
    private boolean mockMode;

    public AzureFaceService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // helper
    public boolean isMockMode() {
        return mockMode;
    }

    /**
     * Chama o /detect do Azure e pega um único faceId
     */
    private String detectSingleFaceId(byte[] imageBytes) {
        String url = endpoint + "/face/v1.0/detect"
                + "?returnFaceId=true"
                + "&recognitionModel=recognition_04";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set("Ocp-Apim-Subscription-Key", apiKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(imageBytes, headers);

        System.out.println("[AzureFaceService] Chamando /detect no Azure...");

        ResponseEntity<List<Map<String, Object>>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                );

        List<Map<String, Object>> body = response.getBody();
        System.out.println("[AzureFaceService] /detect status = " + response.getStatusCode());
        System.out.println("[AzureFaceService] /detect body = " + body);

        if (body == null || body.isEmpty()) {
            throw new IllegalStateException("Nenhum rosto detectado na imagem.");
        }
        if (body.size() > 1) {
            throw new IllegalStateException("Mais de um rosto detectado na imagem.");
        }

        Object faceId = body.get(0).get("faceId");
        if (faceId == null) {
            throw new IllegalStateException("FaceId não retornado pelo Azure.");
        }

        return faceId.toString();
    }

    /**
     * Chama /verify do Azure comparando dois faceIds
     */
    private AzureVerifyResponse callVerify(String faceId1, String faceId2) {
        String url = endpoint + "/face/v1.0/verify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Ocp-Apim-Subscription-Key", apiKey);

        Map<String, String> payload = Map.of(
                "faceId1", faceId1,
                "faceId2", faceId2
        );

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        System.out.println("[AzureFaceService] Chamando /verify no Azure...");
        ResponseEntity<AzureVerifyResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        AzureVerifyResponse.class
                );

        AzureVerifyResponse body = response.getBody();
        System.out.println("[AzureFaceService] /verify status = " + response.getStatusCode());
        System.out.println("[AzureFaceService] /verify body = " + body);

        return body;
    }

    /**
     * Registro do rosto do usuário.
     * Em modo MOCK, NÃO salvamos a imagem inteira pra não estourar a coluna.
     */
    public void registerFace(User user, byte[] imageBytes) {
        System.out.println("[AzureFaceService] registerFace() chamado. mockMode = " + mockMode);
        System.out.println("[AzureFaceService] imageBytes length = " +
                (imageBytes != null ? imageBytes.length : 0));

        if (isMockMode()) {
            System.out.println("[AzureFaceService] MOCK: não chamando Azure nem salvando imagem grande.");

            // 👉 aqui a gente só marca que o usuário tem uma "face" cadastrada
            // pra não deixar a coluna face_image gigante.
            user.setFaceImage(new byte[]{1}); // 1 byte só, caberia em qualquer tipo binário
            try {
                userRepository.save(user);
                System.out.println("[AzureFaceService] MOCK: usuário salvo com face_image simbólico.");
            } catch (DataIntegrityViolationException ex) {
                System.out.println("[AzureFaceService] ERRO ao salvar usuário em modo MOCK: " + ex.getMessage());
                throw ex;
            }
            return;
        }

        // 🔹 MODO REAL (quando você tiver o acesso do Azure aprovado)
        String faceId = detectSingleFaceId(imageBytes);
        System.out.println("[AzureFaceService] faceId detectado no registro = " + faceId);

        user.setFaceImage(imageBytes);

        try {
            userRepository.save(user);
            System.out.println("[AzureFaceService] Usuário salvo com face_image real.");
        } catch (DataIntegrityViolationException ex) {
            System.out.println("[AzureFaceService] ERRO ao salvar imagem real no banco: " + ex.getMessage());
            System.out.println("[AzureFaceService] Provavelmente a coluna face_image é pequena demais. " +
                    "Use LONGBLOB no MySQL.");
            throw ex;
        }
    }

    /**
     * Verificação facial.
     * Em modo MOCK, sempre retorna true (só pra fluxo de estudo).
     */
    public boolean verifyFace(User user, byte[] newImageBytes) {
        System.out.println("[AzureFaceService] verifyFace() chamado. mockMode = " + mockMode);

        if (isMockMode()) {
            System.out.println("[AzureFaceService] MOCK: sempre retornando true na verificação.");
            return true;
        }

        byte[] stored = user.getFaceImage();
        if (stored == null || stored.length == 0) {
            throw new IllegalStateException("Usuário não possui imagem de rosto cadastrada.");
        }

        String faceId1 = detectSingleFaceId(stored);
        String faceId2 = detectSingleFaceId(newImageBytes);

        AzureVerifyResponse verify = callVerify(faceId1, faceId2);
        if (verify == null) {
            throw new IllegalStateException("Resposta inválida da Azure Face API.");
        }

        System.out.println("[AzureFaceService] Resultado verify: isIdentical=" +
                verify.isIdentical() + ", confidence=" + verify.confidence());

        return verify.isIdentical() || verify.confidence() >= threshold;
    }

    public record AzureVerifyResponse(boolean isIdentical, double confidence) {}
}
