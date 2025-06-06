package com.example.mingle.domain.post.post.controller;

import com.example.mingle.domain.post.post.dto.PostRequestDto;
import com.example.mingle.domain.post.post.dto.PostResponseDto;
import com.example.mingle.domain.post.post.entity.BusinessDocumentCategory;
import com.example.mingle.domain.post.post.entity.NoticeType;
import com.example.mingle.domain.post.post.service.PostService;
import com.example.mingle.global.security.auth.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post", description = "게시글 관련 API")
public class ApiV1PostController {
    private final PostService postService;
    //게시글 CREATE
    @Operation(
            summary = "게시글 작성",
            description = "게시판을 선택하여 새로운 게시글을 작성합니다.",
            parameters = {
                    @Parameter(name = "postTypeId", description = "게시판 ID", required = true)
            },
            responses = {
                    @ApiResponse(responseCode = "201", description = "작성 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 (내용 누락 등)"),
                    @ApiResponse(responseCode = "403", description = "권한없음"),
                    @ApiResponse(responseCode = "404", description = "해당 게시판 없음")
            }
    )
    @PostMapping(value = "/create/image", consumes = "multipart/form-data")
    public ResponseEntity<PostResponseDto> createPost(
            @RequestPart("requestDto") @Valid PostRequestDto requestDto,
            @RequestPart(value = "postImage", required = false) MultipartFile[] postImage,
            @Parameter(description = "사용자 ID", required = true) @AuthenticationPrincipal SecurityUser user
    ) throws IOException{
        PostResponseDto responseDto = postService.createPost(
                requestDto.getPostTypeId(),
                user.getId(),
                requestDto,
                postImage
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    //공지사항 READ (회사전체공지/부서별공지/회사소식)
    @Operation(
            summary = "공지사항 유형별 조회",
            description = "공지사항 유형(GENERAL_NOTICE, DEPARTMENT_NOTICE, COMPANY_NEWS)에 따라 게시글을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "공지사항 조회 성공"),
                    @ApiResponse(responseCode = "404", description = "존재하지 않는 부서 혹은 메뉴입니다")
            }
    )
    @GetMapping("/notices")
    public ResponseEntity<List<PostResponseDto>> getNoticesByType(
            @Parameter(description = "공지사항 유형(GENERAL_NOTICE, DEPARTMENT_NOTICE, COMPANY_NEWS)", required = true)
            @RequestParam NoticeType noticeType,
            @Parameter(description = "부서 ID(부서별 공지사항 조회시 필수입력)", required = false)
            @RequestParam(required = false) Long departmentId
    ) {
        List<PostResponseDto> notices = postService.getNoticesByType(noticeType, departmentId);
        return ResponseEntity.ok(notices);
    }

    //업무자료 게시판 READ (category : 회의록/업무문서)
    @Operation(
            summary = "업무자료 게시판 게시글 조회",
            description = "게시판 ID를 통해 업무자료 게시판 카테고리에 속한 게시글들을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 카테고리를 찾을 수 없습니다")
            }
    )
    @GetMapping("/menu/{deptId}/posts")
    public ResponseEntity<List<PostResponseDto>> getBusinessDocuments(
            @Parameter(description = "게시판 ID", required = true) @PathVariable Long deptId,
            @RequestParam(required = false) BusinessDocumentCategory category
    ){
        List<PostResponseDto> posts = postService.getBusinessDocuments(deptId, category);
        return ResponseEntity.ok(posts);
    }

    //오디션공고 게시판 READ
    @Operation(
            summary = "오디션공고 게시판 게시글 조회",
            description = "게시판 ID를 통해 오디션공고 게시글을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 게시판을 찾을 수 없습니다")
            }
    )
    @GetMapping("/audition/{postMenuId}")
    public ResponseEntity<List<PostResponseDto>> getAuditionPosts(
            @Parameter(description = "게시판 ID", required = true ) @PathVariable Long postMenuId){
        List<PostResponseDto> posts = postService.getAuditionPosts(postMenuId);
        return ResponseEntity.ok(posts);
    }

    //부서별 게시판 READ
    @Operation(
            summary = "부서별 메뉴의 게시글 조회",
            description = "부서 ID와 게시판 ID를 통해 해당 카테고리에 속한 게시글들을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 카테고리 없음")
            }
    )
    @GetMapping("/departments/{deptId}/menus/{postMenuId}/posts")
    public ResponseEntity<List<PostResponseDto>> getPostsByMenu(
            @Parameter(description = "부서 ID", required = true) @PathVariable Long deptId,
            @Parameter(description = "게시판 ID", required = true) @PathVariable Long postMenuId
    ){
        List<PostResponseDto> posts = postService.getPostsByMenu(deptId, postMenuId);
        return ResponseEntity.ok(posts);
    }

    //게시글 1개 READ(상세보기)
    @Operation(
            summary = "게시글 상세보기",
            description = "게시글 ID를 통해 특정 게시글을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공"),
                    @ApiResponse(responseCode = "404", description = "해당 게시글 없음")
            }
    )
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPostById(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId){
        PostResponseDto post = postService.getPostById(postId);
        return ResponseEntity.ok(post);
    }

    //게시글 UPDATE(수정)
    @Operation(
            summary = "게시글 수정",
            description = "게시글 ID와 수정할 내용을 입력하여 게시글을 수정합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "수정 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청 (내용 누락 등)"),
                    @ApiResponse(responseCode = "404", description = "해당 게시글 없음")
            }
    )
    @PutMapping(value = "/{postId}", consumes = "multipart/form-data")
    public ResponseEntity<PostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestPart("postRequestDto") @Valid PostRequestDto postRequestDto,
            @RequestPart(value = "postImage", required = false) MultipartFile[] postImage,
            @Parameter(description = "사용자 ID", required = true)
            @AuthenticationPrincipal SecurityUser user
    ) throws IOException {
        PostResponseDto responseDto = postService.updatePost(postId, user.getId(), postRequestDto, postImage);
        return ResponseEntity.ok(responseDto);
    }

    //게시글 DELETE
    @Operation(
            summary = "게시글 삭제",
            description = "게시글 ID를 통해 특정 게시글을 삭제합니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공"),
                    @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
                    @ApiResponse(responseCode = "403", description = "권한이 없는 사용자"),
                    @ApiResponse(responseCode = "404", description = "해당 게시글 없음")
            }
    )
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable Long postId,
            @Parameter(description = "사용자 ID", required = true) @AuthenticationPrincipal SecurityUser user
    ){
        postService.deletePost(postId, user.getId());
        return ResponseEntity.noContent().build();
    }

    //TODO : 게시글 검색
