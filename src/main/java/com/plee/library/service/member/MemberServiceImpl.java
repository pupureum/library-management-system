package com.plee.library.service.member;

import com.plee.library.domain.book.Book;
import com.plee.library.domain.book.BookInfo;
import com.plee.library.domain.member.Member;
import com.plee.library.config.MemberAdapter;
import com.plee.library.domain.member.MemberLoanHistory;
import com.plee.library.domain.member.MemberRequestHistory;
import com.plee.library.dto.admin.request.UpdateMemberRequest;
import com.plee.library.dto.member.request.SignUpMemberRequest;
import com.plee.library.dto.admin.response.MemberStatusResponse;
import com.plee.library.dto.member.response.MemberInfoResponse;
import com.plee.library.repository.book.BookInfoRepository;
import com.plee.library.repository.member.MemberRequestHistoryRepository;
import com.plee.library.util.message.MemberMessage;
import com.plee.library.repository.book.BookRepository;
import com.plee.library.repository.member.MemberLoanHistoryRepository;
import com.plee.library.repository.member.MemberRepository;
import com.plee.library.dto.member.condition.LoanHistorySearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberServiceImpl implements MemberService, UserDetailsService {

    private final MemberRepository memberRepository;
    private final MemberLoanHistoryRepository memberLoanHisRepository;
    private final MemberRequestHistoryRepository memberReqHisRepository;
    private final BookRepository bookRepository;
    private final BookInfoRepository bookInfoRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 회원가입 요청의 유효성을 검증합니다.
     * 비밀번호와 비밀번호 확인이 일치하는지, 로그인 아이디가 중복되는지를 확인하여 BindingResult에 오류를 추가합니다.
     *
     * @param request       회원가입 요청 정보를 담은 객체
     * @param bindingResult 검증 결과를 담는 BindingResult 객체
     */
    @Override
    @Transactional(readOnly = true)
    public void validateSignupRequest(SignUpMemberRequest request, BindingResult bindingResult) {
        // 비밀번호 입력 2개가 일치하는지 확인
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordNotMatch", MemberMessage.NOT_MATCHED_PASSWORD.getMessage());
        }
        // 로그인 아이디가 중복되는지 확인
        if (memberRepository.existsByLoginId(request.getLoginId())) {
            bindingResult.rejectValue("loginId", "duplicateLoginId", MemberMessage.DUPLICATE_LOGIN_ID.getMessage());
        }
    }

    /**
     * 회원 정보를 저장합니다.
     *
     * @param request 회원가입 정보를 담은 요청 객체
     * @return 저장된 회원
     */
    @Override
    @Transactional
    public void saveMember(SignUpMemberRequest request) {
        // 비밀번호 암호화
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 회원 정보 생성
        Member member = Member.builder()
                .loginId(request.getLoginId())
                .password(encoder.encode(request.getPassword()))
                .name(request.getName())
                .build();
        memberRepository.save(member);
    }

    /**
     * 스프링 시큐리티에서 사용자 정보를 가져옵니다.
     * 로그인 아이디로 회원인지 판별합니다.
     *
     * @param loginId 로그인 ID
     * @return 사용자를 나타내는 UserDetails 객체
     * @throws UsernameNotFoundException 회원 정보를 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    @Primary
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException(MemberMessage.NOT_FOUND_MEMBER.getMessage()));
        return new MemberAdapter(member);
    }

    /**
     * 특정 회원의 정보를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 조회된 회원의 정보 (MemberInfoResponse 객체)
     * @throws NoSuchElementException 요청한 회원을 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public MemberInfoResponse findMember(Long memberId) {
        Member foundMember = findMemberById(memberId);

        // 회원 정보를 MemberInfoResponse 객체로 변환
        return MemberInfoResponse.from(foundMember);
    }

    /**
     * 특정 회원을 ID로 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 조회된 Member 객체
     * @throws NoSuchElementException 요청한 회원이 존재하지 않을 경우
     */
    @Transactional(readOnly = true)
    public Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NoSuchElementException(MemberMessage.NOT_FOUND_MEMBER.getMessage()));
    }

    /**
     * 모든 회원을 최신순으로 페이지네이션하여 조회합니다.
     *
     * @param pageable 페이지 정보
     * @return 회원 정보를 담은 Page 객체
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MemberStatusResponse> findAllMembers(Pageable pageable) {
        Page<Member> members = memberRepository.findAll(pageable);

        // 조회된 회원들을 MemberInfoResponse 객체 리스트로 변환
        List<MemberStatusResponse> response = MemberStatusResponse.from(members);
        return new PageImpl<>(response, pageable, members.getTotalElements());
    }

    /**
     * 현재 비밀번호와 일치 여부를 판별합니다.
     *
     * @param currentPassword 입력된 현재 비밀번호
     * @param memberId        회원 ID
     * @return 비밀번호가 일치하면 true, 일치하지 않으면 false
     * @throws NoSuchElementException 회원이 존재하지 않을 경우
     */
    @Override
    @Transactional(readOnly = true)
    public boolean checkCurrentPassword(String currentPassword, Long memberId) {
        Member member = findMemberById(memberId);
        return passwordEncoder.matches(currentPassword, member.getPassword());
    }

    /**
     * 관리자에 의해 회원 정보를 변경합니다.
     * 이름과 권한을 변경할 수 있습니다.
     *
     * @param memberId 회원 ID
     * @param request  변경할 회원 정보
     * @throws NoSuchElementException 요청한 회원이 존재하지 않을 경우
     */
    @Override
    @Transactional
    public void updateMemberByAdmin(Long memberId, UpdateMemberRequest request) {
        Member member = findMemberById(memberId);

        // 이름이 변경되었다면 변경
        if (!request.getName().equals(member.getName())) {
            member.changeName(request.getName());
        }
        // 권한이 변경되었다면 변경
        if (!request.getRole().equals(member.getRole())) {
            member.changeRole(request.getRole());
        }
    }

    /**
     * 회원 정보를 변경합니다.
     * 이름과 비밀번호를 변경할 수 있습니다.
     *
     * @param memberId 회원 ID
     * @param request  변경할 회원 정보
     * @throws NoSuchElementException 요청한 회원이 존재하지 않을 경우
     * @throws IllegalStateException  이름과 비밀번호 모두 변경되지 않은 경우
     */
    @Override
    @Transactional
    public void changeMemberInfo(Long memberId, UpdateMemberRequest request) {
        Member member = findMemberById(memberId);
        String newName = request.getName();
        String newPassword = request.getPassword();

        boolean isChangedName = !newName.equals(member.getName());
        boolean isChangedPassword = !newPassword.isEmpty() && !passwordEncoder.matches(newPassword, member.getPassword());

        // 이름과 비밀번호 모두 변경되지 않은 경우
        if (!isChangedName && !isChangedPassword) {
            throw new IllegalStateException(MemberMessage.NOT_CHANGED_ANYTHING.getMessage());
        }

        // 이름이 변경되었다면 변경
        if (isChangedName) {
            member.changeName(newName);
        }

        // 비밀번호가 변경되었다면 변경
        if (isChangedPassword) {
            member.changePassword(passwordEncoder.encode(newPassword));
        }
    }

    /**
     * 특정 회원을 삭제합니다.
     * 대출중인 도서가 있다면 강제 반납 처리합니다.
     * 입고 처리 되지 않았으며, 해당 회원만 신청한 도서가 있는 경우, 해당 도서 정보도 삭제합니다.
     *
     * @param memberId 회원 ID
     * @throws NoSuchElementException 요청한 회원이 존재하지 않을 경우
     */
    @Override
    @Transactional
    public void deleteMember(Long memberId) {
        Member member = findMemberById(memberId);
        // 대출중인 도서가 있는 회원의 경우 강제 반납 처리
        List<MemberLoanHistory> notReturnedHistories = memberLoanHisRepository.searchHistory(
                LoanHistorySearchCondition
                        .builder()
                        .memberId(memberId)
                        .notReturned(true)
                        .build());

        notReturnedHistories.forEach(history -> {
            history.doReturn();
            bookRepository.findByBookInfoIsbn(history.getBookInfo().getIsbn())
                    .ifPresent(Book::increaseLoanableCnt);
        });

        // 입고처리 되지 않은 내역 중, 해당 사용자만 신청했던 도서 신청 내역이 있는 경우, 해당 도서의 정보도 제거
        List<MemberRequestHistory> histories = memberReqHisRepository.findByMemberIdAndIsApprovedFalse(memberId);
        histories.forEach(history -> {
            BookInfo bookInfo = history.getBookInfo();
            boolean isRequestedOnlyByMember = memberReqHisRepository.countByBookInfo(bookInfo) == 1;
            if (isRequestedOnlyByMember) {
                bookInfoRepository.delete(bookInfo);
                log.info("SUCCESS delete bookInfo : {}", bookInfo.getTitle());

            }
        });

        memberRepository.delete(member);
        log.info("SUCCESS delete member id : {}", memberId);
    }
}
