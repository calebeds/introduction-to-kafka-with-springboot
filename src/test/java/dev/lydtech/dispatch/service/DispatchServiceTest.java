package dev.lydtech.dispatch.service;

import dev.lydtech.dispatch.client.StockServiceClient;
import dev.lydtech.dispatch.message.DispatchCompleted;
import dev.lydtech.dispatch.message.DispatchPreparing;
import dev.lydtech.dispatch.message.OrderCreated;
import dev.lydtech.dispatch.message.OrderDispatched;
import dev.lydtech.dispatch.util.TestEventData;
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
    private StockServiceClient stockServiceClientMock;
    private KafkaTemplate kafkaTemplateMock;

    @BeforeEach
    void setUp() {
        kafkaTemplateMock = Mockito.mock(KafkaTemplate.class);
        stockServiceClientMock = mock(StockServiceClient.class);
        service = new DispatchService(kafkaTemplateMock, stockServiceClientMock);
    }

    @Test
    void process_Success() throws Exception {
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatched.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparing.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchCompleted.class)))
                .thenReturn(mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString()))
                .thenReturn("true");

        String key = UUID.randomUUID().toString();

        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());

        service.process(key, testEvent);

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaTemplateMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
    }

    @Test
    void testProcess_DispatchTrackingProdcuerThrowsException() {
        String key = UUID.randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        when(stockServiceClientMock.checkAvailability(anyString()))
                .thenReturn("true");
        doThrow(new RuntimeException("dispatch tracking producer failure")).when(kafkaTemplateMock).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verifyNoMoreInteractions(kafkaTemplateMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
        assertThat(exception.getMessage(), equalTo("dispatch tracking producer failure"));
    }

    @Test
    void testProcess_OrderDispatchedProducerThrowsException() throws Exception {
        String key = UUID.randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString()))
                .thenReturn("true");

        doThrow(new RuntimeException("order dispatched producer failure"))
                .when(kafkaTemplateMock).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaTemplateMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verifyNoMoreInteractions(kafkaTemplateMock);
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());
        assertThat(exception.getMessage(), equalTo("order dispatched producer failure"));
    }

    @Test
    void testProcess_SecondDispatchTrackingProducerThrowsException() throws Exception {
        when(kafkaTemplateMock.send(anyString(), anyString(), any(OrderDispatched.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparing.class)))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        when(stockServiceClientMock.checkAvailability(anyString()))
                .thenReturn("true");

        String key = UUID.randomUUID().toString();
        OrderCreated testEvent = TestEventData.buildOrderCreatedEvent(UUID.randomUUID(), UUID.randomUUID().toString());
        when(kafkaTemplateMock.send(anyString(), anyString(), any(DispatchPreparing.class))).thenReturn(mock(CompletableFuture.class));

        doThrow(new RuntimeException("dispatch tracking producer failure"))
                .when(kafkaTemplateMock).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));

        Exception exception = assertThrows(RuntimeException.class, () -> service.process(key, testEvent));

        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchPreparing.class));
        verify(kafkaTemplateMock, times(1)).send(eq("order.dispatched"), eq(key), any(OrderDispatched.class));
        verify(kafkaTemplateMock, times(1)).send(eq("dispatch.tracking"), eq(key), any(DispatchCompleted.class));
        verify(stockServiceClientMock, times(1)).checkAvailability(testEvent.getItem());

        assertThat(exception.getMessage(), equalTo("dispatch tracking producer failure"));
    }
}