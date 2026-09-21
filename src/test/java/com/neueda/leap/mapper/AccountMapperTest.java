package com.neueda.leap.mapper;

import com.neueda.leap.Account;
import com.neueda.leap.Client;
import com.neueda.leap.enums.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class AccountMapperTest {

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private ClientMapper clientMapper;

    private UUID testClientId;

    @BeforeEach
    public void setUp() {
        // Get a test client ID
        Client client = clientMapper.selectClientByEmail("alice.johnson@example.com");
        assertNotNull(client, "Client 'alice.johnson@example.com' not found - check database migration");
        testClientId = client.getClientId();
    }

    @Test
    public void testSelectAccountsByClientId() {
        List<Account> accounts = accountMapper.selectAccountsByClientId(testClientId, 10, 0);
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());
        
        accounts.forEach(account -> {
            assertEquals(testClientId, account.getClientId());
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertNotNull(account.getOpenedAt());
        });
    }

    @Test
    public void testCountAccountsByClientId() {
        int count = accountMapper.countAccountsByClientId(testClientId);
        assertTrue(count >= 1); // Each client has at least 1 account
    }

    @Test
    public void testSelectAccountById() {
        // Get an account first
        List<Account> accounts = accountMapper.selectAccountsByClientId(testClientId, 1, 0);
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());
        
        Account accountFromList = accounts.get(0);
        
        // Now fetch by ID
        Account accountById = accountMapper.selectAccountById(accountFromList.getAccountId());
        assertNotNull(accountById);
        assertEquals(accountFromList.getAccountId(), accountById.getAccountId());
        assertEquals(accountFromList.getClientId(), accountById.getClientId());
    }

    @Test
    public void testSelectAccountByIdNotFound() {
        Account account = accountMapper.selectAccountById(UUID.randomUUID());
        assertNull(account);
    }

    @Test
    public void testTotalAccountsInDatabase() {
        // Check if ANY accounts exist at all
        List<Account> allAccounts = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 100000, 0);
        System.out.println("TOTAL ACTIVE accounts in database: " + (allAccounts != null ? allAccounts.size() : "null"));
        
        assertTrue(allAccounts != null && allAccounts.size() > 0, "NO ACCOUNTS IN DATABASE");
        
        if (allAccounts.size() > 0) {
            System.out.println("First account clientId: " + allAccounts.get(0).getClientId());
            System.out.println("First account accountId: " + allAccounts.get(0).getAccountId());
        }
    }

    @Test
    public void testClientIdMatching() {
        System.out.println("testClientId from setUp: " + testClientId);
        System.out.println("testClientId type: " + (testClientId == null ? "null" : testClientId.getClass().getName()));
        
        // Get first account from DB
        List<Account> allAccounts = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 1, 0);
        if (allAccounts.size() > 0) {
            Account firstAccount = allAccounts.get(0);
            System.out.println("First account in DB - clientId: " + firstAccount.getClientId());
            System.out.println("First account in DB - clientId type: " + firstAccount.getClientId().getClass().getName());
            System.out.println("Do they match? " + testClientId.equals(firstAccount.getClientId()));
        }
        
        // Now try to query for accounts with testClientId
        List<Account> accountsForTestClient = accountMapper.selectAccountsByClientId(testClientId, 10, 0);
        System.out.println("Accounts found for testClientId: " + (accountsForTestClient == null ? "null" : accountsForTestClient.size()));
    }

    @Test
    public void testSelectAccountsByStatus() {
        List<Account> activeAccounts = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 10, 0);
        assertNotNull(activeAccounts);
        assertFalse(activeAccounts.isEmpty());
        
        activeAccounts.forEach(account -> {
            assertEquals(AccountStatus.ACTIVE, account.getStatus());
            assertNotNull(account.getAccountId());
            assertNotNull(account.getClientId());
        });
    }

    @Test
    public void testAccountPagination() {
        // Get multiple clients and their accounts
        List<Account> allActiveAccounts = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 100, 0);
        
        if (allActiveAccounts.size() > 5) {
            List<Account> page1 = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 5, 0);
            List<Account> page2 = accountMapper.selectAccountsByStatus(AccountStatus.ACTIVE, 5, 5);
            
            assertEquals(5, page1.size());
            assertEquals(5, page2.size());
            assertNotEquals(page1.get(0).getAccountId(), page2.get(0).getAccountId());
        }
    }

    @Test
    public void testAccountHasTimestamps() {
        List<Account> accounts = accountMapper.selectAccountsByClientId(testClientId, 1, 0);
        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());
        
        Account account = accounts.get(0);
        assertNotNull(account.getOpenedAt());
    }
}
