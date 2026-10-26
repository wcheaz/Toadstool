package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.Client;
import com.neueda.leap.Fill;
import com.neueda.leap.Order;
import com.neueda.leap.enums.FillStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class FillMapperTest {

    @Autowired
    private FillMapper fillMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ClientMapper clientMapper;

    private UUID testOrderId;

    @BeforeEach
    public void setUp() {
        // Get a test order with fills
        Client client = clientMapper.selectClientByEmail("alice.johnson@example.com");
        assertNotNull(client);
        
        List<Account> accounts = accountMapper.selectAccountsByClientId(client.getClientId(), 1, 0);
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());
        
        List<Order> orders = orderMapper.selectOrdersByAccountId(accounts.get(0).getAccountId(), 100, 0);
        
        // Find an order with fills
        for (Order order : orders) {
            int fillCount = fillMapper.countFillsByOrderId(order.getOrderId());
            if (fillCount > 0) {
                testOrderId = order.getOrderId();
                break;
            }
        }
    }

    @Test
    public void testSelectFillsByOrderId() {
        if (testOrderId == null) {
            // Skip if no order with fills found
            return;
        }

        List<Fill> fills = fillMapper.selectFillsByOrderId(testOrderId);
        assertNotNull(fills);
        assertFalse(fills.isEmpty());
        
        fills.forEach(fill -> {
            assertEquals(testOrderId, fill.getOrderId());
            assertNotNull(fill.getFillId());
            assertTrue(fill.getPrice().signum() >= 0);
            assertTrue(fill.getQuantity().signum() > 0);
            assertTrue(fill.getStatus() == FillStatus.Filled || 
                      fill.getStatus() == FillStatus.Failed || 
                      fill.getStatus() == FillStatus.Pending);
            assertNotNull(fill.getExecutedAt());
        });
    }

    @Test
    public void testCountFillsByOrderId() {
        if (testOrderId == null) {
            return;
        }

        int count = fillMapper.countFillsByOrderId(testOrderId);
        assertTrue(count > 0);
    }

    @Test
    public void testSelectFillById() {
        if (testOrderId == null) {
            return;
        }

        List<Fill> fills = fillMapper.selectFillsByOrderId(testOrderId);
        assertFalse(fills.isEmpty());
        
        Fill fillFromList = fills.get(0);
        
        // Now fetch by ID
        Fill fillById = fillMapper.selectFillById(fillFromList.getFillId());
        assertNotNull(fillById);
        assertEquals(fillFromList.getFillId(), fillById.getFillId());
        assertEquals(fillFromList.getOrderId(), fillById.getOrderId());
    }

    @Test
    public void testSelectFillsByStatus() {
        List<Fill> filledFills = fillMapper.selectFillsByStatus(FillStatus.Filled, 10, 0);
        assertNotNull(filledFills);
        
        filledFills.forEach(fill -> {
            assertEquals(FillStatus.Filled, fill.getStatus());
            assertNotNull(fill.getFillId());
            assertNotNull(fill.getOrderId());
        });
    }

    @Test
    public void testFillStatusValues() {
        List<Fill> allFills = fillMapper.selectFillsByStatus(FillStatus.Filled, 100, 0);
        List<Fill> pendingFills = fillMapper.selectFillsByStatus(FillStatus.Pending, 100, 0);
        List<Fill> failedFills = fillMapper.selectFillsByStatus(FillStatus.Failed, 100, 0);
        
        allFills.forEach(fill -> assertEquals(FillStatus.Filled, fill.getStatus()));
        pendingFills.forEach(fill -> assertEquals(FillStatus.Pending, fill.getStatus()));
        failedFills.forEach(fill -> assertEquals(FillStatus.Failed, fill.getStatus()));
    }

    @Test
    public void testFillPagination() {
        List<Fill> allFills = fillMapper.selectFillsByStatus(FillStatus.Filled, 1000, 0);
        
        if (allFills.size() > 5) {
            List<Fill> page1 = fillMapper.selectFillsByStatus(FillStatus.Filled, 5, 0);
            List<Fill> page2 = fillMapper.selectFillsByStatus(FillStatus.Filled, 5, 5);
            
            assertEquals(5, page1.size());
            assertEquals(5, page2.size());
            assertNotEquals(page1.get(0).getFillId(), page2.get(0).getFillId());
        }
    }

    @Test
    public void testFillHasTimestamp() {
        if (testOrderId == null) {
            return;
        }

        List<Fill> fills = fillMapper.selectFillsByOrderId(testOrderId);
        assertFalse(fills.isEmpty());
        
        Fill fill = fills.get(0);
        assertNotNull(fill.getExecutedAt());
    }
}
