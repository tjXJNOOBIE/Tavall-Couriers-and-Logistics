package org.tavall.couriers.api.shipping;



import org.tavall.couriers.api.delivery.state.DeliveryState;

import java.time.Instant;
import java.util.Objects;

public class ShippingLabelMetaData {
    private String uuid;
    private String trackingNumber;
    private String recipientName;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    private boolean priority;
    private Instant deliverBy;


    public DeliveryState getDeliveryState() {

        return deliveryState;
    }


    public void setDeliveryState(DeliveryState deliveryState) {

        this.deliveryState = deliveryState;
    }


    private DeliveryState deliveryState;

    public ShippingLabelMetaData() {
    }

    public ShippingLabelMetaData(
            String uuid,
            String trackingNumber,
            String recipientName,
            String phoneNumber,
            String address,
            String city,
            String state,
            String zipCode,
            String country,
            boolean priority,
            Instant deliverBy,
            DeliveryState deliveryState) {
        this.uuid = uuid;
        this.trackingNumber = trackingNumber;
        this.recipientName = recipientName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.priority = priority;
        this.deliverBy = deliverBy;
        this.deliveryState = deliveryState;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public Instant getDeliverBy() {
        return deliverBy;
    }

    public void setDeliverBy(Instant deliverBy) {
        this.deliverBy = deliverBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShippingLabelMetaData that)) return false;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public String toString() {
        return "ShippingLabelMetaData{" +
                "uuid='" + uuid + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", recipientName='" + recipientName + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", priority=" + priority +
                ", deliverBy=" + deliverBy +
                '}';
    }
}