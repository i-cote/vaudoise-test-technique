package com.clientscontractsapi.app.models.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateCostAmountRequestDto {

    @NotNull
    private Long contractId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal costAmount;

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }
}
