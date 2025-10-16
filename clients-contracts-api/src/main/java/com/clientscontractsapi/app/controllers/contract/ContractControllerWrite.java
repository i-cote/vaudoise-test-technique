package com.clientscontractsapi.app.controllers.contract;

import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.dto.CreateContractRequestDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractControllerWrite {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;

    public ContractControllerWrite(ContractRepository contractRepository, ClientRepository clientRepository) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
    }

    @PostMapping("/create-contract")
    public ResponseEntity<ContractDto> createContract(@Valid @RequestBody CreateContractRequestDto request) {
        Optional<ClientEntity> clientOpt = clientRepository.findById(request.getClientId());
        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        LocalDate startDate = Optional.ofNullable(request.getStartDate()).orElse(LocalDate.now());
        LocalDate endDate = request.getEndDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ContractEntity contract = new ContractEntity();
        contract.setClient(clientOpt.get());
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setCostAmount(request.getCostAmount());

        OffsetDateTime now = OffsetDateTime.now();
        contract.setCreatedAt(now);
        contract.setUpdatedAt(now);

        ContractEntity saved = contractRepository.save(contract);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
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
