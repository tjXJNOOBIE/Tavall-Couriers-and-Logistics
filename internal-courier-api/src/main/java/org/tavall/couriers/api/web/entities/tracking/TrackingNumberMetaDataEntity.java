package org.tavall.couriers.api.web.entities.tracking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import org.tavall.couriers.api.delivery.state.DeliveryState;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_state", nullable = false, length = 32)
    private DeliveryState deliveryState = DeliveryState.LABEL_CREATED;

    public TrackingNumberMetaDataEntity() {
    }

    public TrackingNumberMetaDataEntity(String trackingNumber, UUID qrUuid) {
        this(trackingNumber, qrUuid, DeliveryState.LABEL_CREATED);
    }

    public TrackingNumberMetaDataEntity(String trackingNumber, UUID qrUuid, DeliveryState deliveryState) {
        this.trackingNumber = trackingNumber;
        this.qrUuid = qrUuid;
        this.deliveryState = deliveryState != null ? deliveryState : DeliveryState.LABEL_CREATED;
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

    public DeliveryState getDeliveryState() {
        return deliveryState;
    }

    public void setDeliveryState(DeliveryState deliveryState) {
        this.deliveryState = deliveryState != null ? deliveryState : DeliveryState.LABEL_CREATED;
    }
}
