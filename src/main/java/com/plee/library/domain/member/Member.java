package com.plee.library.domain.member;

import com.plee.library.domain.book.BookInfo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DynamicInsert
@Table(name = "member", uniqueConstraints = {@UniqueConstraint(name = "login_id_unique", columnNames = {"login_id"})})
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_seq", updatable = false)
    private Long id;

    @Column(name = "name", length = 25, nullable = false)
    private String name;

    @Column(name = "login_id", length = 40, nullable = false)
    private String loginId;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @ColumnDefault("'MEMBER'")
    @Column(name = "role", nullable = false)
    private Role role;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberLoanHistory> memberLoanHistories = new ArrayList<MemberLoanHistory>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberRequestHistory> memberRequestHistories = new ArrayList<MemberRequestHistory>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberBookmark> memberBookmarks = new ArrayList<MemberBookmark>();

    // TODO 수정 필요
//    @Column(name = "penalty_end_at")
//    private LocalDate penaltyEndAt;

    @Builder
    public Member(String name, String loginId, String password) {
        this.name = name;
        this.loginId = loginId;
        this.password = password;
    }

    public void addBookRequest(BookInfo bookInfo, String reqReason) {
        this.memberRequestHistories.add(new MemberRequestHistory(this, bookInfo, reqReason));
    }

    public void addBookmark(BookInfo bookInfo) {
        this.memberBookmarks.add(new MemberBookmark(this, bookInfo));
    }

    public void removeBookmark(BookInfo bookInfo) {
        MemberBookmark targetBookmark = this.memberBookmarks.stream()
                .filter(bookmark -> bookmark.getBookInfo().equals(bookInfo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("찜한 책을 찾을 수 없습니다."));
        this.memberBookmarks.remove(targetBookmark);
    }

    public void loanBook(BookInfo bookInfo) {
        this.memberLoanHistories.add(new MemberLoanHistory(this, bookInfo));
    }

    public void returnBook(MemberLoanHistory targetHistory) {
        targetHistory.doReturn();
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changePassword(String password) {
        this.password = password;
    }

    public void changeName(String name) {
        this.name = name;
    }
}
