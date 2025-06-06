package com.example.mingle.domain.user.auth.service;

import com.example.mingle.domain.user.team.entity.Department;
import com.example.mingle.domain.user.team.repository.DepartmentRepository;
import com.example.mingle.domain.user.auth.dto.LoginRequestDto;
import com.example.mingle.domain.user.auth.dto.TokenResponseDto;
import com.example.mingle.domain.user.user.entity.User;
import com.example.mingle.domain.user.user.entity.UserRole;
import com.example.mingle.domain.user.user.entity.UserStatus;
import com.example.mingle.domain.user.user.repository.UserRepository;
import com.example.mingle.global.exception.ApiException;
import com.example.mingle.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;

    /**
     * 로그인
     */
    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        String identifier = request.getLoginId(); // 사용자가 입력한 ID 또는 이메일

        // 입력값이 이메일인지 loginId인지 판단해서 사용자 조회
        Optional<User> userOptional;
        if (identifier.contains("@")) {
            userOptional = userRepository.findByEmail(identifier);
        } else {
            userOptional = userRepository.findByLoginId(identifier);
        }

        // 사용자 정보가 없으면 예외 발생
        User user = userOptional
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ApiException(ErrorCode.ACCOUNT_SUSPENDED); // 적절한 에러코드 사용
        }

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException(ErrorCode.INVALID_PASSWORD);
        }

        String accessToken = authTokenService.genAccessToken(user);

        // 리프레시 토큰 생성
        String refreshToken = authTokenService.genRefreshToken(user);

        // 리프레시 토큰 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        return TokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();
    }



    /**
     * 로그아웃 처리
     * → refreshToken을 무효화하고 저장
     * → Controller에서는 쿠키 삭제와 SecurityContext 초기화만 담당
     */
    @Transactional
    public void logout(User user) {
        user.setRefreshToken(null);
        userRepository.save(user);
    }



    // refreshToken을 받아 access + refresh 토큰을 새로 발급해주는 메서드
    @Transactional
    public TokenResponseDto refreshToken(String refreshToken) {
        // 1. 토큰 유효성 검사
        if (!authTokenService.isValid(refreshToken)) {
            throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. refreshToken과 일치하는 유저 조회
        User user = userRepository.findByRefreshToken(refreshToken)
                .filter(u -> u.getRefreshToken().equals(refreshToken))  // 보안상 재확인
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 3. 새 토큰 발급
        String newAccessToken = authTokenService.genAccessToken(user);
        String newRefreshToken = authTokenService.genRefreshToken(user);

        // 4. refreshToken 업데이트 후 저장
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        // 5. 응답
        return TokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .build();

    }



    /**
     * AccessToken을 통해 사용자 정보 파싱
     * → Rq.getUserFromAccessToken()에서 사용
     */
    public User getUserFromAccessToken(String accessToken) {
        log.info("accessToken 파싱 시도 중");

        if (!authTokenService.isValid(accessToken)) {
            log.warn("accessToken 유효하지 않음");
            return null;
        }

        Map<String, Object> payload = authTokenService.payload(accessToken);

        if (payload == null) return null;

        long userId = ((Number) payload.get("userId")).longValue();
        String email = (String) payload.get("email");
        String nickname = (String) payload.get("nickname");
        String roleString = (String) payload.get("role");
        UserRole role = UserRole.valueOf(roleString);

        // department 조회 (없을 경우 null 가능)
        Department department = departmentRepository.findByUserId(userId);

        log.info("token payload userId: {}", userId);

        return User.builder()
                .id(userId)
                .email(email)
                .nickname(nickname)
                .role(role)
                .department(department) // 추가된 부서 정보
                .build();
    }



    /**
     * accessToken 생성 (AuthTokenService 위임)
     * → Rq.makeAuthCookies(), Rq.refreshAccessToken() 등에서 사용
     */
    public String genAccessToken(User user) {
        return authTokenService.genAccessToken(user);
    }



    /**
     * refreshToken으로 사용자 조회
     * → Rq.refreshAccessTokenByRefreshToken()에서 사용
     */
    @Transactional(readOnly = true)
    public Optional<User> findByRefreshToken(String refreshToken) {
        return userRepository.findByRefreshToken(refreshToken);
    }
}
