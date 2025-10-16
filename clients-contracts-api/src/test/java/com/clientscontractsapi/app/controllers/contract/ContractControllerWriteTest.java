package com.clientscontractsapi.app.controllers.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.dto.CreateContractRequestDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ContractControllerWriteTest {

    private ContractRepository contractRepository;
    private ClientRepository clientRepository;
    private ContractControllerWrite contractControllerWrite;

    @BeforeEach
    void setUp() {
        contractRepository = Mockito.mock(ContractRepository.class);
        clientRepository = Mockito.mock(ClientRepository.class);
        contractControllerWrite = new ContractControllerWrite(contractRepository, clientRepository);
    }

    @Test
    void createContractWithDefaultsPersistsAndReturnsCreated() {
        CreateContractRequestDto request = new CreateContractRequestDto();
        request.setClientId(42L);
        request.setCostAmount(new BigDecimal("100.50"));

        ClientEntity client = new ClientEntity();
        client.setId(42L);

        when(clientRepository.findById(42L)).thenReturn(Optional.of(client));

        when(contractRepository.save(Mockito.any(ContractEntity.class)))
                .thenAnswer(invocation -> {
                    ContractEntity entity = invocation.getArgument(0);
                    entity.setId(1L);
                    return entity;
                });

        LocalDate today = LocalDate.now();

        ResponseEntity<ContractDto> response = contractControllerWrite.createContract(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(client.getId(), response.getBody().getClientId());
        assertEquals(today, response.getBody().getStartDate());
        assertNull(response.getBody().getEndDate());
        assertEquals(request.getCostAmount(), response.getBody().getCostAmount());
        assertNotNull(response.getBody().getCreatedAt());

        ArgumentCaptor<ContractEntity> captor = ArgumentCaptor.forClass(ContractEntity.class);
        verify(contractRepository).save(captor.capture());
        verify(clientRepository).findById(42L);
        verifyNoMoreInteractions(contractRepository, clientRepository);

        ContractEntity persisted = captor.getValue();
        assertSame(client, persisted.getClient());
        assertEquals(today, persisted.getStartDate());
        assertNull(persisted.getEndDate());
        assertEquals(request.getCostAmount(), persisted.getCostAmount());
        OffsetDateTime createdAt = persisted.getCreatedAt();
        OffsetDateTime updatedAt = persisted.getUpdatedAt();
        assertNotNull(createdAt);
        assertNotNull(updatedAt);
        // Both timestamps should be identical at creation time.
        assertEquals(createdAt, updatedAt);
    }

    @Test
    void createContractReturnsBadRequestWhenEndBeforeStart() {
        CreateContractRequestDto request = new CreateContractRequestDto();
        request.setClientId(12L);
        request.setStartDate(LocalDate.of(2024, 1, 10));
        request.setEndDate(LocalDate.of(2024, 1, 5));
        request.setCostAmount(new BigDecimal("50.00"));

        ClientEntity client = new ClientEntity();
        client.setId(12L);

        when(clientRepository.findById(12L)).thenReturn(Optional.of(client));

        ResponseEntity<ContractDto> response = contractControllerWrite.createContract(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());

        verify(clientRepository).findById(12L);
        verify(contractRepository, never()).save(Mockito.any());
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void createContractReturnsNotFoundWhenClientMissing() {
        CreateContractRequestDto request = new CreateContractRequestDto();
        request.setClientId(999L);
        request.setCostAmount(new BigDecimal("75.00"));

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<ContractDto> response = contractControllerWrite.createContract(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(clientRepository).findById(999L);
        verify(contractRepository, never()).save(Mockito.any());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }
}
