package com.clientscontractsapi.app.controllers.contract;

import com.clientscontractsapi.app.models.contract.dto.ActiveContractsCostResponseDto;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        BigDecimal sum =
                contractRepository.sumActiveCostAmountByClient(clientId, LocalDate.now());
        BigDecimal value = sum != null ? sum : BigDecimal.ZERO;
        return ResponseEntity.ok(new ActiveContractsCostResponseDto(clientId, value));
    }
}
