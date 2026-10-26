package com.neueda.leap;

import com.neueda.leap.enums.AssetClass;
import com.neueda.leap.enums.InstrumentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Instrument API Spec Contract Tests")
class InstrumentSpecContractTest {

    @Test
    @DisplayName("Instrument defaults to TRADABLE status")
    void instrumentDefaultsToTradableStatus() {
        Instrument instrument = new Instrument();

        assertEquals(InstrumentStatus.TRADABLE, instrument.getStatus());
    }

    @Test
    @DisplayName("InstrumentCreateRequest matches the create-instrument payload")
    void instrumentCreateRequestMatchesSpec() throws Exception {
        Class<?> requestType = Class.forName("com.neueda.leap.InstrumentCreateRequest");
        Object request = instantiate(requestType);

        assertHasField(requestType, "symbol", String.class);
        assertHasField(requestType, "name", String.class);
        assertHasField(requestType, "assetClass", AssetClass.class);
        assertNoField(requestType, "instrumentId");
        assertNoField(requestType, "status");

        assertPropertyRoundTrip(request, requestType, "symbol", String.class, "AAPL");
        assertPropertyRoundTrip(request, requestType, "name", String.class, "Apple Inc.");
        assertPropertyRoundTrip(request, requestType, "assetClass", AssetClass.class, AssetClass.EQUITY);
    }

    @Test
    @DisplayName("InstrumentStatusUpdateRequest matches the status update payload")
    void instrumentStatusUpdateRequestMatchesSpec() throws Exception {
        Class<?> requestType = Class.forName("com.neueda.leap.InstrumentStatusUpdateRequest");
        Object request = instantiate(requestType);

        assertHasField(requestType, "status", InstrumentStatus.class);
        assertNoField(requestType, "symbol");
        assertNoField(requestType, "name");
        assertNoField(requestType, "assetClass");
        assertNoField(requestType, "instrumentId");

        assertPropertyRoundTrip(request, requestType, "status", InstrumentStatus.class, InstrumentStatus.HALTED);
    }

    private static Object instantiate(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void assertHasField(Class<?> type, String fieldName, Class<?> fieldType) throws Exception {
        Field field = type.getDeclaredField(fieldName);
        assertEquals(fieldType, field.getType(), "Unexpected type for field '" + fieldName + "'");
    }

    private static void assertNoField(Class<?> type, String fieldName) {
        assertThrows(NoSuchFieldException.class, () -> type.getDeclaredField(fieldName));
    }

    private static void assertPropertyRoundTrip(Object instance, Class<?> type, String propertyName, Class<?> propertyType, Object value) throws Exception {
        String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        Method setter = type.getMethod("set" + suffix, propertyType);
        Method getter = type.getMethod("get" + suffix);

        setter.invoke(instance, value);
        assertEquals(value, getter.invoke(instance), "Property round-trip failed for '" + propertyName + "'");
    }
}
