package com.plee.library.util.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BookMessage implements Message {
    SUCCESS_ADD_BOOK("도서 입고가 성공적으로 처리되었습니다."),
    SUCCESS_LOAN_BOOK("대출이 성공적으로 처리되었습니다."),
    SUCCESS_RETURN_BOOK("반납이 성공적으로 처리되었습니다."),
    SUCCESS_RENEW_BOOK("연장이 성공적으로 처리되었습니다."),
    SUCCESS_UPDATE_QUANTITY("수량이 변경되었습니다."),
    SUCCESS_DELETE_BOOK("도서가 삭제되었습니다."),
    NOT_FOUND_BOOK_INFO("책 정보를 찾을 수 없습니다."),
    NOT_FOUND_BOOK("해당 도서를 찾을 수 없습니다."),
    NOT_FOUND_CATEGORY("해당 카테고리를 찾을 수 없습니다."),
    NOT_FOUND_LOAN_HISTORY("대출 내역이 없습니다."),
    NOT_FOUND_BOOKMARK("찜한 도서가 아닙니다."),
    ALREADY_EXIST_BOOK("이미 존재하는 책입니다."),
    ALREADY_BOOKMARK("이미 찜한 도서입니다."),
    ALREADY_BOOK_REQUEST("이미 해당 도서의 추가 요청을 하셨습니다."),
    ALREADY_RENEW_BOOK("대출 기간 연장은 1회만 가능합니다."),
    ALREADY_LOAN_BOOK("이미 대출중인 도서입니다."),
    ALREADY_RETURN_BOOK("대출중인 도서가 아닙니다."),
    CANNOT_LOAN_BOOK("모두 대출중으로 대출 가능한 도서가 없습니다."),
    CANNOT_UPDATE_QUANTITY("대출중인 도서 수보다 적은 수량으로 수정할 수 없습니다."),
    CANNOT_UPDATE_SAME_QUANTITY("현재 수량과 같은 수량으로 수정할 수 없습니다."),
    INVALID_LOANABLE_CNT("대여 가능한 수량이 올바르지 않습니다."),
    API_ERROR("네이버 도서 검색에 오류가 발생했습니다. 잠시 후에 다시 시도해주세요."),
    MAX_LOAN_BOOK("대출 가능한 도서의 수를 초과하였습니다. 최대 3권까지 가능합니다.");

    private final String message;
}
