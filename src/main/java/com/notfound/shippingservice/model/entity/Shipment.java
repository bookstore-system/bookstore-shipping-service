package com.notfound.shippingservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shipment", uniqueConstraints = @UniqueConstraint(name = "uk_shipment_saga", columnNames = "saga_id"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shipment {

    @Id
    @GeneratedValue
    @UuidGenerator
    UUID id;

    @Column(name = "saga_id", nullable = false)
    UUID sagaId;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "shipping_order_code")
    String shippingOrderCode;

    @Column(name = "cod_amount")
    Integer codAmount;

    Double totalFee;

    @Column(name = "expected_delivery_time")
    LocalDateTime expectedDeliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    Status status;

    @Column(name = "last_error", length = 1024)
    String lastError;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public enum Status {
        CREATED,
        CANCELLED,
        FAILED
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
