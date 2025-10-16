package com.clientscontractsapi.app.controllers.client;

import com.clientscontractsapi.app.models.client.dto.CreateClientRequestDto;
import com.clientscontractsapi.app.models.client.dto.UpdateClientRequestDto;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientControllerWrite {

    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;

    public ClientControllerWrite(ClientRepository clientRepository, ContractRepository contractRepository) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
    }

    @PostMapping("/create-client")
    public ResponseEntity<ClientEntity> createClient(@Valid @RequestBody CreateClientRequestDto request) {
        boolean isCompany = StringUtils.hasText(request.getCompanyIdentifier());

        if (isCompany && request.getBirthdate() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (!isCompany && request.getBirthdate() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        ClientEntity client = new ClientEntity();
        client.setClientType(isCompany ? "COMPANY" : "PERSON");
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setName(request.getName());
        client.setBirthdate(isCompany ? null : request.getBirthdate());
        client.setCompanyIdentifier(isCompany ? request.getCompanyIdentifier() : null);

        OffsetDateTime now = OffsetDateTime.now();
        client.setCreatedAt(now);
        client.setUpdatedAt(now);

        ClientEntity saved = clientRepository.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/update-client")
    public ResponseEntity<ClientEntity> updateClient(@Valid @RequestBody UpdateClientRequestDto request) {
        Optional<ClientEntity> existingClient = clientRepository.findById(request.getId());
        if (existingClient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ClientEntity client = existingClient.get();
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setName(request.getName());

        ClientEntity saved = clientRepository.save(client);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/delete-client/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        Optional<ClientEntity> clientOpt = clientRepository.findById(id);
        if (clientOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        LocalDate today = LocalDate.now();
        List<ContractEntity> contracts = contractRepository.findByClientId(id);
        if (!contracts.isEmpty()) {
            contracts.forEach(contract -> contract.setEndDate(today));
            contractRepository.saveAll(contracts);
        }

        clientRepository.delete(clientOpt.get());
        return ResponseEntity.noContent().build();
    }
}
