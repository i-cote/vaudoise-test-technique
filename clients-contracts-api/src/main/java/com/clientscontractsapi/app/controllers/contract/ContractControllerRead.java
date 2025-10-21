package com.clientscontractsapi.app.controllers.contract;

import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.contract.dto.ActiveContractsCostResponseDto;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractControllerRead {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;

    public ContractControllerRead(ContractRepository contractRepository, ClientRepository clientRepository) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
    }

    @GetMapping("/clients/{clientId}/active-cost")
    public ResponseEntity<ActiveContractsCostResponseDto> getActiveContractsCost(@PathVariable Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client with id %d was not found.".formatted(clientId));
        }

        BigDecimal sum =
                contractRepository.sumActiveCostAmountByClient(clientId, LocalDate.now());
        BigDecimal value = sum != null ? sum : BigDecimal.ZERO;
        return ResponseEntity.ok(new ActiveContractsCostResponseDto(clientId, value));
    }

    @GetMapping("/clients/{clientId}/contracts")
    public ResponseEntity<List<ContractDto>> getActiveContractsForClient(
            @PathVariable Long clientId,
            @RequestParam(value = "updatedSince", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime updatedSince) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client with id %d was not found.".formatted(clientId));
        }

        LocalDate today = LocalDate.now();
        List<ContractEntity> contracts =
                updatedSince == null
                        ? contractRepository.findActiveContractsByClient(clientId, today)
                        : contractRepository.findActiveContractsByClientAndUpdatedSince(
                                clientId, today, updatedSince);

        List<ContractDto> dtos = contracts.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ContractDto toDto(ContractEntity entity) {
        return new ContractDto(
                entity.getId(),
                entity.getClient().getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCostAmount(),
                entity.getCreatedAt());
    }
}
