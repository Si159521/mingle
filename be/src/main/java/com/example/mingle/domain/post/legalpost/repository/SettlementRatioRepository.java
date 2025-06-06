package com.example.mingle.domain.post.legalpost.repository;

import com.example.mingle.domain.post.legalpost.entity.Contract;
import com.example.mingle.domain.post.legalpost.entity.SettlementRatio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

@Repository
public interface SettlementRatioRepository extends JpaRepository<SettlementRatio, Long> {

    List<SettlementRatio> findByContract(Contract contract);

    @Modifying
    @Query("DELETE FROM SettlementRatio r WHERE r.contract = :contract")
    void deleteAllByContract(@Param("contract") Contract contract);

}