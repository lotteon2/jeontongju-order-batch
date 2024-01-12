package com.jeontongju.orderbatch.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Entity
@DynamicInsert
@DynamicUpdate
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long settlementId;

    @Column(nullable = false)
    private Long sellerId;

    private String sellerName;

    @Column(nullable = false)
    private Long settlementYear;

    @Column(nullable = false)
    private Long settlementMonth;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private Long settlementCommission;

    @Column(nullable = false)
    private Long settlementAmount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String settlementImgUrl;
}