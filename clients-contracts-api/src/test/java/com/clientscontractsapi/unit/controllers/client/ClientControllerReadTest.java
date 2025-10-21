package com.clientscontractsapi.unit.controllers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.controllers.client.ClientControllerRead;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ClientControllerReadTest {

    private ClientRepository clientRepository;
    private ClientControllerRead clientController;

    @BeforeEach
    void setUp() {
        clientRepository = Mockito.mock(ClientRepository.class);
        clientController = new ClientControllerRead(clientRepository);
    }

    @Test
    void getClientByIdReturnsClientWhenFound() {
        ClientEntity client = new ClientEntity();
        client.setId(42L);
        client.setClientType("PERSON");
        client.setEmail("john.doe@example.com");
        client.setPhone("+123456789");
        client.setName("John Doe");
        client.setBirthdate(LocalDate.of(1990, 1, 1));
        client.setCreatedAt(OffsetDateTime.now());
        client.setUpdatedAt(OffsetDateTime.now());

        when(clientRepository.findById(42L)).thenReturn(Optional.of(client));

        ResponseEntity<ClientEntity> response = clientController.getClientById(42L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(client, response.getBody());
        verify(clientRepository).findById(42L);
        verifyNoMoreInteractions(clientRepository);
    }

    @Test
    void getClientByIdThrowsWhenMissing() {
        when(clientRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> clientController.getClientById(404L));

        assertEquals("Client with id 404 was not found.", exception.getMessage());
        verify(clientRepository).findById(404L);
        verifyNoMoreInteractions(clientRepository);
    }
}
