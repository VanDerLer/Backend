package com.vanderler.vanderler_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vanderler.vanderler_backend.model.Book;
import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.model.UserBook;

public interface UserBookRepository extends JpaRepository<UserBook, Long> {

    boolean existsByUserAndBook(User user, Book book);

    Optional<UserBook> findByUserAndBook(User user, Book book);

    List<UserBook> findByUser(User user);
}
