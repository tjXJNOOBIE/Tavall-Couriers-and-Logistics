package org.tavall.couriers.api.shipping;


import org.junit.jupiter.api.Test;
import org.tavall.couriers.api.console.Log;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

public class ShippingLabelMetaDataTest {

    private ShippingLabelMetaData buildMetaData() {
        return new ShippingLabelMetaData(
                "uuid-123",
                "track-456",
                "Alex Rider",
                "555-0101",
                "123 Market St",
                "San Jose",
                "CA",
                "95112",
                "USA",
                true,
                Instant.parse("2024-01-01T10:15:30Z")
        );
    }

    @Test
    void getUuidTest() {
        Log.info("Starting test: getUuidTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("uuid-123", metaData.getUuid());
        Log.success("getUuidTest passed validation.");
    }

    @Test
    void setUuidTest() {
        Log.info("Starting test: setUuidTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setUuid("uuid-999");
        assertEquals("uuid-999", metaData.getUuid());
        Log.success("setUuidTest passed validation.");
    }

    @Test
    void getTrackingNumberTest() {
        Log.info("Starting test: getTrackingNumberTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("track-456", metaData.getTrackingNumber());
        Log.success("getTrackingNumberTest passed validation.");
    }

    @Test
    void setTrackingNumberTest() {
        Log.info("Starting test: setTrackingNumberTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setTrackingNumber("track-999");
        assertEquals("track-999", metaData.getTrackingNumber());
        Log.success("setTrackingNumberTest passed validation.");
    }

    @Test
    void getRecipientNameTest() {
        Log.info("Starting test: getRecipientNameTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("Alex Rider", metaData.getRecipientName());
        Log.success("getRecipientNameTest passed validation.");
    }

    @Test
    void setRecipientNameTest() {
        Log.info("Starting test: setRecipientNameTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setRecipientName("Jamie Doe");
        assertEquals("Jamie Doe", metaData.getRecipientName());
        Log.success("setRecipientNameTest passed validation.");
    }

    @Test
    void getPhoneNumberTest() {
        Log.info("Starting test: getPhoneNumberTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("555-0101", metaData.getPhoneNumber());
        Log.success("getPhoneNumberTest passed validation.");
    }

    @Test
    void setPhoneNumberTest() {
        Log.info("Starting test: setPhoneNumberTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setPhoneNumber("555-0202");
        assertEquals("555-0202", metaData.getPhoneNumber());
        Log.success("setPhoneNumberTest passed validation.");
    }

    @Test
    void getAddressTest() {
        Log.info("Starting test: getAddressTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("123 Market St", metaData.getAddress());
        Log.success("getAddressTest passed validation.");
    }

    @Test
    void setAddressTest() {
        Log.info("Starting test: setAddressTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setAddress("456 Elm St");
        assertEquals("456 Elm St", metaData.getAddress());
        Log.success("setAddressTest passed validation.");
    }

    @Test
    void getCityTest() {
        Log.info("Starting test: getCityTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("San Jose", metaData.getCity());
        Log.success("getCityTest passed validation.");
    }

    @Test
    void setCityTest() {
        Log.info("Starting test: setCityTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setCity("Oakland");
        assertEquals("Oakland", metaData.getCity());
        Log.success("setCityTest passed validation.");
    }

    @Test
    void getStateTest() {
        Log.info("Starting test: getStateTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("CA", metaData.getState());
        Log.success("getStateTest passed validation.");
    }

    @Test
    void setStateTest() {
        Log.info("Starting test: setStateTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setState("NV");
        assertEquals("NV", metaData.getState());
        Log.success("setStateTest passed validation.");
    }

    @Test
    void getZipCodeTest() {
        Log.info("Starting test: getZipCodeTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("95112", metaData.getZipCode());
        Log.success("getZipCodeTest passed validation.");
    }

    @Test
    void setZipCodeTest() {
        Log.info("Starting test: setZipCodeTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setZipCode("89101");
        assertEquals("89101", metaData.getZipCode());
        Log.success("setZipCodeTest passed validation.");
    }

    @Test
    void getCountryTest() {
        Log.info("Starting test: getCountryTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals("USA", metaData.getCountry());
        Log.success("getCountryTest passed validation.");
    }

    @Test
    void setCountryTest() {
        Log.info("Starting test: setCountryTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setCountry("Canada");
        assertEquals("Canada", metaData.getCountry());
        Log.success("setCountryTest passed validation.");
    }

    @Test
    void isPriorityTest() {
        Log.info("Starting test: isPriorityTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertTrue(metaData.isPriority());
        Log.success("isPriorityTest passed validation.");
    }

    @Test
    void setPriorityTest() {
        Log.info("Starting test: setPriorityTest");
        ShippingLabelMetaData metaData = buildMetaData();
        metaData.setPriority(false);
        assertFalse(metaData.isPriority());
        Log.success("setPriorityTest passed validation.");
    }

    @Test
    void getDeliverByTest() {
        Log.info("Starting test: getDeliverByTest");
        ShippingLabelMetaData metaData = buildMetaData();
        assertEquals(Instant.parse("2024-01-01T10:15:30Z"), metaData.getDeliverBy());
        Log.success("getDeliverByTest passed validation.");
    }

    @Test
    void setDeliverByTest() {
        Log.info("Starting test: setDeliverByTest");
        ShippingLabelMetaData metaData = buildMetaData();
        Instant updated = Instant.parse("2024-02-02T09:00:00Z");
        metaData.setDeliverBy(updated);
        assertEquals(updated, metaData.getDeliverBy());
        Log.success("setDeliverByTest passed validation.");
    }

    @Test
    void equalsTest() {
        Log.info("Starting test: equalsTest");
        ShippingLabelMetaData metaData = buildMetaData();
        ShippingLabelMetaData sameUuid = new ShippingLabelMetaData(
                "uuid-123",
                "track-789",
                "Different Name",
                "555-9999",
                "789 Pine St",
                "Seattle",
                "WA",
                "98101",
                "USA",
                false,
                Instant.parse("2024-03-03T12:00:00Z")
        );
        ShippingLabelMetaData differentUuid = buildMetaData();
        differentUuid.setUuid("uuid-456");

        assertEquals(metaData, sameUuid);
        assertNotEquals(metaData, differentUuid);
        Log.success("equalsTest passed validation.");
    }

    @Test
    void hashCodeTest() {
        Log.info("Starting test: hashCodeTest");
        ShippingLabelMetaData metaData = buildMetaData();
        ShippingLabelMetaData sameUuid = buildMetaData();
        ShippingLabelMetaData differentUuid = buildMetaData();
        differentUuid.setUuid("uuid-456");

        assertEquals(metaData.hashCode(), sameUuid.hashCode());
        assertNotEquals(metaData.hashCode(), differentUuid.hashCode());
        Log.success("hashCodeTest passed validation.");
    }

    @Test
    void toStringTest() {
        Log.info("Starting test: toStringTest");
        ShippingLabelMetaData metaData = buildMetaData();
        String output = metaData.toString();


    assertTrue(output.contains("ShippingLabelMetaData{"));
    assertTrue(output.contains("uuid='uuid-123'"));
    assertTrue(output.contains("trackingNumber='track-456'"));
    assertTrue(output.contains("recipientName='Alex Rider'"));
    assertTrue(output.contains("city='San Jose'"));
    assertTrue(output.contains("state='CA'"));
    assertTrue(output.contains("country='USA'"));
    assertTrue(output.contains("priority=true"));
    assertTrue(output.contains("deliverBy=2024-01-01T10:15:30Z"));
        Log.success("toStringTest passed validation.");
}
}