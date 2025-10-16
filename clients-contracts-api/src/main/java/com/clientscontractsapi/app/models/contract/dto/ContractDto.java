package com.clientscontractsapi.app.models.contract.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ContractDto {

    private final Long id;
    private final Long clientId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final BigDecimal costAmount;
    private final OffsetDateTime createdAt;

    public ContractDto(
            Long id,
            Long clientId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal costAmount,
            OffsetDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.costAmount = costAmount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getClientId() {
        return clientId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
