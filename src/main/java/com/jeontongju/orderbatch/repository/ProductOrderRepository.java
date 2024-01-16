package com.jeontongju.orderbatch.repository;

import com.jeontongju.orderbatch.dto.SellerTotalAmountProjection;
import com.jeontongju.orderbatch.entity.ProductOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {
    @Query("SELECT NEW com.jeontongju.orderbatch.dto.SellerTotalAmountProjection(p.sellerId, p.sellerName, SUM(p.productPrice * p.productCount)) " +
            "FROM ProductOrder p JOIN p.orders o " +
            "WHERE REPLACE(SUBSTRING(p.orderDate, 1, 7), '-', '') = :date " +
            "AND p.sellerId IN :sellerIds " +
            "AND (p.productOrderStatus = 'CONFIRMED' OR o.isAuction = true) " +
            "GROUP BY p.sellerId, p.sellerName")
    List<SellerTotalAmountProjection> getTotalAmountBySellerAndDate(String date, List<? extends Long> sellerIds);

    @Modifying
    @Query("UPDATE ProductOrder p SET p.productOrderStatus = 'CONFIRMED' WHERE p.productOrderId IN :productOrderIds")
    void confirmProductOrders(List<Long> productOrderIds);
}