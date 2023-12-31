package com.plee.library.repository.member;

import com.plee.library.domain.member.MemberBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberBookmarkRepository extends JpaRepository<MemberBookmark, Long> {
    Page<MemberBookmark> findAllByMemberId(Long memberId, Pageable pageable);

    List<MemberBookmark> findAllByBookId(Long bookId);

    boolean existsByMemberIdAndBookId(Long memberId, Long bookId);

    void deleteByMemberIdAndBookId(Long memberId, Long bookId);
}