//    //게시글 SEARCH
//    @Operation(
//            summary = "게시글 검색",
//            description = "제목 또는 작성자 기준으로 게시글을 검색합니다. type=title 또는 type=writer"
//    )
//    @GetMapping("/search")
//    public ResponseEntity<Page<PostResponseDto>> searchPosts(
//            @RequestParam String type,
//            @RequestParam String keyword,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
//    ){
//        String[] sortParams = sort.split(",");
//        String sortField = sortParams[0];
//        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
//                ? Sort.Direction.ASC
//                : Sort.Direction.DESC;
//
//        Page<PostResponseDto> posts = postService.searchPostsPageable(type, keyword, page, size, sortField, direction);
//        return ResponseEntity.ok(posts);
//    }

    //게시글 페이지 조회
    @GetMapping("/pageable")
    @Operation(
            summary = "게시글 페이징",
            description = "페이지를 적용하여 게시글을 적용합니다",
            responses = {
                    @ApiResponse(responseCode = "200", description = "조회 성공")
            }
    )
    public ResponseEntity<Page<PostResponseDto>> getPostsByPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long postMenuId,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort) {
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Page<PostResponseDto> posts;
        if (postMenuId != null) {
            posts = postService.getPostsByPageable(postMenuId, page, size, sortField, direction);
        } else {
            posts = postService.getAllPostsPageable(page, size, sortField, direction);
        }
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/business-documents")
    public ResponseEntity<List<PostResponseDto>> getBusinessDocumentsByDepartment(
            @RequestParam Long departmentId,
            @RequestParam(required = false) BusinessDocumentCategory category
    ) {
        List<PostResponseDto> posts = postService.getBusinessDocuments(departmentId, category);
        return ResponseEntity.ok(posts);
    }
}
