package com.plee.library.dto.book.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class BookSearchCondition {
    private String keyword;
    private boolean title;
    private boolean author;
    private Long categoryId;
}
