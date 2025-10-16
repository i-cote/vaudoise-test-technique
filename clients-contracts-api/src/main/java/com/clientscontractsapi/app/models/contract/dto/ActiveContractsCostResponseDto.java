package com.clientscontractsapi.app.models.contract.dto;

import java.math.BigDecimal;

public class ActiveContractsCostResponseDto {

    private final Long clientId;
    private final BigDecimal activeCostAmount;

    public ActiveContractsCostResponseDto(Long clientId, BigDecimal activeCostAmount) {
        this.clientId = clientId;
        this.activeCostAmount = activeCostAmount;
    }

    public Long getClientId() {
        return clientId;
    }

    public BigDecimal getActiveCostAmount() {
        return activeCostAmount;
    }
}
