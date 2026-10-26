package com.neueda.leap;

import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.AdminRole;
import com.neueda.leap.enums.AdminUserStatus;
import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.ClientStatus;
import com.neueda.leap.enums.FillStatus;
import com.neueda.leap.enums.InstrumentStatus;
import com.neueda.leap.enums.LoginOutcome;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.TradeEventEntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@DisplayName("Enum Contract Tests")
class EnumContractTest {

    @Test
    @DisplayName("ClientStatus matches the YAML contract")
    void clientStatusMatchesSpec() {
        assertEnumNames(ClientStatus.values(), "ACTIVE", "SUSPENDED", "CLOSED");
    }

    @Test
    @DisplayName("AccountStatus matches the YAML contract")
    void accountStatusMatchesSpec() {
        assertEnumNames(AccountStatus.values(), "ACTIVE", "SUSPENDED", "CLOSED");
    }

    @Test
    @DisplayName("OrderSide matches the YAML contract")
    void orderSideMatchesSpec() {
        assertEnumNames(OrderSide.values(), "BUY", "SELL");
    }

    @Test
    @DisplayName("FillStatus matches the YAML contract")
    void fillStatusMatchesSpec() {
        assertEnumNames(FillStatus.values(), "Filled", "Failed", "Pending");
    }

    @Test
    @DisplayName("AdminRole matches the YAML contract")
    void adminRoleMatchesSpec() {
        assertEnumNames(AdminRole.values(), "ADMIN", "ANALYST");
    }

    @Test
    @DisplayName("AdminUserStatus matches the YAML contract")
    void adminUserStatusMatchesSpec() {
        assertEnumNames(AdminUserStatus.values(), "ACTIVE", "SUSPENDED");
    }

    @Test
    @DisplayName("AssetClass matches the YAML contract")
    void assetClassMatchesSpec() {
        assertEnumNames(AssetClass.values(), "EQUITY", "FX", "CRYPTO");
    }

    @Test
    @DisplayName("InstrumentStatus matches the YAML contract")
    void instrumentStatusMatchesSpec() {
        assertEnumNames(InstrumentStatus.values(), "TRADABLE", "HALTED", "INACTIVE");
    }

    @Test
    @DisplayName("TradeEventEntityType matches the YAML contract")
    void tradeEventEntityTypeMatchesSpec() {
        assertEnumNames(TradeEventEntityType.values(), "ORDER", "FILL");
    }

    @Test
    @DisplayName("LoginOutcome matches the YAML contract")
    void loginOutcomeMatchesSpec() {
        assertEnumNames(LoginOutcome.values(), "SUCCESS", "FAILURE");
    }

    private static void assertEnumNames(Enum<?>[] actual, String... expected) {
        String[] actualNames = Arrays.stream(actual).map(Enum::name).toArray(String[]::new);
        assertArrayEquals(expected, actualNames);
    }
}
