package com.jeontongju.orderbatch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class SellerTotalAmountProjection {
    private Long sellerId;
    private String sellerName;
    private Long totalAmount;
}