package com.plee.library.controller.admin;

import com.plee.library.domain.member.Role;
import com.plee.library.dto.book.request.SaveBookRequest;
import com.plee.library.dto.book.response.SearchBookResponse;
import com.plee.library.service.book.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final BookService bookService;

    @GetMapping("/new-book")
    public String addBook(Model model) {
        model.addAttribute("selectedMenu", "admin-new-book");
        return "admin/addBook";
    }

    @PostMapping("/new-book")
    public ResponseEntity<String> addBook(@Valid SaveBookRequest request) {
        bookService.saveBook(request);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/api/book")
    @ResponseBody
    public SearchBookResponse searchBooksByApi(@RequestParam("keyword") String keyword) {
        SearchBookResponse response = bookService.findBySearchApi(keyword);
        return response;
    }

    @GetMapping("/books")
    public String getAllBooks(Model model) {
        model.addAttribute("books", bookService.findAllBooks());
        model.addAttribute("selectedMenu", "admin-book-list");
        return "admin/bookList";
    }

    @PutMapping("/books/{bookId}")
    public ResponseEntity<String> updateBookQuantity(@PathVariable Long bookId, @RequestParam Integer quantity) throws Exception {
        log.info("bookId: {}, quantity: {}", bookId, quantity);
        bookService.updateBookQuantity(bookId, quantity);
        return ResponseEntity.ok("Success");
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<String> deleteBook(@PathVariable Long bookId) {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok("Success");
    }

    @GetMapping("/loan-status")
    public String getLoanStatus(Model model) {
        model.addAttribute("selectedMenu", "admin-loan-status");
        return "admin/loanStatus";
    }

    @GetMapping("/request-status")
    public String getRequestStatus(Model model) {
        model.addAttribute("isAdmin", true);
        model.addAttribute("selectedMenu", "admin-request-status");
        return "admin/requestList";
    }

    @GetMapping("/members")
    public String manageUserForm(Model model) {
        model.addAttribute("isAdmin", true);
        model.addAttribute("selectedMenu", "admin-member-management");
        model.addAttribute("roleTypes", Role.values());
//        TestUserDto test1 = new TestUserDto("1", "test1", "test1@gmail.com", Role.Member);
//        TestUserDto test2 = new TestUserDto("2", "test2", "test2@gmail.com", Role.Admin);
//        List<TestUserDto> users = List.of(test1, test2);
//        model.addAttribute("users", users);
        return "admin/userManagement";
    }
}