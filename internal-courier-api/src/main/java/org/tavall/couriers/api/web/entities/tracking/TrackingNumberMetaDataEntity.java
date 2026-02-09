package org.tavall.couriers.api.web.entities.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "tracking_number_metadata", schema = "courier_schemas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TrackingNumberMetaDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

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
