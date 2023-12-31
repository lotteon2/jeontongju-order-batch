package com.jeontongju.orderbatch.batch;

import com.jeontongju.TestBatchConfig;
import com.jeontongju.orderbatch.batch.repository.DeliveryRepository;
import com.jeontongju.orderbatch.batch.repository.OrdersRepository;
import com.jeontongju.orderbatch.entity.Delivery;
import com.jeontongju.orderbatch.entity.Orders;
import com.jeontongju.orderbatch.entity.ProductOrder;
import com.jeontongju.orderbatch.enums.OrderStatusEnum;
import com.jeontongju.orderbatch.enums.ProductOrderStatusEnum;
import com.jeontongju.orderbatch.repository.ProductOrderRepository;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = {ProductOrderConfirmBatch.class, TestBatchConfig.class})
public class ProductOrderConfirmBatchTest {
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private ProductOrderRepository productOrderRepository;
    @Autowired
    private DeliveryRepository deliveryRepository;

    @AfterEach
    public void deleteData(){
        deliveryRepository.deleteAll();
        productOrderRepository.deleteAll();
        ordersRepository.deleteAll();
    }

    @Test
    public void 만약_모든_주문이_14일이상이_지났다면_모두_구매확정_처리된다() throws Exception {
        LocalDateTime orderDate1 = convertStringToLocalDateTime("2023-12-16 11:22:11");
        LocalDateTime orderDate2 = convertStringToLocalDateTime("2023-12-15 11:22:11");
        LocalDateTime orderDate3 = convertStringToLocalDateTime("2023-12-14 11:22:11");
        long sellerA = 100;
        long sellerB = 101;
        long sellerC = 102;

        Orders orders1 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq1").consumerId(1L).orderDate(orderDate1)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders2 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq2").consumerId(1L).orderDate(orderDate2)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders3 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3").consumerId(1L).orderDate(orderDate3)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();

        ordersRepository.save(orders1);
        ordersRepository.save(orders2);
        ordersRepository.save(orders3);

