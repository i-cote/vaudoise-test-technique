package com.clientscontractsapi.unit.controllers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.controllers.client.ClientControllerWrite;
import com.clientscontractsapi.app.exceptions.BadRequestException;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.client.dto.CreateClientRequestDto;
import com.clientscontractsapi.app.models.client.dto.UpdateClientRequestDto;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ClientControllerWriteTest {

    private ClientRepository clientRepository;
    private ContractRepository contractRepository;
    private ClientControllerWrite clientControllerWrite;

    @BeforeEach
    void setUp() {
        clientRepository = Mockito.mock(ClientRepository.class);
        contractRepository = Mockito.mock(ContractRepository.class);
        clientControllerWrite = new ClientControllerWrite(clientRepository, contractRepository);
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

        when(clientRepository.existsByEmailIgnoreCase("john.doe@example.com")).thenReturn(false);
        when(clientRepository.save(ArgumentMatchers.any(ClientEntity.class))).thenReturn(savedEntity);

        ResponseEntity<ClientEntity> response = clientControllerWrite.createClient(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(savedEntity, response.getBody());

        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).existsByEmailIgnoreCase("john.doe@example.com");
        verify(clientRepository).save(captor.capture());
        verifyNoMoreInteractions(clientRepository, contractRepository);

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

        when(clientRepository.existsByEmailIgnoreCase("acme@example.com")).thenReturn(false);
        when(clientRepository.save(ArgumentMatchers.any(ClientEntity.class))).thenReturn(savedEntity);

        ResponseEntity<ClientEntity> response = clientControllerWrite.createClient(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(savedEntity, response.getBody());

        ArgumentCaptor<ClientEntity> captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(clientRepository).existsByEmailIgnoreCase("acme@example.com");
        verify(clientRepository).save(captor.capture());
        verifyNoMoreInteractions(clientRepository, contractRepository);

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
    void updateExistingPersonClientReturnsOk() {
        UpdateClientRequestDto request = new UpdateClientRequestDto();
        request.setId(42L);
        request.setEmail("updated.john@example.com");
        request.setPhone("+111222333");
        request.setName("John Updated");

        ClientEntity existing = new ClientEntity();
        existing.setId(42L);
        existing.setClientType("PERSON");
        existing.setEmail("old@example.com");
        existing.setPhone("+999888777");
        existing.setName("Old John");

        when(clientRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ResponseEntity<ClientEntity> response = clientControllerWrite.updateClient(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(existing, response.getBody());
        assertEquals(request.getEmail(), existing.getEmail());
        assertEquals(request.getPhone(), existing.getPhone());
        assertEquals(request.getName(), existing.getName());

        verify(clientRepository).findById(42L);
        verify(clientRepository).save(existing);
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void createPersonClientRequiresBirthdate() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("jane@example.com");
        request.setPhone("+111222333");
        request.setName("Jane Doe");

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> clientControllerWrite.createClient(request));

        assertEquals("Persons must include a birthdate.", exception.getMessage());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void createCompanyClientMustNotIncludeBirthdate() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("corp@example.com");
        request.setPhone("+444555666");
        request.setName("Corp");
        request.setCompanyIdentifier("CHE-123.456.789");
        request.setBirthdate(LocalDate.of(1990, 1, 1));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> clientControllerWrite.createClient(request));

        assertEquals("Companies must not include a birthdate.", exception.getMessage());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void createClientThrowsWhenEmailAlreadyExists() {
        CreateClientRequestDto request = new CreateClientRequestDto();
        request.setEmail("existing@example.com");
        request.setPhone("+123123123");
        request.setName("Existing Person");
        request.setBirthdate(LocalDate.of(1995, 5, 10));

        when(clientRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> clientControllerWrite.createClient(request));

        assertEquals("Client with email existing@example.com already exists.", exception.getMessage());

        verify(clientRepository).existsByEmailIgnoreCase("existing@example.com");
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void updateClientThrowsNotFoundWhenMissing() {
        UpdateClientRequestDto request = new UpdateClientRequestDto();
        request.setId(404L);
        request.setEmail("missing@example.com");
        request.setPhone("+10101010");
        request.setName("Missing Client");

        when(clientRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> clientControllerWrite.updateClient(request));

        assertEquals("Client with id 404 was not found.", exception.getMessage());

        verify(clientRepository).findById(404L);
        verify(contractRepository, never()).findByClientId(Mockito.anyLong());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void deleteClientUpdatesContractsAndDeletesClient() {
        ClientEntity client = new ClientEntity();
        client.setId(7L);

        ContractEntity activeContract = new ContractEntity();
        activeContract.setId(100L);
        activeContract.setEndDate(null);

        ContractEntity endedContract = new ContractEntity();
        endedContract.setId(101L);
        endedContract.setEndDate(LocalDate.of(2023, 5, 1));

        List<ContractEntity> contracts = Arrays.asList(activeContract, endedContract);

        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(contractRepository.findByClientId(7L)).thenReturn(contracts);

        ResponseEntity<Void> response = clientControllerWrite.deleteClient(7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        LocalDate today = LocalDate.now();
        assertEquals(today, activeContract.getEndDate());
        assertEquals(today, endedContract.getEndDate());

        verify(clientRepository).findById(7L);
        verify(contractRepository).findByClientId(7L);
        verify(contractRepository).saveAll(contracts);
        verify(clientRepository).delete(client);
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void deleteClientWithNoContractsStillDeletesClient() {
        ClientEntity client = new ClientEntity();
        client.setId(8L);

        when(clientRepository.findById(8L)).thenReturn(Optional.of(client));
        when(contractRepository.findByClientId(8L)).thenReturn(Collections.emptyList());

        ResponseEntity<Void> response = clientControllerWrite.deleteClient(8L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(clientRepository).findById(8L);
        verify(contractRepository).findByClientId(8L);
        verify(contractRepository, never()).saveAll(Mockito.anyList());
        verify(clientRepository).delete(client);
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void deleteClientThrowsNotFoundWhenMissing() {
        when(clientRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> clientControllerWrite.deleteClient(404L));

        assertEquals("Client with id 404 was not found.", exception.getMessage());

        verify(clientRepository).findById(404L);
        verify(contractRepository, never()).findByClientId(Mockito.anyLong());
        verify(contractRepository, never()).saveAll(Mockito.anyList());
        verify(clientRepository, never()).delete(Mockito.any());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }
}
