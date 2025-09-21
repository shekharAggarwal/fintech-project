package com.fintech.paymentservice.model;

public record PaymentResult(boolean success, String code, String providerTxnId){}
