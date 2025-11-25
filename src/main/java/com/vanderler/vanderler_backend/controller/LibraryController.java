package com.vanderler.vanderler_backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vanderler.vanderler_backend.dto.BookDtos.BookSummary;
import com.vanderler.vanderler_backend.service.BookService;

@RestController
@RequestMapping("/api/library")
@CrossOrigin(origins = "http://localhost:5173")
public class LibraryController {

    private final BookService bookService;

    public LibraryController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<List<BookSummary>> minhaBiblioteca() {
        return ResponseEntity.ok(bookService.minhaBiblioteca());
    }

    @PostMapping("/{bookId}")
    public ResponseEntity<Void> adicionar(@PathVariable Long bookId) {
        bookService.adicionarNaMinhaBiblioteca(bookId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> remover(@PathVariable Long bookId) {
        bookService.removerDaMinhaBiblioteca(bookId);
        return ResponseEntity.noContent().build();
    }
}
