package com.vanderler.vanderler_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanderler.vanderler_backend.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {
}
