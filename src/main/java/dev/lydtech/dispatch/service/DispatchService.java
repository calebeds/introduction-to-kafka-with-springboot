package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchService {

    private static final String DISPATCH_TRACKING_TOPIC = "dispatch.tracking";
    private static final String ORDER_DISPATCHED_TOPIC = "order.dispatched";
    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void process(String key, OrderCreated orderCreated) throws Exception {
        DispatchPreparing dispatchPreparing = DispatchPreparing.builder()
                .orderId(orderCreated.getOrderId())
                .build();
        kafkaTemplate.send(DISPATCH_TRACKING_TOPIC, key, dispatchPreparing).get();

        OrderDispatched orderDispatched = OrderDispatched.builder()
                .orderId(orderCreated.getOrderId())
                .processedById(APPLICATION_ID)
                .notes("Dispatched: " + orderCreated.getItem())
                .build();
        kafkaTemplate.send(ORDER_DISPATCHED_TOPIC, key, orderDispatched).get();

        DispatchCompleted dispatchCompleted = DispatchCompleted.builder()
                .orderId(orderCreated.getOrderId())
                .dispatchedDate(LocalDate.now().toString())
                .build();
        kafkaTemplate.send(DISPATCH_TRACKING_TOPIC, key, dispatchCompleted).get();

        log.info("Sent messages: key: " + key + " - orderId " +  orderCreated.getOrderId() + " - processById: " + APPLICATION_ID);
    }
}
