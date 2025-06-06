package com.example.mingle.domain.user.auth.controller;

import com.example.mingle.domain.user.auth.service.AuthLoginService;
import com.example.mingle.domain.user.user.dto.*;
import com.example.mingle.domain.user.user.entity.User;
import com.example.mingle.domain.user.user.service.UserService;
import com.example.mingle.global.rq.Rq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import com.example.mingle.domain.user.auth.dto.SignupRequestDto;
import com.example.mingle.domain.user.auth.dto.LoginRequestDto;
import com.example.mingle.domain.user.auth.dto.TokenResponseDto;

import java.util.List;

// 인증 관련 API 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ApiV1AuthController {

    // service쪽에 태워서 가입시킴
    private final UserService userService;
    private final AuthLoginService authLoginService;

    private final Rq rq;

    /**
     * 회원가입
     */
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        User user = userService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponseDto.fromEntity(user));
    }



    /**
     * 로그인 → accessToken 발급
     */
    @Operation(summary = "로그인", description = "로그인 ID 또는 이메일과 비밀번호를 입력해 accessToken을 발급받습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response) {
        TokenResponseDto tokenDto = authLoginService.login(request);

        // ✅ ResponseCookie 대신 직접 Set-Cookie 헤더 문자열 작성
        String accessCookie = "accessToken=" + tokenDto.getAccessToken()
                + "; HttpOnly; Path=/; Max-Age=3600; SameSite=None; Secure; Domain=mingleservice.site";

        String refreshCookie = "refreshToken=" + tokenDto.getRefreshToken()
                + "; HttpOnly; Path=/; Max-Age=" + (60 * 60 * 24 * 7) + "; SameSite=None; Secure; Domain=mingleservice.site";

//        // accessToken을 쿠키로 설정
//        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokenDto.getAccessToken())
//                .httpOnly(true) // JS에서 접근 불가 (보안상 안전)
//                .secure(false) // https 환경에서는 true, 로컬에선 false
//                .path("/")
//                .sameSite("Lax")
//                .maxAge(60 * 60) // 1시간
//                .build();
//
//        // refreshToken도 쿠키로 설정
//        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenDto.getRefreshToken())
//                .httpOnly(true)
//                .secure(false)
//                .path("/")
//                .sameSite("Lax")
//                .maxAge(60 * 60 * 24 * 7) // 7일
//                .build();

        // 헤더에 쿠키 추가
        response.addHeader("Set-Cookie", accessCookie);
        response.addHeader("Set-Cookie", refreshCookie);

        return ResponseEntity.ok(tokenDto);
    }



    /**
     * 로그아웃
     */
    @Operation(summary = "로그아웃", description = "로그아웃 처리를 합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        User user = rq.getActor();
        if (user != null) authLoginService.logout(user); // 저장 필요 시

        rq.deleteCookie("accessToken");
        rq.deleteCookie("refreshToken");
        rq.deleteCookie("JSESSIONID");
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok("로그아웃 되었습니다.");
    }



    /**
     * refreshToken을 이용해 accessToken 재발급
     */
    @Operation(summary = "AccessToken 재발급", description = "쿠키에 저장된 refreshToken을 이용해 새로운 accessToken을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AccessToken 재발급 성공"),
            @ApiResponse(responseCode = "401", description = "유효하지 않은 refreshToken")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@CookieValue("refreshToken") String refreshToken) {
        TokenResponseDto response = authLoginService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }




    /**
     * 내 정보 조회
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        try{
            User user = rq.getActor(); // 현재 로그인한 사용자 가져오기

            if (user == null) {
                System.out.println("user is null from rq.getActor()");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 인증 안 된 경우
            }

            System.out.println("user id: " + user.getId());
            System.out.println("department: " + user.getDepartment());
            System.out.println("status: " + user.getStatus());
            System.out.println("phoneNum: " + user.getPhoneNum());
            System.out.println("imageUrl: " + user.getImageUrl());

            return ResponseEntity.ok(UserResponseDto.fromEntity(user)); // DTO 변환 후 반환
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 예외 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 에러 발생: " + e.getMessage());
        }
    }



    /**
     * 내 프로필 수정
     * → 현재 로그인한 사용자가 자신의 닉네임, 이메일, 전화번호, 이미지URL을 수정
     */
    @Operation(summary = "내 프로필 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다.")
    @ApiResponse(responseCode = "200", description = "수정 성공")
    @PatchMapping("/me")
    public ResponseEntity<ProfileResponseDto> updateMyProfile(
            @RequestBody ProfileUpdateRequestDto requestDto
    ) {
        User actor = rq.getActor(); // 현재 로그인한 사용자 조회
        if (actor == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User updated = userService.updateMyProfile(actor, requestDto);
        return ResponseEntity.ok(ProfileResponseDto.fromEntity(updated));
    }




    /**
     * 특정 유저 정보 조회
     */
    @Operation(summary = "유저 프로필 조회", description = "특정 유저의 ID를 통해 프로필 정보를 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserProfile(@PathVariable Long userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
    }



    /**
     * 관리자 페이지 조회
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin-only")
    public ResponseEntity<String> adminPage() {
        return ResponseEntity.ok("관리자만 접근 가능합니다.");
    }



    /**
     * 특정 권한 페이지(추후 수정 가능)
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping("/staff-or-admin")
    public ResponseEntity<String> staffOrAdminPage() {
        return ResponseEntity.ok("관리자 또는 스태프만 접근 가능합니다.");
    }



    @GetMapping("/users/by-department")
    public ResponseEntity<List<UserResponseDto>> getUsersByDepartment(@RequestParam String name) {
        List<User> users = userService.getUsersByDepartmentName(name);

        List<UserResponseDto> response = users.stream()
                .map(UserResponseDto::fromEntity)
                .toList();

        return ResponseEntity.ok(response);
    }

}