        ProductOrder productOrder1 = ProductOrder.builder().orders(orders1).productId("test-id").productName("test-name")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate1).consumerId(1L).build();
        ProductOrder productOrder2 = ProductOrder.builder().orders(orders2).productId("test-id2").productName("test-name2")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate2).consumerId(1L).build();
        ProductOrder productOrder3 = ProductOrder.builder().orders(orders3).productId("test-id3").productName("test-name3")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerC)
                .sellerName("셀러102번").productThumbnailImageUrl("이미지3").orderDate(orderDate3).consumerId(1L).build();
        productOrderRepository.save(productOrder1);
        productOrderRepository.save(productOrder2);
        productOrderRepository.save(productOrder3);

        Delivery delivery1 = Delivery.builder().productOrder(productOrder1).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery2 = Delivery.builder().productOrder(productOrder2).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery3 = Delivery.builder().productOrder(productOrder3).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        deliveryRepository.save(delivery1);
        deliveryRepository.save(delivery2);
        deliveryRepository.save(delivery3);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "20231230")
                .addString("version","1")
        .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        for(ProductOrder productOrder : productOrderRepository.findAll()){
            Assertions.assertEquals(productOrder.getProductOrderStatus(), ProductOrderStatusEnum.CONFIRMED);
        }
    }

    @Test
    public void 경매상품의_경우_14일이지나든_지나지않든_구매확정_처리되지_않는다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-16 11:22:11");
        LocalDateTime orderDate2 = convertStringToLocalDateTime("2023-12-25 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders1 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq1").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(true).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders2 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq2").consumerId(1L).orderDate(orderDate2)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(true).paymentMethod(PaymentMethodEnum.KAKAO).build();

        ordersRepository.save(orders1);
        ordersRepository.save(orders2);

        ProductOrder productOrder1 = ProductOrder.builder().orders(orders1).productId("test-id").productName("test-name")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build();
        ProductOrder productOrder2 = ProductOrder.builder().orders(orders2).productId("test-id2").productName("test-name2")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate2).consumerId(1L).build();
        productOrderRepository.save(productOrder1);
        productOrderRepository.save(productOrder2);

        Delivery delivery1 = Delivery.builder().productOrder(productOrder1).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery2 = Delivery.builder().productOrder(productOrder2).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        deliveryRepository.save(delivery1);
        deliveryRepository.save(delivery2);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "20231230")
                .addString("version","2")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        for(ProductOrder productOrder : productOrderRepository.findAll()){
            Assertions.assertEquals(productOrder.getProductOrderStatus(), ProductOrderStatusEnum.ORDER);
        }
    }

    @Test
    public void 모든_주문이_14일이상이_지나지않은경우_구매확정_처리되지_않는다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-23 11:22:11");
        LocalDateTime orderDate2 = convertStringToLocalDateTime("2023-12-25 11:22:11");
        LocalDateTime orderDate3 = convertStringToLocalDateTime("2023-12-24 11:22:11");
        long sellerA = 100;
        long sellerB = 101;
        long sellerC = 102;

        Orders orders1 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq1").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders2 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq2").consumerId(1L).orderDate(orderDate2)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders3 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq3").consumerId(1L).orderDate(orderDate3)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();

        ordersRepository.save(orders1);
        ordersRepository.save(orders2);
        ordersRepository.save(orders3);

        ProductOrder productOrder1 = ProductOrder.builder().orders(orders1).productId("test-id").productName("test-name")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build();
        ProductOrder productOrder2 = ProductOrder.builder().orders(orders2).productId("test-id2").productName("test-name2")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate2).consumerId(1L).build();
        ProductOrder productOrder3 = ProductOrder.builder().orders(orders3).productId("test-id3").productName("test-name3")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerC)
                .sellerName("셀러102번").productThumbnailImageUrl("이미지3").orderDate(orderDate3).consumerId(1L).build();
        productOrderRepository.save(productOrder1);
        productOrderRepository.save(productOrder2);
        productOrderRepository.save(productOrder3);

        Delivery delivery1 = Delivery.builder().productOrder(productOrder1).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery2 = Delivery.builder().productOrder(productOrder2).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery3 = Delivery.builder().productOrder(productOrder3).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        deliveryRepository.save(delivery1);
        deliveryRepository.save(delivery2);
        deliveryRepository.save(delivery3);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "20231230")
                .addString("version","3")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        for(ProductOrder productOrder : productOrderRepository.findAll()){
            Assertions.assertEquals(productOrder.getProductOrderStatus(), ProductOrderStatusEnum.ORDER);
        }
    }

    @Test
    public void 배송완료된_상품만_14일이상이_지난경우_구매확정_된다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-11 11:22:11");
        LocalDateTime orderDate2 = convertStringToLocalDateTime("2023-12-11 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders1 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq1").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders2 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq2").consumerId(1L).orderDate(orderDate2)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders1);
        ordersRepository.save(orders2);

        ProductOrder productOrder1 = ProductOrder.builder().orders(orders1).productId("test-id").productName("test-name")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build();
        ProductOrder productOrder2 = ProductOrder.builder().orders(orders2).productId("test-id2").productName("test-name2")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate2).consumerId(1L).build();
        productOrderRepository.save(productOrder1);
        productOrderRepository.save(productOrder2);

        Delivery delivery1 = Delivery.builder().productOrder(productOrder1).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery2 = Delivery.builder().productOrder(productOrder2).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.SHIPPING).build();
        deliveryRepository.save(delivery1);
        deliveryRepository.save(delivery2);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "20231230")
                .addString("version","4")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        ProductOrder productOrderA = productOrderRepository.findById(productOrder1.getProductOrderId()).orElseThrow(()->new RuntimeException(""));
        ProductOrder productOrderB = productOrderRepository.findById(productOrder2.getProductOrderId()).orElseThrow(()->new RuntimeException(""));
        Assertions.assertEquals(productOrderA.getProductOrderStatus(), ProductOrderStatusEnum.CONFIRMED);
        Assertions.assertEquals(productOrderB.getProductOrderStatus(), ProductOrderStatusEnum.ORDER);
    }

    @Test
    public void 취소상품의_경우_구매확정_되지_않는다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-11 11:22:11");
        LocalDateTime orderDate2 = convertStringToLocalDateTime("2023-12-11 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders1 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq1").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.CANCEL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        Orders orders2 = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq2").consumerId(1L).orderDate(orderDate2)
                .orderStatus(OrderStatusEnum.CANCEL).totalPrice(10000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders1);
        ordersRepository.save(orders2);

        ProductOrder productOrder1 = ProductOrder.builder().orders(orders1).productId("test-id").productName("test-name")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.CANCEL).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build();
        ProductOrder productOrder2 = ProductOrder.builder().orders(orders2).productId("test-id2").productName("test-name2")
                .productCount(1L).productPrice(10000L).productRealAmount(10000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.CANCEL).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate2).consumerId(1L).build();
        productOrderRepository.save(productOrder1);
        productOrderRepository.save(productOrder2);

        Delivery delivery1 = Delivery.builder().productOrder(productOrder1).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.COMPLETED).build();
        Delivery delivery2 = Delivery.builder().productOrder(productOrder2).recipientName("최성훈").recipientPhoneNumber("01012345678")
                .basicAddress("서울시").addressDetail("연희동어딘가").zonecode("12345").deliveryCode("1234").deliveryStatus(ProductOrderStatusEnum.SHIPPING).build();
        deliveryRepository.save(delivery1);
        deliveryRepository.save(delivery2);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "20231230")
                .addString("version","5")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        ProductOrder productOrderA = productOrderRepository.findById(productOrder1.getProductOrderId()).orElseThrow(()->new RuntimeException(""));
        ProductOrder productOrderB = productOrderRepository.findById(productOrder2.getProductOrderId()).orElseThrow(()->new RuntimeException(""));
        Assertions.assertEquals(productOrderA.getProductOrderStatus(), ProductOrderStatusEnum.CANCEL);
        Assertions.assertEquals(productOrderB.getProductOrderStatus(), ProductOrderStatusEnum.CANCEL);
    }

    private LocalDateTime convertStringToLocalDateTime(String inputString) {
        return LocalDateTime.parse(inputString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}