package org.tavall.couriers.api.web.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "delivery_routes", schema = "courier_schemas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryRouteEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "route_id", nullable = false, length = 40)
    private String routeId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "label_count", nullable = false)
    private int labelCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    public DeliveryRouteEntity() {
    }

    public DeliveryRouteEntity(String routeId,
                               String status,
                               int labelCount,
                               Instant createdAt,
                               Instant updatedAt,
                               String notes) {
        this.routeId = routeId;
        this.status = status;
        this.labelCount = labelCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.notes = notes;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLabelCount() {
        return labelCount;
    }

    public void setLabelCount(int labelCount) {
        this.labelCount = labelCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
