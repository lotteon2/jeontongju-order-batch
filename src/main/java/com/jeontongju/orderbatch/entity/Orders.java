package com.jeontongju.orderbatch.entity;

import com.jeontongju.orderbatch.entity.common.BaseEntity;
import com.jeontongju.orderbatch.enums.OrderStatusEnum;
import com.sun.istack.NotNull;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
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
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@DynamicInsert
@DynamicUpdate
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders extends BaseEntity {
    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String ordersId;

    @NotNull
    private Long consumerId;

    @NotNull
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(255) default 'NORMAL'")
    private OrderStatusEnum orderStatus;

    @NotNull
    private Long totalPrice;

    @Column(columnDefinition = "boolean default false")
    private Boolean isAuction;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentMethodEnum paymentMethod;

    @OneToMany(mappedBy = "orders")
    private List<ProductOrder> productOrders;

    public boolean isCancelledOrAuction(){
        // 주문상태가 취소거나 경매상품이라면 취소할 수 있는 주문이다
        return getOrderStatus() == OrderStatusEnum.CANCEL || getIsAuction();
    }

    public void changeProductOrderStatusToCancelStatus(){
        this.orderStatus = OrderStatusEnum.CANCEL;
    }
}