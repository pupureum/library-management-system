package com.plee.library.service.book;

import com.plee.library.dto.admin.request.SearchBookRequest;
import com.plee.library.dto.admin.request.UpdateBookRequest;
import com.plee.library.dto.admin.response.BooksResponse;
import com.plee.library.dto.admin.response.LoanStatusResponse;
import com.plee.library.dto.admin.response.RequestStatusResponse;
import com.plee.library.dto.admin.response.LoanDailyStatusResponse;
import com.plee.library.dto.book.request.*;
import com.plee.library.dto.book.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface BookService {
    void saveBook(SaveBookRequest request);

    void addNewBookRequest(AddBookRequest request, Long memberId);

    void loanBook(Long bookId, Long memberId);

    void renewBook(Long historyId);

    void returnBook(ReturnBookRequest request, Long memberId);

    int processDailyBookReturn(LocalDateTime scheduledDateTime);

    void updateBookQuantity(Long bookId, UpdateBookRequest request);

    void deleteBook(Long bookId);

    void addBookmark(Long memberId, Long bookId);

    void removeBookmark(Long memberId, Long bookId);

    LoanDailyStatusResponse calculateDailyLoanCounts();

    Page<BooksMarkResponse> findBySearchKeyword(SearchKeywordBookRequest request, Long memberId, Pageable pageable);

    List<BookInfoResponse> findNewBooks();

    BookDetailResponse getBookDetails(Long memberId, Long bookId);

    List<CategoryResponse> findCategories();

    Page<BooksResponse> findBooks(Pageable pageable);

    Page<BooksResponse> searchBooks(SearchBookRequest request, Pageable pageable);

    Page<BooksResponse> findBooksByCategory(Long categoryId, Pageable pageable);

    Page<BooksMarkResponse> findBooksWithMark(Long memberId, Pageable pageable);

    Page<BooksMarkResponse> findBooksByCategoryWithMark(Long categoryId, Long memberId , Pageable pageable);

    Page<LoanHistoryResponse> findLoanHistory(Long memberId, Pageable pageable);

    Page<LoanHistoryResponse> findOnLoanHistory(Long memberId);

    Page<LoanStatusResponse> findAllLoanHistory(Pageable pageable);

    Page<RequestHistoryResponse> findMemberRequestHistory(Long memberId, Pageable pageable);

    SearchBookResponse findBySearchApi(String keyword);

    Page<RequestStatusResponse> findAllNewBookReqHistory(Pageable pageable);

    Page<MarkedBooksResponse> findBookmarked(Long memberId, Pageable pageable);
}