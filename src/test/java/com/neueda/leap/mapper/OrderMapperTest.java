package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.Client;
import com.neueda.leap.Order;
import com.neueda.leap.enums.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private AccountMapper accountMapper;

    private UUID testAccountId;

    @BeforeEach
    public void setUp() {
        // Get a test account ID
        Client client = clientMapper.selectClientByEmail("alice.johnson@example.com");
        assertNotNull(client);
        
        List<Account> accounts = accountMapper.selectAccountsByClientId(client.getClientId(), 1, 0);
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());
        testAccountId = accounts.get(0).getAccountId();
    }

    @Test
    public void testSelectOrdersByAccountId() {
        List<Order> orders = orderMapper.selectOrdersByAccountId(testAccountId, 10, 0);
        assertNotNull(orders);
        // May be empty if this specific account has no orders, that's OK
        
        orders.forEach(order -> {
            assertEquals(testAccountId, order.getAccountId());
            assertNotNull(order.getOrderId());
            assertNotNull(order.getInstrumentId());
            assertTrue(order.getSide() == OrderSide.BUY || order.getSide() == OrderSide.SELL);
            assertTrue(order.getQuantity().signum() > 0);
            assertNotNull(order.getIdempotencyKey());
            assertNotNull(order.getSubmittedAt());
        });
    }

    @Test
    public void testCountOrdersByAccountId() {
        int count = orderMapper.countOrdersByAccountId(testAccountId);
        assertTrue(count >= 0);
    }

    @Test
    public void testSelectOrderById() {
        List<Order> orders = orderMapper.selectOrdersByAccountId(testAccountId, 100, 0);
        
        if (!orders.isEmpty()) {
            Order orderFromList = orders.get(0);
            
            // Now fetch by ID
            Order orderById = orderMapper.selectOrderById(orderFromList.getOrderId());
            assertNotNull(orderById);
            assertEquals(orderFromList.getOrderId(), orderById.getOrderId());
            assertEquals(orderFromList.getAccountId(), orderById.getAccountId());
        }
    }

    @Test
    public void testSelectOrderByIdempotencyKey() {
        List<Order> orders = orderMapper.selectOrdersByAccountId(testAccountId, 100, 0);
        
        if (!orders.isEmpty()) {
            Order orderFromList = orders.get(0);
            
            // Fetch by idempotency key
            Order orderByIdempotency = orderMapper.selectOrderByIdempotencyKey(
                testAccountId, 
                orderFromList.getIdempotencyKey()
            );
            
            assertNotNull(orderByIdempotency);
            assertEquals(orderFromList.getOrderId(), orderByIdempotency.getOrderId());
            assertEquals(orderFromList.getIdempotencyKey(), orderByIdempotency.getIdempotencyKey());
        }
    }

    @Test
    public void testOrderSideValues() {
        List<Order> orders = orderMapper.selectOrdersByAccountId(testAccountId, 100, 0);
        
        orders.forEach(order -> {
            OrderSide side = order.getSide();
            assertTrue(side == OrderSide.BUY || side == OrderSide.SELL, 
                "Order side must be BUY or SELL, got: " + side);
        });
    }

    @Test
    public void testOrderPagination() {
        List<Order> allOrders = orderMapper.selectOrdersByAccountId(testAccountId, 1000, 0);
        
        if (allOrders.size() > 5) {
            List<Order> page1 = orderMapper.selectOrdersByAccountId(testAccountId, 5, 0);
            List<Order> page2 = orderMapper.selectOrdersByAccountId(testAccountId, 5, 5);
            
            assertEquals(5, page1.size());
            assertEquals(5, page2.size());
            assertNotEquals(page1.get(0).getOrderId(), page2.get(0).getOrderId());
        }
    }

    @Test
    public void testOrderHasTimestamp() {
        List<Order> orders = orderMapper.selectOrdersByAccountId(testAccountId, 1, 0);
        
        if (!orders.isEmpty()) {
            Order order = orders.get(0);
            assertNotNull(order.getSubmittedAt());
        }
    }
}
