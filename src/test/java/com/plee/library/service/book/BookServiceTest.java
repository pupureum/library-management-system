package com.plee.library.service.book;

import com.plee.library.domain.book.Book;
import com.plee.library.domain.book.BookCategory;
import com.plee.library.domain.book.BookInfo;
import com.plee.library.domain.member.Member;
import com.plee.library.domain.member.MemberBookmark;
import com.plee.library.domain.member.MemberLoanHistory;
import com.plee.library.dto.admin.request.SearchBookRequest;
import com.plee.library.dto.admin.request.UpdateBookRequest;
import com.plee.library.dto.admin.response.BooksResponse;
import com.plee.library.dto.admin.response.LoanStatusResponse;
import com.plee.library.dto.admin.response.LoanDailyStatusResponse;
import com.plee.library.dto.book.condition.BookSearchCondition;
import com.plee.library.dto.book.request.AddBookRequest;
import com.plee.library.dto.book.request.ReturnBookRequest;
import com.plee.library.dto.book.request.SaveBookRequest;
import com.plee.library.dto.book.request.SearchKeywordBookRequest;
import com.plee.library.dto.book.response.BooksMarkResponse;
import com.plee.library.dto.book.response.BookInfoResponse;
import com.plee.library.dto.book.response.LoanHistoryResponse;
import com.plee.library.dto.member.condition.LoanHistorySearchCondition;
import com.plee.library.repository.book.BookCategoryRepository;
import com.plee.library.util.message.BookMessage;
import com.plee.library.repository.book.BookInfoRepository;
import com.plee.library.repository.book.BookRepository;
import com.plee.library.repository.member.MemberBookmarkRepository;
import com.plee.library.repository.member.MemberLoanHistoryRepository;
import com.plee.library.repository.member.MemberRepository;
import com.plee.library.repository.member.MemberRequestHistoryRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.test.context.support.WithUserDetails;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService 테스트")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookInfoRepository bookInfoRepository;
    @Mock
    private BookCategoryRepository bookCategoryRepository;
    @Mock
    private MemberRequestHistoryRepository memberReqHisRepository;
    @Mock
    private MemberBookmarkRepository memberBookmarkRepository;
    @Mock
    private MemberLoanHistoryRepository memberLoanHisRepository;
    @Mock
    private MemberRepository memberRepository;
    @InjectMocks
    private BookServiceImpl bookService;

    private BookInfo bookInfo;
    private Book book;
    private Member member;

    private BookCategory category;

    @BeforeEach
    void setUp() {
        bookInfo = BookInfo.builder()
                .isbn("9413465673518")
                .title("title")
                .author("author")
                .publisher("publisher")
                .build();

        member = Member.builder()
                .loginId("plee@gmail.com")
                .password("password")
                .build();

        category = new BookCategory(1L, "category");

        book = Book.builder()
                .bookInfo(bookInfo)
                .quantity(2)
                .category(category)
                .build();
    }

    @Nested
    @DisplayName("도서 저장")
    class SaveBookTest {
        private SaveBookRequest req;
        private BookCategory category;

        @BeforeEach
        void setUp() {
            req = SaveBookRequest.builder()
                    .isbn("9413465673518")
                    .title("title")
                    .author("author")
                    .publisher("publisher")
                    .quantity(2)
                    .categoryId(1L)
                    .build();
            category = new BookCategory(1L, "category");
        }

        @Test
        @DisplayName("도서 요청이 있던 도서인 경우")
        void save_RequestExist() {
            // given
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(false);
            given(bookInfoRepository.findById(anyString())).willReturn(Optional.of(bookInfo));
            given(memberReqHisRepository.existsByBookInfoIsbnAndIsApprovedFalse(anyString())).willReturn(true);
            given(bookCategoryRepository.findById(anyLong())).willReturn(Optional.of(category));

            // when
            bookService.saveBook(req);

            // then
            then(memberReqHisRepository).should(times(1)).approveByBookInfoIsbn(req.getIsbn());
            // 도서 정보 저장은 호출되면 X
            then(bookInfoRepository).should(never()).save(any(BookInfo.class));
            then(bookRepository).should(times(1)).save(any());
        }

        @DisplayName("도서 요청이 없던 도서인 경우")
        @Test
        void save_RequestNotExist() {
            // given
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(false);
            given(bookInfoRepository.findById(anyString())).willReturn(Optional.ofNullable(null));
            given(bookInfoRepository.save(any(BookInfo.class))).willReturn(bookInfo);
            given(bookCategoryRepository.findById(anyLong())).willReturn(Optional.of(category));

            // when
            bookService.saveBook(req);

            // then
            then(bookInfoRepository).should(times(1)).save(any(BookInfo.class));
            then(bookRepository).should(times(1)).save(any(Book.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 도서인 경우")
        void saveBook_Fail() {
            // given
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(true);

            // when
            assertThatThrownBy(() -> bookService.saveBook(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_EXIST_BOOK.getMessage());
        }
    }

    @Nested
    @DisplayName("신규 도서 요청")
    class AddNewBookRequestTest {
        AddBookRequest req;

        @BeforeEach
        void setUp() {
            req = AddBookRequest.builder()
                    .isbn("9234754245321")
                    .title("title")
                    .author("author")
                    .publisher("publisher")
                    .reqReason("reason")
                    .build();
        }

        @Test
        @DisplayName("도서 정보가 없는 경우")
        void addNewBookRequest_NotExistBookInfo() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(false);
            given(memberReqHisRepository.existsByMemberIdAndBookInfoIsbn(member.getId(), req.getIsbn())).willReturn(false);
            given(bookInfoRepository.findById(anyString())).willReturn(Optional.empty());
            given(bookInfoRepository.save(any(BookInfo.class))).willReturn(req.toEntity());

            // when
            bookService.addNewBookRequest(req, 1L);

            // then
            then(bookInfoRepository).should(times(1)).save(any(BookInfo.class));
            assertThat(member.getMemberRequestHistories().size()).isEqualTo(1);
            assertThat(member.getMemberRequestHistories().get(0).getBookInfo().getIsbn()).isEqualTo(req.getIsbn());
        }

        @Test
        @DisplayName("도서 정보가 경우")
        void addNewBookRequest_ExistBookInfo() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(false);
            given(memberReqHisRepository.existsByMemberIdAndBookInfoIsbn(member.getId(), req.getIsbn())).willReturn(false);
            given(bookInfoRepository.findById(anyString())).willReturn(Optional.of(bookInfo));

            // when
            bookService.addNewBookRequest(req, 1L);

            // then
            then(bookInfoRepository).should(never()).save(any(BookInfo.class));
            assertThat(member.getMemberRequestHistories().size()).isEqualTo(1);
            assertThat(member.getMemberRequestHistories().get(0).getBookInfo().getIsbn()).isEqualTo(bookInfo.getIsbn());
        }

        @Test
        @DisplayName("실패: 이미 보유한 도서인 경우")
        void addNewBookRequest_FailAlreadyBookExist() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.existsByBookInfoIsbn(anyString())).willReturn(true);

            // when
            assertThatThrownBy(() -> bookService.addNewBookRequest(req, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_EXIST_BOOK.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 추가 요청한 도서인 경우")
        void addNewBookRequest_FailAlreadyReqExist() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.existsByBookInfoIsbn(req.getIsbn())).willReturn(false);
            given(memberReqHisRepository.existsByMemberIdAndBookInfoIsbn(member.getId(), req.getIsbn())).willReturn(true);

            // when
            assertThatThrownBy(() -> bookService.addNewBookRequest(req, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_BOOK_REQUEST.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 대출")
    class LoanBookTest {
        @Test
        @DisplayName("도서 대출 성공")
        void loanBook() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            given(memberLoanHisRepository.findByMemberIdAndBookInfoIsbnAndReturnedAtIsNull(member.getId(), bookInfo.getIsbn())).willReturn(Optional.empty());
            given(memberLoanHisRepository.countByMemberIdAndReturnedAtIsNull(member.getId())).willReturn(1L);

            // when
            bookService.loanBook(1L, 1L);

            // then
            assertThat(member.getMemberLoanHistories().size()).isEqualTo(1);
            assertThat(member.getMemberLoanHistories().get(0).getBookInfo().getIsbn()).isEqualTo(bookInfo.getIsbn());
            assertThat(book.getLoanableCnt()).isEqualTo(1);
        }

        @Test
        @DisplayName("실패: 대출 가능한 도서 수량이 없는 경우")
        void loanBook_FailLoanableCnt() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            book.decreaseLoanableCnt();
            book.decreaseLoanableCnt();

            // when, then
            assertThatThrownBy(() -> bookService.loanBook(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.CANNOT_LOAN_BOOK.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 대출한 도서인 경우")
        void loanBook_FailAlreadyLoan() {
            // given
            given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            MemberLoanHistory history = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
            given(memberLoanHisRepository.findByMemberIdAndBookInfoIsbnAndReturnedAtIsNull(eq(member.getId()), eq(bookInfo.getIsbn()))).willReturn(Optional.of(history));

            // when, then
            assertThatThrownBy(() -> bookService.loanBook(1L, 1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_LOAN_BOOK.getMessage());
        }

        @Test
        @DisplayName("실패: 최대 대출 수량인 3권을 대출중인 경우")
        void loanBook_FailExceedMax() {
            // given
            given(memberRepository.findById(member.getId())).willReturn(Optional.of(member));
            given(bookRepository.findById(book.getId())).willReturn(Optional.of(book));
            given(memberLoanHisRepository.findByMemberIdAndBookInfoIsbnAndReturnedAtIsNull(member.getId(), bookInfo.getIsbn()))
                    .willReturn(Optional.empty());
            given(memberLoanHisRepository.countByMemberIdAndReturnedAtIsNull(member.getId())).willReturn(3L);

            // when, then
            assertThatThrownBy(() -> bookService.loanBook(book.getId(), member.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.MAX_LOAN_BOOK.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 반납")
    class ReturnBookTest {
        private ReturnBookRequest req;
        private MemberLoanHistory history;

        @BeforeEach
        void setUp() {
            req = ReturnBookRequest.builder()
                    .historyId(1L)
                    .bookInfoIsbn(bookInfo.getIsbn())
                    .build();
            history = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
        }

        @Test
        @DisplayName("도서 반납 성공")
        void returnBook() {
            // given
            given(bookRepository.findByBookInfoIsbn(bookInfo.getIsbn())).willReturn(Optional.of(book));
            given(memberLoanHisRepository.findByMemberIdAndBookInfoIsbnAndReturnedAtIsNull(anyLong(), anyString()))
                    .willReturn(Optional.of(history));
            // 도서 대출 처리
            member.loanBook(book);
            book.decreaseLoanableCnt();

            // when
            bookService.returnBook(req, 1L);

            // then
            assertThat(book.getLoanableCnt()).isEqualTo(2);
            assertThat(member.getMemberLoanHistories().size()).isEqualTo(1);
            assertThat(member.getMemberLoanHistories().get(0).getBookInfo().getIsbn()).isEqualTo(bookInfo.getIsbn());
        }

        @Test
        @DisplayName("실패: 도서가 없는 경우")
        void returnBook_FailNotExistBook() {
            // given
            given(bookRepository.findByBookInfoIsbn(anyString())).willReturn(Optional.empty());

            // when, then
            assertThatThrownBy(() -> bookService.returnBook(req, 1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(BookMessage.NOT_FOUND_BOOK.getMessage());
        }

        @Test
        @DisplayName("실패: 대출 기록이 없는 경우")
        void returnBook_FailNotLoaned() {
            // given
            given(bookRepository.findByBookInfoIsbn(anyString())).willReturn(Optional.of(book));
            given(memberLoanHisRepository.findByMemberIdAndBookInfoIsbnAndReturnedAtIsNull(anyLong(), anyString()))
                    .willReturn(Optional.empty());

            // when, then
            assertThatThrownBy(() -> bookService.returnBook(req, 1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(BookMessage.NOT_FOUND_LOAN_HISTORY.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 연장")
    class RenewBookTest {
        private MemberLoanHistory history;

        @BeforeEach
        void setUp() {
            history = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
        }

        @Test
        @DisplayName("도서 연장 성공")
        void renewBook() {
            // given
            given(memberLoanHisRepository.findById(anyLong())).willReturn(Optional.of(history));

            // when
            bookService.renewBook(1L);

            // then
            assertThat(history.isRenewable()).isFalse();
        }

        @Test
        @DisplayName("실패: 대출내역이 없는 경우")
        void renewBook_FailNotFountHistory() {
            // given
            given(memberLoanHisRepository.findById(anyLong())).willReturn(Optional.ofNullable(null));

            // when, then
            assertThatThrownBy(() -> bookService.renewBook(1L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(BookMessage.NOT_FOUND_LOAN_HISTORY.getMessage());
        }

        @Test
        @DisplayName("실패: 대출중이 아닌 경우")
        void renewBook_FailAlreadyReturn() {
            // given
            given(memberLoanHisRepository.findById(anyLong())).willReturn(Optional.of(history));
            history.doReturn();

            // when, then
            assertThatThrownBy(() -> bookService.renewBook(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_RETURN_BOOK.getMessage());
        }

        @Test
        @DisplayName("실패: 이미 연장한 경우")
        void renewBook_failAlreadyRenew() {
            // given
            given(memberLoanHisRepository.findById(anyLong())).willReturn(Optional.of(history));
            history.doRenew();

            // when, then
            assertThatThrownBy(() -> bookService.renewBook(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BookMessage.ALREADY_RENEW_BOOK.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 수량 수정")
    class UpdateBookQuantityTest {

        @Test
        @DisplayName("도서 수량 수정 성공")
        void updateBookQuantity() {
            // given
            UpdateBookRequest req = new UpdateBookRequest(3, 1L,"",0);
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));

            // when
            bookService.updateBookQuantity(1L, req);

            // then
            assertThat(book.getQuantity()).isEqualTo(3);
            // 대출 가능한 수량도 함께 증가 했는지 확인
            assertThat(book.getLoanableCnt()).isEqualTo(3);
        }

        @Test
        @DisplayName("실패: 현재 수량과 같은 수량으로 수정하려는 경우")
        void updateBookQuantity_FailSameQuantity() {
            // given
            UpdateBookRequest req = new UpdateBookRequest(2, 1L,"",0);
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));

            // when, then
            assertThatThrownBy(() -> bookService.updateBookQuantity(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(BookMessage.CANNOT_UPDATE_SAME_QUANTITY.getMessage());
        }

        @Test
        @DisplayName("실패: 대출중인 도서 수보다 적은 수량으로 수정하려는 경우")
        void updateBookQuantity_FailQuantity() {
            // given
            UpdateBookRequest req = new UpdateBookRequest(1, 1L,"",0);
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            book.decreaseLoanableCnt();
            book.decreaseLoanableCnt();

            // when, then
            assertThatThrownBy(() -> bookService.updateBookQuantity(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(BookMessage.CANNOT_UPDATE_QUANTITY.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 삭제")
    class DeleteBookTest {
        @Test
        @DisplayName("어떤 이력도 없는 경우")
        void deleteBook() {
            // given
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));

            // when
            bookService.deleteBook(1L);

            // then
            then(bookRepository).should(times(1)).deleteById(anyLong());
            then(bookInfoRepository).should(times(1)).deleteById(anyString());
        }

        @Test
        @DisplayName("대출중인 도서가 있는 경우")
        void deleteBook_WithLoanedBook() {
            // given
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            // 대출 기록 생성
            MemberLoanHistory history = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
            book.decreaseLoanableCnt();
            given(memberLoanHisRepository.searchHistory(any(LoanHistorySearchCondition.class))).willReturn(List.of(history));
            given(memberLoanHisRepository.existsByBookInfoIsbn(anyString())).willReturn(true);

            // when
            bookService.deleteBook(1L);

            // then
            // 반납 처리 확인
            assertThat(history.isReturned()).isTrue();
            then(memberLoanHisRepository).should(times(1)).searchHistory(any(LoanHistorySearchCondition.class));

            // 찜 삭제 호출 안됨 확인
            then(memberBookmarkRepository).should(never()).deleteAll();

            // 도서 삭제 호출 확인
            then(bookRepository).should(times(1)).deleteById(anyLong());

            // 도서 정보 삭제 호출 안됨 확인
            then(bookInfoRepository).should(never()).deleteById(anyString());
        }

        @Test
        @DisplayName("대출 기록은 없고, 찜 및 도서 요청 기록이 있는 경우")
        void deleteBook_WithBookmark() {
            // given
            given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
            // 찜 기록 생성
            MemberBookmark memberBookmark = MemberBookmark.builder()
                    .member(member)
                    .book(book)
                    .build();

            given(memberBookmarkRepository.findAllByBookId(anyLong())).willReturn(List.of(memberBookmark));
            // 도서 요청 기록 유
            given(memberReqHisRepository.existsByBookInfoIsbn(anyString())).willReturn(true);

            // when
            bookService.deleteBook(1L);

            // then
            // 찜 기록 삭제 확인
            then(memberBookmarkRepository).should(times(1)).findAllByBookId(anyLong());
            then(memberBookmarkRepository).should(times(1)).deleteAll(List.of(memberBookmark));

            // 대출 기록 조회 호출 여부 확인
            then(memberLoanHisRepository).should(never()).searchHistory(any(LoanHistorySearchCondition.class));

            // 도서 삭제 호출 확인
            then(bookRepository).should(times(1)).deleteById(anyLong());

            // 도서 정보 삭제 호출 안됨 확인
            then(bookInfoRepository).should(never()).deleteById(anyString());
        }
    }

    @Nested
    @DisplayName("찜")
    class BookmarkTest {

        @Nested
        @DisplayName("찜 추가")
        class AddBookmarkTest {
            @Test
            @DisplayName("찜 추가 성공")
            void addBookmark() {
                // given
                given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
                given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));

                // when
                bookService.addBookmark(1L, 1L);

                // then
                assertThat(member.getMemberBookmarks().size()).isEqualTo(1);
                assertThat(member.getMemberBookmarks().contains(bookInfo));
            }

            @Test
            @DisplayName("실패: 도서가 없는 경우")
            void addBookmark_FailNotExistBook() {
                // given
                given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
                given(bookRepository.findById(anyLong())).willReturn(Optional.empty());

                // when, then
                assertThatThrownBy(() -> bookService.addBookmark(1L, 1L))
                        .isInstanceOf(NoSuchElementException.class)
                        .hasMessageContaining(BookMessage.NOT_FOUND_BOOK.getMessage());
            }

            @Test
            @DisplayName("실패: 이미 찜한 경우")
            void addBookmark_FailAlreadyBookmark() {
                // given
                given(memberRepository.findById(anyLong())).willReturn(Optional.of(member));
                given(bookRepository.findById(anyLong())).willReturn(Optional.of(book));
                given(memberBookmarkRepository.existsByMemberIdAndBookId(anyLong(), anyLong())).willReturn(true);

                // when, then
                assertThatThrownBy(() -> bookService.addBookmark(1L, 1L))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(BookMessage.ALREADY_BOOKMARK.getMessage());
            }
        }

        @Nested
        @DisplayName("찜 해제")
        class RemoveBookmarkTest {
            @Test
            @DisplayName("찜 해제 성공")
            void removeBookmark() {
                // given
                given(bookRepository.existsById(anyLong())).willReturn(true);
                given(memberBookmarkRepository.existsByMemberIdAndBookId(anyLong(), anyLong())).willReturn(true);

                // when
                bookService.removeBookmark(1L, 1L);

                // then
                assertThat(member.getMemberBookmarks().size()).isEqualTo(0);
                assertThat(member.getMemberBookmarks().contains(bookInfo)).isFalse();
                then(memberBookmarkRepository).should(times(1)).deleteByMemberIdAndBookId(anyLong(), anyLong());
            }

            @Test
            @DisplayName("실패: 도서가 없는 경우")
            void removeBookmark_FailNotExistBook() {
                // given
                given(bookRepository.existsById(anyLong())).willReturn(false);

                // when, then
                assertThatThrownBy(() -> bookService.removeBookmark(1L, 1L))
                        .isInstanceOf(NoSuchElementException.class)
                        .hasMessageContaining(BookMessage.NOT_FOUND_BOOK.getMessage());
            }

            @Test
            @DisplayName("실패: 찜하지 않은 경우")
            void removeBookmark_FailNotExistBookmark() {
                // given
                given(bookRepository.existsById(anyLong())).willReturn(true);
                given(memberBookmarkRepository.existsByMemberIdAndBookId(anyLong(), anyLong())).willReturn(false);

                // when, then
                assertThatThrownBy(() -> bookService.removeBookmark(1L, 1L))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining(BookMessage.NOT_FOUND_BOOKMARK.getMessage());
            }
        }
    }

    @Test
    @DisplayName("최근 5일간의 대출 수 계산")
    void calculateDailyLoanCounts() {
        // given
        List<Object[]> expectedData = new ArrayList<>();
        // 현재 날짜 기준으로 2일전, 4일전, 1건씩 데이터 생성
        expectedData.add(new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(4)), 1L});
        expectedData.add(new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(2)), 1L});
        expectedData.add(new Object[]{java.sql.Date.valueOf(LocalDate.now()), 2L});

        given(memberLoanHisRepository.countGroupByCreatedAtRange(
                eq(LocalDate.now().minusDays(4)), eq(LocalDate.now())))
                .willReturn(expectedData);

        // when
        LoanDailyStatusResponse result = bookService.calculateDailyLoanCounts();

        // then
        Map<LocalDate, Integer> expected = new HashMap<>();
        expected.put(LocalDate.now().minusDays(4), 1);
        expected.put(LocalDate.now().minusDays(3), 0);
        expected.put(LocalDate.now().minusDays(2), 1);
        expected.put(LocalDate.now().minusDays(1), 0);
        expected.put(LocalDate.now(), 2);
        assertThat(result.getDailyLoanData()).isEqualTo(expected);
    }


    @Test
    @DisplayName("키워드 검색")
    void findBySearchKeyword() {
        // given
        SearchKeywordBookRequest req = new SearchKeywordBookRequest("test", true, true, 0);
        // 검색 결과 생성
        List<Book> books = createBooks();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
        given(bookRepository.search(any(BookSearchCondition.class), any(Pageable.class))).willReturn(bookPage);
        // 찜 정보 생성
        given(memberBookmarkRepository.existsByMemberIdAndBookId(1L, null)).willReturn(false);

        // when
        Page<BooksMarkResponse> result = bookService.findBySearchKeyword(req, 1L, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        // 찜 정보 모두 false 확인
        assertThat(result.getContent()).allSatisfy(book -> assertThat(book.isMarked()).isEqualTo(false));
        then(bookRepository).should(times(1)).search(any(BookSearchCondition.class), any(Pageable.class));
        then(memberBookmarkRepository).should(times(4)).existsByMemberIdAndBookId(1L, null);
    }

    @Nested
    @WithUserDetails
    @DisplayName("관리자 사내 도서 목록 검색")
    class AdminSearchBooksTest {
        private BookCategory category;
        private SearchBookRequest request;

        @BeforeEach
        void setUp() {
            // 카테고리 및 검색 조건 생성
            category = BookCategory.builder()
                    .id(1L)
                    .categoryName("category")
                    .build();

            request = SearchBookRequest.builder()
                    .categoryId(category.getId())
                    .keyword("Java")
                    .build();
        }

        @Test
        @DisplayName("도서 검색 성공 (카테고리 조건과 키워드 모두 있는 경우)")
        void searchBooks_WithCategory() {
            // given
             given(bookCategoryRepository.findById(category.getId())).willReturn(Optional.of(category));

            // 검색 결과 도서 생성
            List<Book> books = createBooks();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(books, pageable, books.size());
            given(bookRepository.search(any(BookSearchCondition.class), any(Pageable.class))).willReturn(bookPage);

            // when
            Page<BooksResponse> result = bookService.searchBooks(request, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(4);
            then(bookRepository).should(times(1)).search(any(BookSearchCondition.class), any(Pageable.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 카테고리로 검색하려는 경우")
        void searchBooks_OnlyKeyword() {
            // given
            given(bookCategoryRepository.findById(category.getId())).willReturn(Optional.empty());

            // when, then
            assertThatThrownBy(() -> bookService.searchBooks(request, PageRequest.of(0, 10)))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(BookMessage.NOT_FOUND_CATEGORY.getMessage());
        }
    }

    @Test
    @DisplayName("최근 입고된 도서 4권 조회")
    void findNewBooks() {
        // given
        List<Book> books = createBooks();
        given(bookRepository.findTop4ByOrderByCreatedAtDesc()).willReturn(books);

        // when
        List<BookInfoResponse> result = bookService.findNewBooks();

        // then
        List<BookInfoResponse> expected = BookInfoResponse.from(books);
        assertThat(result.size()).isEqualTo(4);
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(bookRepository).should(times(1)).findTop4ByOrderByCreatedAtDesc();
    }

    // 도서 4권 생성
    private List<Book> createBooks() {
        BookCategory category = new BookCategory(1L, "category");
        List<Book> books = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BookInfo bookInfo = BookInfo.builder()
                    .isbn("isbn" + i)
                    .title("test" + i)
                    .author("author" + i)
                    .build();
            Book book = Book.builder()
                    .bookInfo(bookInfo)
                    .quantity(1)
                    .category(category)
                    .build();
            books.add(book);
        }
        return books;
    }

    @Test
    @DisplayName("전체 도서 조회")
    void findAllBooks() {
        // given
        List<Book> books = createBooks();
        books.add(book);
        // 4권의 도서만 페이지네이션하여 조회
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 4);
        given(bookRepository.findAll(pageable)).willReturn(bookPage);

        // when
        Page<BooksResponse> result = bookService.findBooks(pageable);

        // then
        List<BooksResponse> expected = BooksResponse.from(bookPage);
        // 전체 도서 수는 5권
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DisplayName("찜 정보와 함께 전체 도서 조회")
    void findAllBooksWithMark() {
        // given
        List<Book> books = createBooks();
        books.add(book);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(books, pageable, 4);
        given(bookRepository.findAll(pageable)).willReturn(bookPage);

        // 찜 정보 생성
        given(memberBookmarkRepository.existsByMemberIdAndBookId(1L, null)).willReturn(true);

        // when
        Page<BooksMarkResponse> result = bookService.findBooksWithMark(1L, pageable);

        // then
        // 전체 도서 수는 5권
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getContent()).allSatisfy(book -> assertThat(book.isMarked()).isEqualTo(true));
    }

    @Nested
    @WithUserDetails
    @DisplayName("특정 카테고리 도서 검색")
    class FindBooksByCategoryWithMarkTest {
        @Test
        @DisplayName("해당 카테고리의 도서 정보와 회원의 찜 정보 조회 성공")
        void findBooksByCategoryWithMark_Success() {
            // given
            Long categoryId = 1L;
            given(bookCategoryRepository.existsById(categoryId)).willReturn(true);

            // 도서 조회 결과 생성
            List<Book> books = createBooks();
            books.add(book);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Book> bookPage = new PageImpl<>(books, pageable, 4);
            given(bookRepository.search(any(BookSearchCondition.class), any(Pageable.class))).willReturn(bookPage);

            // 찜 정보 생성
            given(memberBookmarkRepository.existsByMemberIdAndBookId(1L, null)).willReturn(true);

            // when
            Page<BooksMarkResponse> result = bookService.findBooksByCategoryWithMark(1L, 1L, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getContent()).allSatisfy(book -> assertThat(book.getBookCategory().getId()).isEqualTo(category.getId()));
            assertThat(result.getContent()).allSatisfy(book -> assertThat(book.getBookCategory().getCategoryName()).isEqualTo(category.getCategoryName()));
            assertThat(result.getContent()).allSatisfy(book -> assertThat(book.isMarked()).isEqualTo(true));

            then(bookCategoryRepository).should(times(1)).existsById(categoryId);
            then(bookRepository).should(times(1)).search(any(BookSearchCondition.class), any(Pageable.class));
            then(memberBookmarkRepository).should(times(5)).existsByMemberIdAndBookId(1L, null);
        }

        @Test
        @DisplayName("실패: 해당 카테고리 없는 경우")
        void findBooksByCategoryWithMark_Fail() {
            // given
            Long categoryId = 1L;
            given(bookCategoryRepository.existsById(categoryId)).willThrow(new NoSuchElementException(BookMessage.NOT_FOUND_CATEGORY.getMessage()));

            // when, then
            assertThatThrownBy(() -> bookService.findBooksByCategoryWithMark(categoryId, 1L, any(Pageable.class)))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining(BookMessage.NOT_FOUND_CATEGORY.getMessage());
        }
    }

    @Nested
    @DisplayName("도서 대출 이력 조회")
    class FindLoanHistoryTest {
        private MemberLoanHistory memberLoanHis;
        private MemberLoanHistory memberLoanHis2;

        @BeforeEach
        void setUp() {
            memberLoanHis = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
            memberLoanHis2 = MemberLoanHistory.builder()
                    .member(member)
                    .bookInfo(bookInfo)
                    .build();
        }

        @Test
        @DisplayName("모든 이력 조회")
        void findAllLoanHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<MemberLoanHistory> histories = new PageImpl<>(Arrays.asList(memberLoanHis, memberLoanHis2), pageable, 2);

            given(memberLoanHisRepository.findAll(pageable)).willReturn(histories);

            // when
            Page<LoanStatusResponse> result = bookService.findAllLoanHistory(pageable);

            // then
            List<LoanStatusResponse> expectedResponse = LoanStatusResponse.from(histories);

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("특정 회원 이력 조회")
        void findLoanHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<MemberLoanHistory> histories = new PageImpl<>(Arrays.asList(memberLoanHis, memberLoanHis2), pageable, 2);

            given(memberLoanHisRepository.findAllByMemberId(anyLong(), any(Pageable.class))).willReturn(histories);

            // when
            Page<LoanHistoryResponse> result = bookService.findLoanHistory(1L, pageable);

            // then
            List<LoanHistoryResponse> expectedResponse = LoanHistoryResponse.from(histories.getContent());
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("특정 회원의 대출중인 이력만 조회")
        void findOnLoanHistory() {
            // given
            List<MemberLoanHistory> histories = Arrays.asList(memberLoanHis, memberLoanHis2);
            given(memberLoanHisRepository.searchHistory(any(LoanHistorySearchCondition.class))).willReturn(histories);

            // when
            Page<LoanHistoryResponse> result = bookService.findOnLoanHistory(1L);

            // then
            List<LoanHistoryResponse> expectedResponse = LoanHistoryResponse.from(histories);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).usingRecursiveComparison().isEqualTo(expectedResponse);
        }

        @Test
        @DisplayName("대출중인 이력이 없는 경우, 대출중인 이력만 조회")
        void findOnLoanHistory_Empty() {
            // given
            given(memberLoanHisRepository.searchHistory(any(LoanHistorySearchCondition.class))).willReturn(Collections.emptyList());

            // when
            Page<LoanHistoryResponse> result = bookService.findOnLoanHistory(1L);

            // then
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }
    }
}