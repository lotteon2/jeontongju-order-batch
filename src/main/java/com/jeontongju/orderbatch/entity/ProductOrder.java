package com.jeontongju.orderbatch.entity;

import com.jeontongju.orderbatch.entity.common.BaseEntity;
import com.jeontongju.orderbatch.enums.ProductOrderStatusEnum;
import com.sun.istack.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import java.time.LocalDateTime;

@Getter
@Entity
@DynamicInsert
@DynamicUpdate
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOrder extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productOrderId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orders_id")
    private Orders orders;

    @NotNull
    private String productId;

    @NotNull
    private String productName;

    @NotNull
    private Long productCount;

    @NotNull
    private Long productPrice;

    @NotNull
    private Long productRealAmount;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long productRealPointAmount;

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    private Long productRealCouponAmount;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) default 'ORDER'")
    private ProductOrderStatusEnum productOrderStatus;

    @NotNull
    private Long sellerId;

    @NotNull
    private String sellerName;

    @NotNull
    @Column(columnDefinition = "TEXT")
    private String productThumbnailImageUrl;

    @NotNull
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDate;

    @NotNull
    private Long consumerId;

    @OneToOne(mappedBy = "productOrder", fetch = FetchType.LAZY)
    private Delivery delivery;

    /**
     * 주문확정 상태로 변경
     */
    public void changeOrderStatusToConfirmStatus(){
        this.productOrderStatus = ProductOrderStatusEnum.CONFIRMED;
    }
    public void changeOrderStatusToCancelStatus(){
        this.productOrderStatus = ProductOrderStatusEnum.CANCEL;
    }

}
