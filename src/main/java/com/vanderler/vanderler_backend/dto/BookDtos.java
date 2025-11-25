package com.vanderler.vanderler_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class BookDtos {

    public record BookCreateRequest(
            @NotBlank String titulo,
            @NotBlank String autor,
            String descricao,
            @NotBlank String conteudo,
            @NotBlank String categoria
    ) {}

    public record BookSummary(
            Long id,
            String titulo,
            String autor,
            String descricao,
            String categoria
    ) {}

    public record BookContentResponse(
            Long id,
            String titulo,
            String autor,
            String conteudo,
            String categoria
    ) {}
}
