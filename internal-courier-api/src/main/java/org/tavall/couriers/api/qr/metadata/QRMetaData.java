package org.tavall.couriers.api.qr.metadata;


import org.tavall.couriers.api.qr.enums.QRState;
import org.tavall.couriers.api.qr.enums.QRType;
import org.tavall.couriers.api.utils.uuid.GenerateUUID;

import java.time.Instant;

public class QRMetaData {
    private final GenerateUUID uuid;
    private final String qrData;
    private final Instant createdAt;
    private final QRType qrType;
    private QRState qrState;

    public QRMetaData(QRType qrType, GenerateUUID uuid, String qrData, Instant createdAt, QRState qrState) {
        this.qrType = qrType;
        this.uuid = uuid;
        this.qrData = qrData;
        this.createdAt = createdAt;
        this.qrState = qrState;
    }

    public QRType getQrType() {
        return qrType;
    }

    public GenerateUUID getUuid() {
        return uuid;
    }

    public String getQRData() {
        return qrData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public QRState getQrState() {
        return qrState;
    }

    public void setQrState(QRState qrState) {
        this.qrState = qrState;
    }
}