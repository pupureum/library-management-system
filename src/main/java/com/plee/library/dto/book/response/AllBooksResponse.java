package com.plee.library.dto.book.response;

import com.plee.library.domain.book.BookInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AllBooksResponse {
    private final Long id;
    private final int quantity;
    private final int loanableCnt;
    private final BookInfo bookInfo;

    @Builder
    public AllBooksResponse(Long id, int quantity, int loanableCnt, BookInfo bookInfo) {
        this.id = id;
        this.quantity = quantity;
        this.loanableCnt = loanableCnt;
        this.bookInfo = bookInfo;
    }

    public static AllBooksResponse of(Long id, int quantity, int loanableCnt, BookInfo bookInfo) {
        return AllBooksResponse.builder()
                .id(id)
                .quantity(quantity)
                .loanableCnt(loanableCnt)
                .bookInfo(bookInfo)
                .build();
    }
}
