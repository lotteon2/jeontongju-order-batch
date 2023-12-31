package com.jeontongju.orderbatch.batch;

import com.jeontongju.orderbatch.entity.ProductOrder;
import com.jeontongju.orderbatch.enums.ProductOrderStatusEnum;
import com.jeontongju.orderbatch.repository.ProductOrderRepository;
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

import javax.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Configuration
@Slf4j
public class ProductOrderConfirmBatch {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory emf;
    private final ProductOrderRepository productOrderRepository;
    private int chunkSize = 500;

    @Bean
    public Job productConfirmJob() {
        return jobBuilderFactory.get("productConfirmJob")
                .start(productConfirmStep()).build();
    }

    @Bean
    public Step productConfirmStep() {
        return stepBuilderFactory.get("productConfirmStep")
                .<ProductOrder, ProductOrder>chunk(chunkSize)
                .reader(productConfirmReader(null))
                .writer(productConfirmWriter())
        .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<ProductOrder> productConfirmReader(@Value("#{jobParameters[date]}") String date) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderDate", getConfirmDate(date));
        params.put("productOrderStatus", ProductOrderStatusEnum.ORDER);
        params.put("deliveryStatus",ProductOrderStatusEnum.COMPLETED);
        params.put("isAuction", false);

        return new JpaPagingItemReaderBuilder<ProductOrder>()
                .name("productConfirmReader")
                .entityManagerFactory(emf)
                .pageSize(chunkSize)
                .queryString("SELECT p FROM ProductOrder p " +
                        "JOIN FETCH p.orders o "+
                        "JOIN FETCH p.delivery d " +
                        "WHERE p.productOrderStatus = :productOrderStatus " +
                        "AND o.isAuction = :isAuction " +
                        "AND d.deliveryStatus = :deliveryStatus " +
                        "AND CONCAT(SUBSTRING(p.orderDate, 1, 4), SUBSTRING(p.orderDate, 6, 2), SUBSTRING(p.orderDate, 9, 2) ) <= :orderDate "+
                        "ORDER BY p.productOrderId ASC")
                .parameterValues(params)
        .build();
    }

    @Bean
    @StepScope
    public JpaItemWriter<ProductOrder> productConfirmWriter() {
        JpaItemWriter<ProductOrder> jpaItemWriter = new JpaItemWriter<>() {
            @Override
            public void write(List<? extends ProductOrder> items) {
                productOrderRepository.confirmProductOrders(items.stream().map(ProductOrder::getProductOrderId).collect(Collectors.toList()));
            }
        };

        jpaItemWriter.setEntityManagerFactory(emf);
        return jpaItemWriter;
    }

    private String getConfirmDate(String inputDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.parse(inputDate, formatter);
        LocalDate resultDate = date.minusDays(14);
        return resultDate.format(formatter);
    }

}
