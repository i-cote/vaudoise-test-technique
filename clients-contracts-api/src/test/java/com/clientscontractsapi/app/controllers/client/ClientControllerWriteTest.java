package com.clientscontractsapi.app.controllers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.models.client.dto.CreateClientRequestDto;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ClientControllerWriteTest {

    private ClientRepository clientRepository;
    private ClientControllerWrite clientControllerWrite;

    @BeforeEach
    void setUp() {
        clientRepository = Mockito.mock(ClientRepository.class);
        clientControllerWrite = new ClientControllerWrite(clientRepository);
    }

    @Test
    void createPersonClientPersistsAndReturnsCreated() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("john.doe@example.com");
        request.setPhone("+123456789");
        request.setName("John Doe");
        request.setBirthdate(LocalDate.of(1990, 1, 1));

        ClientEntity savedEntity = new ClientEntity();
        savedEntity.setId(100L);

        when(clientRepository.save(ArgumentMatchers.any(ClientEntity.class))).thenReturn(savedEntity);

        ResponseEntity<ClientEntity> response = clientControllerWrite.createClient(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(savedEntity, response.getBody());

        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).save(captor.capture());
        verifyNoMoreInteractions(clientRepository);

        ClientEntity persisted = captor.getValue();
        assertEquals("PERSON", persisted.getClientType());
        assertEquals(request.getEmail(), persisted.getEmail());
        assertEquals(request.getPhone(), persisted.getPhone());
        assertEquals(request.getName(), persisted.getName());
        assertEquals(request.getBirthdate(), persisted.getBirthdate());
        assertNull(persisted.getCompanyIdentifier());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void createCompanyClientPersistsAndReturnsCreated() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("acme@example.com");
        request.setPhone("+987654321");
        request.setName("ACME Inc.");
        request.setCompanyIdentifier("CHE-123.456.789");

        ClientEntity savedEntity = new ClientEntity();
        savedEntity.setId(200L);

        when(clientRepository.save(ArgumentMatchers.any(ClientEntity.class))).thenReturn(savedEntity);

        ResponseEntity<ClientEntity> response = clientControllerWrite.createClient(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(savedEntity, response.getBody());

        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).save(captor.capture());
        verifyNoMoreInteractions(clientRepository);

        ClientEntity persisted = captor.getValue();
        assertEquals("COMPANY", persisted.getClientType());
        assertEquals(request.getEmail(), persisted.getEmail());
        assertEquals(request.getPhone(), persisted.getPhone());
        assertEquals(request.getName(), persisted.getName());
        assertNull(persisted.getBirthdate());
        assertEquals(request.getCompanyIdentifier(), persisted.getCompanyIdentifier());
        assertNotNull(persisted.getCreatedAt());
        assertNotNull(persisted.getUpdatedAt());
    }

    @Test
    void createPersonClientWithoutBirthdateReturnsBadRequest() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("jane.doe@example.com");
        request.setPhone("+123450987");
        request.setName("Jane Doe");

        ResponseEntity<ClientEntity> response = clientControllerWrite.createClient(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verifyNoMoreInteractions(clientRepository);
    }
}
