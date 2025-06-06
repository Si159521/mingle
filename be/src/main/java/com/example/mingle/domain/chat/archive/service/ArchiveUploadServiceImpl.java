package com.example.mingle.domain.chat.archive.service;

import com.example.mingle.domain.chat.archive.dto.ArchiveUploadRequest;
import com.example.mingle.domain.chat.archive.dto.ArchiveItemResponse;
import com.example.mingle.domain.chat.archive.entity.ArchiveItem;
import com.example.mingle.domain.chat.archive.entity.ArchiveTag;
import com.example.mingle.domain.chat.archive.repository.ArchiveItemRepository;
import com.example.mingle.domain.chat.archive.repository.ArchiveTagRepository;
import com.example.mingle.domain.chat.common.util.ChatUtil;
import com.example.mingle.domain.user.user.entity.User;
import com.example.mingle.domain.user.user.repository.UserRepository;
import com.example.mingle.global.aws.AwsS3Uploader;
import com.example.mingle.global.exception.ApiException;
import com.example.mingle.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveUploadServiceImpl implements ArchiveUploadService {

    private final UserRepository userRepository;
    private final ArchiveItemRepository archiveItemRepository;
    private final ArchiveTagRepository archiveTagRepository;
    private final AwsS3Uploader awsS3Uploader;

    @Override
    public ArchiveItemResponse upload(ArchiveUploadRequest request) throws IOException {

        // 1. S3에 업로드
        String fileUrl = awsS3Uploader.upload(request.file(), "archive_files");


        // 2. 업로더 User 조회 (uploaderId → User 객체)
        User uploader = userRepository.findById(request.uploaderId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));


        // 3. ArchiveItem 생성
        ArchiveItem archiveItem = ArchiveItem.builder()
                .chatRoomId(request.chatRoomId())
                .uploader(uploader)
                .fileUrl(fileUrl)
                .originalFilename(request.file().getOriginalFilename())
                .thumbnailUrl(null) // 이미지 파일이 아니라면 비워둠 (후처리로 가능)
                .build();


        // 4. 태그 처리 (수동 입력이 없으면 파일명 기반 추출 예정)

        // 수동 태그가 비어 있으면 파일명에서 자동 추출
        List<String> tags;
        if (request.tags() != null && !request.tags().isEmpty()) {
            tags = request.tags(); // 수동 입력 우선
        } else {
            tags = ChatUtil.extractTagsFromFilename(request.file().getOriginalFilename()); // 자동 추출
        }

        // 추출된 태그가 존재하면 ArchiveTag로 변환 후 연결
        if (!tags.isEmpty()) {
            List<ArchiveTag> tagEntities = (List<ArchiveTag>) tags.stream()
                    .map(tagName -> ArchiveTag.builder()
                            .name(tagName)
                            .archiveItem(archiveItem)
                            .build())
                    .collect(Collectors.toList());
            archiveItem.getTags().addAll(tagEntities);
        }


        // 5. 저장
        archiveItemRepository.save(archiveItem);

        return ArchiveItemResponse.from(archiveItem); // 저장된 결과를 응답 객체로 변환하여 반환
    }



    @Transactional
    @Override
    public void updateTags(Long archiveItemId, List<String> tags) {
        // 기존 자료 유효성 확인
        ArchiveItem archiveItem = archiveItemRepository.findById(archiveItemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_ARCHIVE));

        // 기존 태그 전부 삭제
        archiveTagRepository.deleteAllByArchiveItemId(archiveItemId);

        // 새로 들어온 List<String> tags 값을 기반으로 전부 다시 생성해서 저장
        List<ArchiveTag> newTags = tags.stream()
                .map(tag -> ArchiveTag.of(tag, archiveItem)) // ArchiveTag.of()는 정적 생성자
                .toList();

        archiveTagRepository.saveAll(newTags);
    }



    @Transactional
    @Override
    public void delete(Long archiveItemId) {
        ArchiveItem archiveItem = archiveItemRepository.findById(archiveItemId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND_ARCHIVE));

        archiveItemRepository.delete(archiveItem); // 실제 삭제
        log.info("자료가 삭제되었습니다. id: {}", archiveItemId);
    }
}
