package com.plee.library.repository.member;

import com.plee.library.domain.member.MemberLoanHistory;
import com.plee.library.dto.member.condition.LoanHistorySearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.time.LocalDateTime;
import java.util.List;

import static com.plee.library.domain.member.QMemberLoanHistory.memberLoanHistory;

public class MemberLoanHistoryCustomImpl implements MemberLoanHistoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberLoanHistoryCustomImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MemberLoanHistory> search(LoanHistorySearchCondition condition) {
        return getLoanHistoryByCondition(condition)
                .fetch();
    }

    private JPAQuery<MemberLoanHistory> getLoanHistoryByCondition(LoanHistorySearchCondition condition) {
        return queryFactory
                .selectFrom(memberLoanHistory)
                .where(
                        memberIdEq(condition.getMemberId()),
                        bookInfoIdEq(condition.getBookInfoId()),
                        timeEq(condition.getTime()),
                        notReturnedEq(condition.isNotReturned())
                );
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? memberLoanHistory.member.id.eq(memberId) : null;
    }

    private BooleanExpression bookInfoIdEq(String bookInfoId) {
        return bookInfoId != null ? memberLoanHistory.bookInfo.isbn.eq(bookInfoId) : null;
    }

    private BooleanExpression timeEq(LocalDateTime time) {
        return time != null ? memberLoanHistory.createdAt.before(time) : null;
    }

    private BooleanExpression notReturnedEq(boolean notReturned) {
        return notReturned ? memberLoanHistory.returnedAt.isNull() : null;
    }
}