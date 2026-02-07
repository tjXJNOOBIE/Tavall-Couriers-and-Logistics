package org.tavall.couriers.api.qr.scan.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.tavall.couriers.api.qr.scan.state.LiveCameraState;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "scan_response", schema = "courier_schemas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ScanResponseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid", nullable = false, length = 36)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_state", nullable = false, length = 40)
    private LiveCameraState cameraState;

    @Column(name = "tracking_number", length = 64)
    private String trackingNumber;

    @Column(name = "name", length = 160)
    private String name;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "deadline")
    private Instant deadline;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public ScanResponseEntity() {
    }

    public ScanResponseEntity(
            String uuid,
            LiveCameraState cameraState,
            String trackingNumber,
            String name,
            String address,
            String city,
            String state,
            String zipCode,
            String country,
            String phoneNumber,
            Instant deadline,
            String notes) {
        this.uuid = uuid;
        this.cameraState = cameraState;
        this.trackingNumber = trackingNumber;
        this.name = name;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.deadline = deadline;
        this.notes = notes;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public LiveCameraState getCameraState() {
        return cameraState;
    }

    public void setCameraState(LiveCameraState cameraState) {
        this.cameraState = cameraState;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
