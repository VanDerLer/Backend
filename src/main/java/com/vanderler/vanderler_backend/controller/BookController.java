package com.vanderler.vanderler_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanderler.vanderler_backend.dto.BookDtos.BookContentResponse;
import com.vanderler.vanderler_backend.dto.BookDtos.BookCreateRequest;
import com.vanderler.vanderler_backend.dto.BookDtos.BookSummary;
import com.vanderler.vanderler_backend.service.BookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(origins = "https://vanderler.netlify.app")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookSummary>> listar() {
        return ResponseEntity.ok(bookService.listarTodos());
    }

    @PostMapping
    public ResponseEntity<BookSummary> criar(@RequestBody @Valid BookCreateRequest dto) {
        return ResponseEntity.ok(bookService.criar(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        bookService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<BookContentResponse> ler(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.lerConteudo(id));
    }
}
