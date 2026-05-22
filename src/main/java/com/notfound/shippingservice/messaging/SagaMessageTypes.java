package com.notfound.shippingservice.messaging;

public final class SagaMessageTypes {

    public static final String CREATE_COMMAND = "shipping.create.command";
    public static final String CANCEL_COMMAND = "shipping.cancel.command";

    public static final String CREATED_EVENT = "shipping.created";
    public static final String CANCELLED_EVENT = "shipping.cancelled";
    public static final String FAILED_EVENT = "shipping.failed";

    public static final String RK_CREATE_COMMAND = "shipping.create.command";
    public static final String RK_CANCEL_COMMAND = "shipping.cancel.command";

    public static final String RK_CREATED_EVENT = "shipping.created";
    public static final String RK_CANCELLED_EVENT = "shipping.cancelled";
    public static final String RK_FAILED_EVENT = "shipping.failed";

    private SagaMessageTypes() {}
}
