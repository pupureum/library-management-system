package com.plee.library.domain.book;

import com.plee.library.domain.BaseTimeEntity;
import com.plee.library.util.message.BookMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "book")
public class Book extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_seq", updatable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_info_isbn", nullable = false)
    private BookInfo bookInfo;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "loanable_cnt", nullable = false)
    private int loanableCnt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_category_seq", nullable = false)
    private BookCategory bookCategory;

    @Builder
    public Book(Long id, BookInfo bookInfo, int quantity, BookCategory category) {
        this.id = id;
        this.bookInfo = bookInfo;
        this.quantity = quantity;
        this.loanableCnt = quantity;
        this.bookCategory = category;
    }

    public void setQuantity(int quantity) {
        // 기존 재고 수량과 차이 계산
        int diff = quantity - this.quantity;
        this.quantity = quantity;

        // 재고 수량이 감소한 경우
        if (diff < 0) {
            this.loanableCnt -= Math.abs(diff);
            return;
        }
        // 재고 수량이 증가한 경우
        this.loanableCnt += diff;
    }

    public void decreaseLoanableCnt() {
        if (this.loanableCnt < 1) {
            throw new IllegalStateException(BookMessage.CANNOT_LOAN_BOOK.getMessage());
        }
        this.loanableCnt -= 1;
    }

    public void increaseLoanableCnt() {
        if (this.loanableCnt >= this.quantity) {
            throw new IllegalStateException(BookMessage.INVALID_LOANABLE_CNT.getMessage());
        }
        this.loanableCnt += 1;
    }

    public void increaseLoanableCnt(int cnt) {
        if (this.loanableCnt + cnt > this.quantity) {
            throw new IllegalStateException(BookMessage.INVALID_LOANABLE_CNT.getMessage());
        }
        this.loanableCnt += cnt;
    }

    public void setBookInfo(BookInfo bookInfo) {
        this.bookInfo = bookInfo;
    }
}
