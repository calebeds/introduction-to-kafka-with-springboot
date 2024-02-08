package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DispatchServiceTest {

    private DispatchService service;
    private KafkaTemplate kafkaTemplateMock;

    @BeforeEach
    void setUp() {
        kafkaTemplateMock = Mockito.mock(KafkaTemplate.class);
        service = new DispatchService(kafkaTemplateMock);
    }

    @Test
    void process_Success() throws Exception {
        when(kafkaTemplateMock.send(anyString(), any(OrderDispatched.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), any(DispatchPreparing.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());

        service.process(testEvent);
        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), any(DispatchPreparing.class));
        verify(kafkaTemplateMock, times(1)).send(eq("order.dispatched"), any(OrderDispatched.class));
    }

    @Test
    void testProcess_DispatchTrackingProdcuerThrowsException() {
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        doThrow(new RuntimeException("dispatch tracking producer failure")).when(kafkaTemplateMock).send(eq("dispatch.tracking"), any(DispatchPreparing.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(testEvent));

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), any(DispatchPreparing.class));
        verifyNoMoreInteractions(kafkaTemplateMock);
        assertThat(exception.getMessage(), equalTo("dispatch tracking producer failure"));
    }

    @Test
    void testProcess_OrderDispatchedProducerThrowsException() throws Exception {
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        when(kafkaTemplateMock.send(anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));

        doThrow(new RuntimeException("Producer failure"))
                .when(kafkaTemplateMock).send(eq("order.dispatched"), any(OrderDispatched.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(testEvent));

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), any(DispatchPreparing.class));
        verify(kafkaTemplateMock, times(1)).send(eq("order.dispatched"), any(OrderDispatched.class));
        assertThat(exception.getMessage(), equalTo("Producer failure"));
    }
}