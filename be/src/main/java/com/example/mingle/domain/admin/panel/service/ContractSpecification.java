package com.example.mingle.domain.admin.panel.service;

import com.example.mingle.domain.admin.panel.dto.ContractSearchCondition;
import com.example.mingle.domain.post.legalpost.entity.Contract;
import com.example.mingle.domain.post.legalpost.enums.ContractStatus;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ContractSpecification {

    public static Specification<Contract> build(ContractSearchCondition condition) {
        return (Root<Contract> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (condition.getTeamId() != null) {
                predicates.add(cb.equal(root.get("team").get("id"), condition.getTeamId()));
            }


            // status 필터 적용
            if (condition.getStatus() != null) {
                // 사용자가 명시적으로 상태를 선택했을 경우
                predicates.add(cb.equal(root.get("status"), condition.getStatus()));
            } else {
                // 선택하지 않은 경우에는 TERMINATED 제외
                predicates.add(cb.notEqual(root.get("status"), ContractStatus.TERMINATED));
            }

            if (condition.getContractType() != null) {
                predicates.add(cb.equal(root.get("contractType"), condition.getContractType()));
            }

            if (condition.getContractCategory() != null) {
                predicates.add(cb.equal(root.get("contractCategory"), condition.getContractCategory()));
            }

            if (condition.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), condition.getStartDateFrom()));
            }

            if (condition.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), condition.getStartDateTo()));
            }

            if (condition.getParticipantUserId() != null) {
                // 'participants'는 Contract 엔티티에서의 필드명이어야 함 (@ManyToMany or @OneToMany(mappedBy = "contract"))
                Join<Object, Object> participantJoin = root.join("participants", JoinType.INNER);
                predicates.add(cb.equal(participantJoin.get("id"), condition.getParticipantUserId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
