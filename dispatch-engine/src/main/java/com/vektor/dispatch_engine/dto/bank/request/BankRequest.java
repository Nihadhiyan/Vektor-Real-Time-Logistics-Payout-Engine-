package com.vektor.dispatch_engine.dto.bank.request;

import java.math.BigDecimal;

public record BankRequest(
    String driverId,
    BigDecimal amount
) {}
