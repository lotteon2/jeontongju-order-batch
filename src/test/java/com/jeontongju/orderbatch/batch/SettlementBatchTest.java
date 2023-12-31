package com.jeontongju.orderbatch.batch;

import com.jeontongju.KafkaConsumerMock;
import com.jeontongju.TestBatchConfig;
import com.jeontongju.orderbatch.batch.repository.OrdersRepository;
import com.jeontongju.orderbatch.config.S3UploadUtil;
import com.jeontongju.orderbatch.entity.Orders;
import com.jeontongju.orderbatch.entity.ProductOrder;
import com.jeontongju.orderbatch.entity.Settlement;
import com.jeontongju.orderbatch.enums.OrderStatusEnum;
import com.jeontongju.orderbatch.enums.ProductOrderStatusEnum;
import com.jeontongju.orderbatch.repository.ProductOrderRepository;
import com.jeontongju.orderbatch.repository.SettlementRepository;
import io.github.bitbox.bitbox.enums.PaymentMethodEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBatchTest
@SpringBootTest(classes = {SettlementBatch.class, TestBatchConfig.class,  KafkaConsumerMock.class})
@EmbeddedKafka( partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:7777"},
        ports={7777})
public class SettlementBatchTest {
    @MockBean
    private S3UploadUtil s3UploadUtil;
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private OrdersRepository ordersRepository;
    @Autowired
    private ProductOrderRepository productOrderRepository;
    @Autowired
    private SettlementRepository settlementRepository;

    @BeforeEach
    public void before(){
        when(s3UploadUtil.uploadFile(any(), any())).thenReturn("csh");
    }

    @AfterEach
    public void deleteData(){
        productOrderRepository.deleteAll();
        ordersRepository.deleteAll();
        settlementRepository.deleteAll();
    }

    @Test
    public void 정산이_정상적으로_완료된다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-30 11:22:11");
        long sellerA = 100;
        long sellerB = 101;
        long sellerC = 102;

        Orders orders = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(140000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders);
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id").productName("test-name")
                .productCount(3L).productPrice(20000L).productRealAmount(60000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.CONFIRMED).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build());
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id2").productName("test-name2")
                .productCount(4L).productPrice(10000L).productRealAmount(30000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.CONFIRMED).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate).consumerId(1L).build());
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id3").productName("test-name3")
                .productCount(4L).productPrice(10000L).productRealAmount(30000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.CONFIRMED).sellerId(sellerC)
                .sellerName("셀러102번").productThumbnailImageUrl("이미지3").orderDate(orderDate).consumerId(1L).build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "202312")
                .addString("version","0")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Settlement sellerASettlement = settlementRepository.findBySellerId(sellerA).orElseThrow(RuntimeException::new);
        Settlement sellerBSettlement = settlementRepository.findBySellerId(sellerB).orElseThrow(RuntimeException::new);
        Settlement sellerCSettlement = settlementRepository.findBySellerId(sellerC).orElseThrow(RuntimeException::new);

        Assertions.assertEquals(sellerASettlement.getSettlementImgUrl(), "csh");
        Assertions.assertEquals(sellerASettlement.getTotalAmount(), 60000L);
        Assertions.assertEquals(sellerASettlement.getSettlementCommission(), 3000L);
        Assertions.assertEquals(sellerASettlement.getSettlementAmount(), 57000L);

        Assertions.assertEquals(sellerBSettlement.getSettlementImgUrl(), "csh");
        Assertions.assertEquals(sellerBSettlement.getTotalAmount(), 40000L);
        Assertions.assertEquals(sellerBSettlement.getSettlementCommission(), 2000L);
        Assertions.assertEquals(sellerBSettlement.getSettlementAmount(), 38000L);

        Assertions.assertEquals(sellerCSettlement.getSettlementImgUrl(), "csh");
        Assertions.assertEquals(sellerCSettlement.getTotalAmount(), 40000L);
        Assertions.assertEquals(sellerCSettlement.getSettlementCommission(), 2000L);
        Assertions.assertEquals(sellerCSettlement.getSettlementAmount(), 38000L);
    }

    @Test
    public void 구매확정건만_제대로_정산된다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-30 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(100000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders);
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id").productName("test-name")
                .productCount(3L).productPrice(20000L).productRealAmount(60000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build());
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id2").productName("test-name2")
                .productCount(4L).productPrice(10000L).productRealAmount(30000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.CONFIRMED).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate).consumerId(1L).build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "202312")
                .addString("version","1")
        .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Optional<Settlement> sellerASettlement = settlementRepository.findBySellerId(sellerA);
        Settlement sellerBSettlement = settlementRepository.findBySellerId(sellerB).orElseThrow(RuntimeException::new);

        Assertions.assertTrue(sellerASettlement.isEmpty());
        Assertions.assertEquals(sellerBSettlement.getSettlementImgUrl(), "csh");
        Assertions.assertEquals(sellerBSettlement.getTotalAmount(), 40000L);
        Assertions.assertEquals(sellerBSettlement.getSettlementCommission(), 2000L);
        Assertions.assertEquals(sellerBSettlement.getSettlementAmount(), 38000L);
    }

    @Test
    public void 구매확정건이_없으면_정산되지_않는다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-30 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(100000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders);
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id").productName("test-name")
                .productCount(3L).productPrice(20000L).productRealAmount(60000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build());
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id2").productName("test-name2")
                .productCount(4L).productPrice(10000L).productRealAmount(30000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate).consumerId(1L).build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "202312")
                .addString("version","2")
        .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Optional<Settlement> sellerASettlement = settlementRepository.findBySellerId(sellerA);
        Optional<Settlement> sellerBSettlement = settlementRepository.findBySellerId(sellerB);

        Assertions.assertTrue(sellerASettlement.isEmpty());
        Assertions.assertTrue(sellerBSettlement.isEmpty());
    }

    @Test
    public void 정산월이_아니라면_정산되지_않는다() throws Exception {
        LocalDateTime orderDate = convertStringToLocalDateTime("2023-12-30 11:22:11");
        long sellerA = 100;
        long sellerB = 101;

        Orders orders = Orders.builder().ordersId("qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq").consumerId(1L).orderDate(orderDate)
                .orderStatus(OrderStatusEnum.NORMAL).totalPrice(100000L).isAuction(false).paymentMethod(PaymentMethodEnum.KAKAO).build();
        ordersRepository.save(orders);
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id").productName("test-name")
                .productCount(3L).productPrice(20000L).productRealAmount(60000L).productRealPointAmount(0L)
                .productRealCouponAmount(0L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerA)
                .sellerName("셀러100번").productThumbnailImageUrl("이미지").orderDate(orderDate).consumerId(1L).build());
        productOrderRepository.save(ProductOrder.builder().orders(orders).productId("test-id2").productName("test-name2")
                .productCount(4L).productPrice(10000L).productRealAmount(30000L).productRealPointAmount(0L)
                .productRealCouponAmount(10000L).productOrderStatus(ProductOrderStatusEnum.ORDER).sellerId(sellerB)
                .sellerName("셀러101번").productThumbnailImageUrl("이미지2").orderDate(orderDate).consumerId(1L).build());

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("date", "202401")
                .addString("version","1")
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Optional<Settlement> sellerASettlement = settlementRepository.findBySellerId(sellerA);
        Optional<Settlement> sellerBSettlement = settlementRepository.findBySellerId(sellerB);

        Assertions.assertTrue(sellerASettlement.isEmpty());
        Assertions.assertTrue(sellerBSettlement.isEmpty());
    }

    private LocalDateTime convertStringToLocalDateTime(String inputString) {
        return LocalDateTime.parse(inputString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}