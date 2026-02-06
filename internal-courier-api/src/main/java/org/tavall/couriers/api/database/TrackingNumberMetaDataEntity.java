package org.tavall.couriers.api.database;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tracking_number_metadata")
public class TrackingNumberMetaDataEntity {

    @Id
    @Column(name = "tracking_number", nullable = false, length = 64)
    private String trackingNumber;

    @Column(name = "qr_uuid", nullable = false)
    private UUID qrUuid;

    public TrackingNumberMetaDataEntity() {
    }

    public TrackingNumberMetaDataEntity(String trackingNumber, UUID qrUuid) {
        this.trackingNumber = trackingNumber;
        this.qrUuid = qrUuid;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public UUID getQrUuid() {
        return qrUuid;
    }

    public void setQrUuid(UUID qrUuid) {
        this.qrUuid = qrUuid;
    }
}
