package com.clientscontractsapi.app.controllers.client;

import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientControllerRead {

    private final ClientRepository clientRepository;

    public ClientControllerRead(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
        ClientEntity client =
                clientRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Client with id %d was not found.".formatted(id)));
        return ResponseEntity.ok(client);
    }
}
