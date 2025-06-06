package com.example.mingle.domain.post.legalpost.entity;

import com.example.mingle.domain.post.legalpost.enums.ContractCategory;
import com.example.mingle.domain.post.legalpost.enums.ContractStatus;
import com.example.mingle.domain.post.legalpost.enums.ContractType;
import com.example.mingle.domain.user.team.entity.ArtistTeam;
import com.example.mingle.domain.user.user.entity.User;
import com.example.mingle.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter@Setter
@Table(name = "contract")
public class Contract extends BaseEntity {

    // 계약 당사자: 아티스트 또는 일반 유저
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(precision = 15, scale = 2)
    private BigDecimal contractAmount; // 계약 약정 금액 (선택사항)

    // 소속팀
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private ArtistTeam team;

    // 계약서 파일 (업로드 경로 등)
    @Column(length = 500, nullable = false)
    private String fileUrl;

    // 계약 요약 설명
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String summary;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractCategory contractCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;  // ELECTRONIC or PAPER

    @Column(length = 100)
    private String signerName;

    @Column(columnDefinition = "TEXT")
    private String signerMemo;

    @Column(length = 200)
    private String title; // 계약서 제목

    @Column(length = 100)
    private String companyName; // 서명 발신자 or 회사명 (ex. Mingle엔터)

    private String docusignEnvelopeId;


    @Column(name = "docusign_url", columnDefinition = "TEXT")
    private String docusignUrl;

    @ManyToMany
    @JoinTable(
            name = "contract_participant",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants = new ArrayList<>();
}
