package org.tavall.couriers.api.web.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "delivery_route_stops", schema = "courier_schemas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryRouteStopEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "route_id", nullable = false, length = 40)
    private String routeId;

    @Column(name = "label_uuid", nullable = false, length = 36)
    private String labelUuid;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public DeliveryRouteStopEntity() {
    }

    public DeliveryRouteStopEntity(String id,
                                   String routeId,
                                   String labelUuid,
                                   int stopOrder,
                                   Instant createdAt) {
        this.id = id;
        this.routeId = routeId;
        this.labelUuid = labelUuid;
        this.stopOrder = stopOrder;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getLabelUuid() {
        return labelUuid;
    }

    public void setLabelUuid(String labelUuid) {
        this.labelUuid = labelUuid;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
