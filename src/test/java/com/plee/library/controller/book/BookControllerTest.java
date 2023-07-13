package com.plee.library.controller.book;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plee.library.config.TestUserDetailsConfig;
import com.plee.library.domain.book.Book;
import com.plee.library.domain.book.BookInfo;
import com.plee.library.dto.book.request.AddBookRequest;
import com.plee.library.dto.book.request.ReturnBookRequest;
import com.plee.library.dto.book.request.SearchBookRequest;
import com.plee.library.dto.book.response.AllBooksMarkInfoResponse;
import com.plee.library.dto.book.response.BookDetailResponse;
import com.plee.library.message.BookMsg;
import com.plee.library.service.book.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@Import(TestUserDetailsConfig.class)
@ExtendWith(MockitoExtension.class)
@WebMvcTest(BookController.class)
@DisplayName("BookController 테스트")
class BookControllerTest {

    @MockBean
    private BookService bookService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext applicationContext) {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .addFilter(new CharacterEncodingFilter(StandardCharsets.UTF_8.name(), true))
                .build();
    }

    @Test
    @WithUserDetails
    @DisplayName("GET 전체 도서 목록 페이지를 반환")
    void allBooks() throws Exception {
        // given
        BookInfo bookInfo = BookInfo
                .builder()
                .isbn("9788998274792")
                .title("이것이 자바다")
                .author("신용권")
                .build();

        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AllBooksMarkInfoResponse> res = Arrays.asList(
                AllBooksMarkInfoResponse.builder()
                        .id(1L)
                        .bookInfo(bookInfo)
                        .isMarked(true)
                        .build()
        );
        Page<AllBooksMarkInfoResponse> pageRes = new PageImpl<>(res, pageable, res.size());

        given(bookService.findAllBooksWithMark(1L, pageable)).willReturn(pageRes);

        // when, then
        mockMvc.perform(get("/books")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("book/bookList"))
                .andExpect(model().attribute("books", pageRes))
                .andExpect(model().attribute("selectedMenu", "book-list"));
    }

    @Nested
    @WithUserDetails
    @DisplayName("GET 도서 상세 페이지 요청")
    class BookDetailTest {
        private Book book;

        @BeforeEach
        void setUp() {
            BookInfo bookInfo = BookInfo
                    .builder()
                    .isbn("9788998274792")
                    .title("이것이 자바다")
                    .author("신용권")
                    .build();
            book = Book.builder()
                    .id(1L)
                    .bookInfo(bookInfo)
                    .build();
        }
        @Test
        @DisplayName("도서정보가 있는 경우")
        void bookDetail_success() throws Exception {
            // given
            BookDetailResponse res = BookDetailResponse.of(book, true, true);
            given(bookService.getBookDetails(anyLong(), anyLong())).willReturn(res);

            // when, then
            mockMvc.perform(get("/books/{bookId}", book.getId())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("book/bookDetail"))
                    .andExpect(model().attribute("response", res));
        }

        @Test
        @DisplayName("실패: 없는 도서인 경우")
        void bookDetail_notFound() throws Exception {
            // given
            String errorMessage = BookMsg.NOT_FOUND_BOOK.getMessage();
            BookDetailResponse res = BookDetailResponse.of(book, true, true);
            given(bookService.getBookDetails(anyLong(), anyLong())).willThrow(new NoSuchElementException(errorMessage));

            // when, then
            mockMvc.perform(get("/books/{bookId}", 1L))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books"))
                    .andExpect(flash().attribute("errorMessage", errorMessage));
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("GET 도서 검색 결과 반환")
    class SearchBookByKeywordTest {
        @Test
        @DisplayName("검색어가 존재하는 경우")
        void searchBookByKeyword_fail() throws Exception {
            // given
            given(bookService.findBySearchKeyword(any(SearchBookRequest.class), anyLong(), any(Pageable.class))).willReturn(Page.empty());

            // when, then
            mockMvc.perform(get("/books/search")
                            .param("page", "1")
                            .param("keyword", "keyword")
                            .flashAttr("searchBookRequest", new SearchBookRequest()))
                    .andExpect(status().isOk())
                    .andExpect(view().name("book/searchBookList"))
                    .andExpect(model().attributeExists("books"));
        }

        @Test
        @DisplayName("실패: 검색어가 없는 경우")
        void searchBookByKeyword() throws Exception {
            // given
            // 키워드 정보 없는 검색 요청 dto 생성
            SearchBookRequest failReq = SearchBookRequest
                    .builder()
                    .keyword(" ")
                    .build();

            // when, then
            MvcResult result = mockMvc.perform(get("/books/search")
                            .param("page", "0")
                            .flashAttr("searchBookRequest", failReq))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books?page=0"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", "검색어를 입력해주세요."))
                    .andReturn();
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("POST 도서 대출 요청")
    class LoanBookTest {
        @Test
        @DisplayName("대출 성공")
        void loanBook_success() throws Exception {
            // when, then
            mockMvc.perform(post("/books/loan")
                            .param("bookId", "1")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/loan"))
                    .andExpect(flash().attributeExists("successMessage"))
                    .andExpect(flash().attribute("successMessage", BookMsg.SUCCESS_LOAN_BOOK.getMessage()));
        }

        @Test
        @DisplayName("실패: 대출 불가능한 경우")
        void loanBook_fail() throws Exception {
            // given
            willThrow(new NoSuchElementException(BookMsg.NOT_FOUND_BOOK.getMessage())).given(bookService).loanBook(anyLong(), anyLong());

            // when, then
            mockMvc.perform(post("/books/loan")
                            .param("bookId", "1")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/loan"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", BookMsg.NOT_FOUND_BOOK.getMessage()));
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("PUT 도서 반납 요청")
    class ReturnBookTest {
        @Test
        @DisplayName("반납 성공")
        void returnBook_success() throws Exception {
            // given
            ReturnBookRequest req = ReturnBookRequest.builder()
                    .historyId(1L)
                    .status(true) // 대출중인 도서만 보기 페이지로 리다이렉트 되도록 true로 설정
                    .build();

            // when, then
            mockMvc.perform(put("/books/return")
                            .with(csrf())
                            .flashAttr("returnBookRequest", req))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/on-loan"))
                    .andExpect(flash().attributeExists("successMessage"))
                    .andExpect(flash().attribute("successMessage", BookMsg.SUCCESS_RETURN_BOOK.getMessage()));
        }

        @Test
        @DisplayName("실패: 반납 불가능한 경우")
        void returnBook_fail() throws Exception {
            // given
            ReturnBookRequest req = ReturnBookRequest.builder()
                    .historyId(1L)
                    .status(false)
                    .build();
            willThrow(new NoSuchElementException(BookMsg.NOT_FOUND_LOAN_HISTORY.getMessage())).given(bookService).returnBook(eq(req), anyLong());

            // when, then
            mockMvc.perform(put("/books/return")
                            .with(csrf())
                            .flashAttr("returnBookRequest", req))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/loan"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", BookMsg.NOT_FOUND_LOAN_HISTORY.getMessage()));
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("PUT 도서 연장 요청")
    class RenewBookTest {
        @Test
        @DisplayName("연장 성공")
        void renewBook_success() throws Exception {
            // when, then
            mockMvc.perform(put("/books/renewal")
                            .param("historyId", "1")
                            .param("status", "true")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/on-loan"))
                    .andExpect(flash().attributeExists("successMessage"))
                    .andExpect(flash().attribute("successMessage", BookMsg.SUCCESS_RENEW_BOOK.getMessage()));

        }

        @Test
        @DisplayName("실패: 반납 불가능한 경우")
        void renewBook_fail() throws Exception {
            // given
            willThrow(new NoSuchElementException(BookMsg.NOT_FOUND_LOAN_HISTORY.getMessage())).given(bookService).renewBook(anyLong());

            // when, then
            mockMvc.perform(put("/books/renewal")
                            .param("historyId", "1")
                            .param("status", "false")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/loan"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", BookMsg.NOT_FOUND_LOAN_HISTORY.getMessage()));
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("POST 신규 도서 요청")
    class RequestNewBookTest {
        @Test
        void requestNewBook() throws Exception {
            // given
            AddBookRequest request = AddBookRequest.builder()
                    .title("테스트")
                    .isbn("9791169210027")
                    .reqReason("test reason")
                    .build();
            // when, then
            mockMvc.perform(post("/books/request")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk());
        }
    }

    private String asJsonString(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }


    @Nested
    @WithUserDetails
    @DisplayName("POST 도서 찜 추가 요청")
    class AddBookMarkTest {
        @Test
        @DisplayName("찜 추가 성공 (도서 상세 페이지에서 요청한 경우)")
        void addBookmark() throws Exception {
            // given
            Long bookId = 1L;

            // when, then
            mockMvc.perform(post("/books/like/{bookId}", bookId)
                            .param("page", "0")
                            .param("pageInfo", "bookDetail")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/" + bookId));
        }

        @Test
        @DisplayName("실패: 이미 찜한 도서인 경우 (전체 도서 목록 페이지에서 요청한 경우)")
        void addBookmark_fail() throws Exception {
            // given
            Long bookId = 1L;
            willThrow(new IllegalStateException(BookMsg.ALREADY_BOOKMARK.getMessage()))
                    .given(bookService).addBookmark(anyLong(), anyLong());

            // when, then
            mockMvc.perform(post("/books/like/{bookId}", bookId)
                            .param("page", "0")
                            .param("pageInfo", "bookList")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books?page=0"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", BookMsg.ALREADY_BOOKMARK.getMessage()));
        }
    }

    @Nested
    @WithUserDetails
    @DisplayName("DELETE 도서 찜 해제 요청")
    class RemoveBookmark {
        @Test
        @DisplayName("찜 해제 성공 (도서 상세 페이지에서 요청한 경우)")
        void removeBookmark() throws Exception {
            // given
            Long bookId = 1L;

            // when, then
            mockMvc.perform(delete("/books/unlike/{bookId}", bookId)
                            .param("page", "0")
                            .param("pageInfo", "bookDetail")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/" + bookId));
        }

        @Test
        @DisplayName("실패: 찜하지 않은 경우 (찜 목록 페이지에서 요청한 경우)")
        void removeBookmark_fail() throws Exception {
            // given
            willThrow(new IllegalStateException(BookMsg.NOT_FOUND_BOOKMARK.getMessage()))
                    .given(bookService).removeBookmark(anyLong(), anyLong());

            // when, then
            mockMvc.perform(delete("/books/unlike/{bookId}", 1L)
                            .param("page", "0")
                            .param("pageInfo", "bookmarkList")
                            .with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/books/like?page=0"))
                    .andExpect(flash().attributeExists("errorMessage"))
                    .andExpect(flash().attribute("errorMessage", BookMsg.NOT_FOUND_BOOKMARK.getMessage()));
        }
    }
}