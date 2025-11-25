package com.vanderler.vanderler_backend.service;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.vanderler.vanderler_backend.dto.BookDtos.BookContentResponse;
import com.vanderler.vanderler_backend.dto.BookDtos.BookCreateRequest;
import com.vanderler.vanderler_backend.dto.BookDtos.BookSummary;
import com.vanderler.vanderler_backend.model.Book;
import com.vanderler.vanderler_backend.model.Role;
import com.vanderler.vanderler_backend.model.User;
import com.vanderler.vanderler_backend.model.UserBook;
import com.vanderler.vanderler_backend.repository.BookRepository;
import com.vanderler.vanderler_backend.repository.UserBookRepository;

import jakarta.transaction.Transactional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final UserBookRepository userBookRepository;
    private final UserService userService;

    public BookService(BookRepository bookRepository,
                       UserBookRepository userBookRepository,
                       UserService userService) {
        this.bookRepository = bookRepository;
        this.userBookRepository = userBookRepository;
        this.userService = userService;
    }

    public List<BookSummary> listarTodos() {
        return bookRepository.findAll().stream()
                .map(b -> new BookSummary(
                        b.getId(),
                        b.getTitulo(),
                        b.getAutor(),
                        b.getDescricao(),
                        b.getCategoria()
                ))
                .toList();
    }

    @Transactional
    public BookSummary criar(BookCreateRequest dto) {
        User user = userService.getUsuarioLogado();
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Apenas admin pode criar livros");
        }

        Book book = new Book();
        book.setTitulo(dto.titulo());
        book.setAutor(dto.autor());
        book.setDescricao(dto.descricao());
        book.setConteudo(dto.conteudo());
        book.setCategoria(dto.categoria());

        bookRepository.save(book);

        return new BookSummary(
                book.getId(),
                book.getTitulo(),
                book.getAutor(),
                book.getDescricao(),
                book.getCategoria()
        );
    }

    @Transactional
    public void remover(Long id) {
        User user = userService.getUsuarioLogado();
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Apenas admin pode remover livros");
        }

        if (!bookRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "Livro não encontrado");
        }

        bookRepository.deleteById(id);
    }

    public BookContentResponse lerConteudo(Long id) {
        User user = userService.getUsuarioLogado();
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Livro não encontrado"));

        if (user.getRole() != Role.ADMIN) {
            boolean has = userBookRepository.existsByUserAndBook(user, book);
            if (!has) {
                throw new ResponseStatusException(FORBIDDEN, "Você não possui esse livro na sua biblioteca");
            }
        }

        return new BookContentResponse(
                book.getId(),
                book.getTitulo(),
                book.getAutor(),
                book.getConteudo(),
                book.getCategoria()
        );
    }

    @Transactional
    public void adicionarNaMinhaBiblioteca(Long bookId) {
        User user = userService.getUsuarioLogado();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Livro não encontrado"));

        if (userBookRepository.existsByUserAndBook(user, book)) {
            return;
        }

        UserBook ub = new UserBook();
        ub.setUser(user);
        ub.setBook(book);
        ub.setDataAdicao(LocalDateTime.now());

        userBookRepository.save(ub);
    }

    @Transactional
    public void removerDaMinhaBiblioteca(Long bookId) {
        User user = userService.getUsuarioLogado();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Livro não encontrado"));

        UserBook ub = userBookRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Livro não está na sua biblioteca"));

        userBookRepository.delete(ub);
    }

    public List<BookSummary> minhaBiblioteca() {
        User user = userService.getUsuarioLogado();
        return userBookRepository.findByUser(user).stream()
                .map(UserBook::getBook)
                .map(b -> new BookSummary(
                        b.getId(),
                        b.getTitulo(),
                        b.getAutor(),
                        b.getDescricao(),
                        b.getCategoria()
                ))
                .toList();
    }
}
