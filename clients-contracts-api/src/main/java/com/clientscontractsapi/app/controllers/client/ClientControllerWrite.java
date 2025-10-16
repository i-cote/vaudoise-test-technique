package com.clientscontractsapi.app.controllers.client;

import com.clientscontractsapi.app.models.client.dto.CreateClientRequestDto;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientControllerWrite {

    private final ClientRepository clientRepository;

    public ClientControllerWrite(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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
}
