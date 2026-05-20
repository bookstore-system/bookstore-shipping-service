package com.notfound.shippingservice.repository;

import com.notfound.shippingservice.model.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    Optional<Shipment> findBySagaId(UUID sagaId);
}
