package com.jeontongju.orderbatch.batch.repository;

import com.jeontongju.orderbatch.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
}
