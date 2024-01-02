package com.jeontongju.orderbatch.batch;

import com.jeontongju.orderbatch.config.MyMultipartFile;
import com.jeontongju.orderbatch.config.S3UploadUtil;
import com.jeontongju.orderbatch.dto.SellerTotalAmountProjection;
import com.jeontongju.orderbatch.entity.ProductOrder;
import com.jeontongju.orderbatch.entity.Settlement;
import com.jeontongju.orderbatch.enums.ProductOrderStatusEnum;
import com.jeontongju.orderbatch.repository.ProductOrderRepository;
import com.jeontongju.orderbatch.repository.SettlementRepository;
import gui.ava.html.image.generator.HtmlImageGenerator;
import io.github.bitbox.bitbox.dto.MemberInfoForNotificationDto;
import io.github.bitbox.bitbox.enums.NotificationTypeEnum;
import io.github.bitbox.bitbox.enums.RecipientTypeEnum;
import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.persistence.EntityManagerFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.time.LocalDateTime.now;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class SettlementBatch {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;
    private final ProductOrderRepository productOrderRepository;
    private final SettlementRepository settlementRepository;
    private final S3UploadUtil s3UploadUtil;
    private final KafkaTemplate<String, MemberInfoForNotificationDto> kafkaTemplate;
    private int chunkSize = 500;

    @Bean
    public Job settlementJob() {
        return jobBuilderFactory.get("settlementJob")
                .start(settlementStep()).build();
    }

    @Bean
    public Step settlementStep() {
        return stepBuilderFactory.get("settlementStep")
                .<ProductOrder, Long>chunk(chunkSize)
                .reader(settlementReader(null))
                .writer(settlementWriter(null))
        .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<ProductOrder> settlementReader(@Value("#{jobParameters[date]}") String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("settlementDate", date);
        params.put("productOrderStatus", ProductOrderStatusEnum.CONFIRMED);

        return new JpaPagingItemReaderBuilder<ProductOrder>()
                .name("settlementReader")
                .entityManagerFactory(emf)
                .pageSize(chunkSize)
                .queryString("SELECT DISTINCT p.sellerId FROM ProductOrder p WHERE CONCAT(SUBSTRING(p.orderDate, 1, 4), SUBSTRING(p.orderDate, 6, 2)) = :settlementDate AND p.productOrderStatus = :productOrderStatus ORDER BY p.sellerId ASC")
                .parameterValues(params)
        .build();
    }

    @Bean
    @StepScope
    public JpaItemWriter<Long> settlementWriter(@Value("#{jobParameters[date]}") String date) {
        JpaItemWriter<Long> jpaItemWriter = new JpaItemWriter<>() {
            @Override
            public void write(List<? extends Long> items) {
                String year = date.substring(0,4);
                String month = date.substring(4);
                String settlementDate = year + "년" + month + "월";
                List<Settlement> settlementList = new ArrayList<>();

                for(SellerTotalAmountProjection sellerTotalAmountProjection : productOrderRepository.getTotalAmountBySellerAndDate(date, items)){
                    Long totalAmount = sellerTotalAmountProjection.getTotalAmount();
                    Long commission = (long) (totalAmount * 0.05);
                    String fileName = UUID.randomUUID().toString();

                    settlementList.add(Settlement.builder().sellerId(sellerTotalAmountProjection.getSellerId()).settlementYear(Long.valueOf(year))
                                    .settlementMonth(Long.valueOf(month)).totalAmount(totalAmount).settlementCommission(commission)
                                    .settlementAmount(totalAmount-commission).settlementImgUrl(s3UploadUtil.uploadFile(convertHtmlToImage(settlementDate,
                                    sellerTotalAmountProjection.getSellerName(), String.valueOf(totalAmount), String.valueOf(commission),
                                    String.valueOf(totalAmount - commission)), fileName))
                    .build());
                }

                for(Long id : items){
                    kafkaTemplate.send(KafkaTopicNameInfo.SEND_NOTIFICATION,
                            MemberInfoForNotificationDto.builder()
                                    .recipientId(id)
                                    .recipientType(RecipientTypeEnum.ROLE_SELLER)
                                    .notificationType(NotificationTypeEnum.BALANCE_ACCOUNTS)
                                    .createdAt(now())
                            .build());
                }
                settlementRepository.saveAll(settlementList);
            }
        };

        jpaItemWriter.setEntityManagerFactory(emf);
        return jpaItemWriter;
    }

    private MultipartFile convertHtmlToImage(String date, String shopName, String totalSales, String commission, String finalAmount) {
        try {
            String str = "<html>\n" +
                    "<head>\n" +
                    "<meta charset=\"UTF-8\">\n" +
                    "<style>\n" +
                    "  body {\n" +
                    "    text-align: center;\n" +
                    "  }\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<h1>정산내역증</h1>\n" +
                    "<h2 style=\"text-align: right;\">" + date + "</h2>\n" +
                    "<h2 style=\"text-align: right;\">" + shopName + "</h2>\n" +
                    "<br>\n" +
                    "<p style=\"text-align: center;\">총 판매 금액 : " + totalSales + "원</p>\n" +
                    "<p style=\"text-align: center;\">총 판매 수수료 : " + commission + "원</p>\n" +
                    "<hr>\n" +
                    "<p style=\"text-align: center;\">최종 정산 예정 금액 : " + finalAmount + "원</p>\n" +
                    "<br>\n" +
                    "<p style=\"text-align: center;\">1. 위 정산 금액은 다음달 5일 이내에 입금될 예정이에요.</p>\n" +
                    "<p style=\"text-align: center;\">2. 이에 대한 문의는 전통주.(카톡/번호)로 부탁드려요.</p>\n" +
                    "<h2>전통주.</h2>\n" +
                    "</body>\n" +
                    "</html>";
            String html = new String(str.getBytes("euc-kr"), StandardCharsets.UTF_8);

            byte[] htmlBytes = html.getBytes(StandardCharsets.UTF_8);

            HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
            imageGenerator.loadHtml(new String(htmlBytes, StandardCharsets.UTF_8));
            BufferedImage bufferedImage = imageGenerator.getBufferedImage();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            log.info("file encoding = {}", System.getProperty("file.encoding"));

            return new MyMultipartFile(imageBytes, "image.png");
        } catch (Exception e) {
            throw new RuntimeException("Failed to save the image");
        }
    }
}