package org.tavall.couriers.api.qr.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "qr_metadata", schema = "courier_schemas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class QRMetaDataEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "uuid", nullable = false)
    private UUID uuid;

    @Column(name = "qr_data", nullable = false, columnDefinition = "text")
    private String qrData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_type", nullable = false, length = 40)
    private QRType qrType;

    @Enumerated(EnumType.STRING)
    @Column(name = "qr_state", nullable = false, length = 40)
    private QRState qrState;

    public QRMetaDataEntity() {
    }

    public QRMetaDataEntity(UUID uuid, String qrData, Instant createdAt, QRType qrType, QRState qrState) {
        this.uuid = uuid;
        this.qrData = qrData;
        this.createdAt = createdAt;
        this.qrType = qrType;
        this.qrState = qrState;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getQrData() {
        return qrData;
    }

    public void setQrData(String qrData) {
        this.qrData = qrData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public QRType getQrType() {
        return qrType;
    }

    public void setQrType(QRType qrType) {
        this.qrType = qrType;
    }

    public QRState getQrState() {
        return qrState;
    }

    public void setQrState(QRState qrState) {
        this.qrState = qrState;
    }
}
