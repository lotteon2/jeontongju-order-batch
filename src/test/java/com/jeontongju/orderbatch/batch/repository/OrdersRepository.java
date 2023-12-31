package com.jeontongju.orderbatch.batch.repository;

import com.jeontongju.orderbatch.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {

}