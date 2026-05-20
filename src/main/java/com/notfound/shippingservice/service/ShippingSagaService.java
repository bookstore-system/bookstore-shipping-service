package com.notfound.shippingservice.service;

import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;

public interface ShippingSagaService {

    void handleCreateCommand(SagaMessageEnvelope command);

    void handleCancelCommand(SagaMessageEnvelope command);
}
