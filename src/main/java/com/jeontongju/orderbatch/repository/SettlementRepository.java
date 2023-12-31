package com.jeontongju.orderbatch.repository;

import com.jeontongju.orderbatch.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>{
    Optional<Settlement> findBySellerId(Long sellerId);
}