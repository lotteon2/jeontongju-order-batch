package com.jeontongju;

import io.github.bitbox.bitbox.util.KafkaTopicNameInfo;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class KafkaConsumerMock {
    private CountDownLatch latch = new CountDownLatch(1);
    private List<Object> payload = new ArrayList<>();

    @KafkaListener(topics = KafkaTopicNameInfo.SEND_NOTIFICATION, groupId = "testConsumerGroup")
    public void receive(ConsumerRecord<?, ?> consumerRecord) {
        payload.add(consumerRecord.value());
        latch.countDown();
    }

    public void resetLatch() {
        payload = new ArrayList<>();
        latch = new CountDownLatch(1);
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public List<Object> getPayload() {
        return payload;
    }
}