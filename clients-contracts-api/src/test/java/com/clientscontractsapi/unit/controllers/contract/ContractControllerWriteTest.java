package com.clientscontractsapi.unit.controllers.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.controllers.contract.ContractControllerWrite;
import com.clientscontractsapi.app.exceptions.BadRequestException;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.dto.CreateContractRequestDto;
import com.clientscontractsapi.app.models.contract.dto.UpdateCostAmountRequestDto;
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
    void createContractThrowsBadRequestWhenEndBeforeStart() {
        CreateContractRequestDto request = new CreateContractRequestDto();
        request.setClientId(12L);
        request.setStartDate(LocalDate.of(2024, 1, 10));
        request.setEndDate(LocalDate.of(2024, 1, 5));
        request.setCostAmount(new BigDecimal("50.00"));

        ClientEntity client = new ClientEntity();
        client.setId(12L);

        when(clientRepository.findById(12L)).thenReturn(Optional.of(client));

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> contractControllerWrite.createContract(request));

        assertEquals("End date must be on or after the start date.", exception.getMessage());

        verify(clientRepository).findById(12L);
        verify(contractRepository, never()).save(Mockito.any());
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void createContractThrowsNotFoundWhenClientMissing() {
        CreateContractRequestDto request = new CreateContractRequestDto();
        request.setClientId(999L);
        request.setCostAmount(new BigDecimal("75.00"));

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> contractControllerWrite.createContract(request));

        assertEquals("Client with id 999 was not found.", exception.getMessage());

        verify(clientRepository).findById(999L);
        verify(contractRepository, never()).save(Mockito.any());
        verifyNoMoreInteractions(clientRepository, contractRepository);
    }

    @Test
    void updateContractCostReturnsUpdatedDto() {
        UpdateCostAmountRequestDto request = new UpdateCostAmountRequestDto();
        request.setContractId(55L);
        request.setCostAmount(new BigDecimal("999.99"));

        ClientEntity client = new ClientEntity();
        client.setId(9L);

        ContractEntity existing = new ContractEntity();
        existing.setId(55L);
        existing.setClient(client);
        existing.setCostAmount(new BigDecimal("100.00"));
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));

        ContractEntity saved = new ContractEntity();
        saved.setId(55L);
        saved.setClient(client);
        saved.setCostAmount(request.getCostAmount());
        saved.setCreatedAt(existing.getCreatedAt());

        when(contractRepository.findById(55L)).thenReturn(Optional.of(existing));
        when(contractRepository.save(existing)).thenReturn(saved);

        ResponseEntity<ContractDto> response = contractControllerWrite.updateContractCost(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(request.getCostAmount(), response.getBody().getCostAmount());
        assertEquals(saved.getId(), response.getBody().getId());

        verify(contractRepository).findById(55L);
        verify(contractRepository).save(existing);
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void updateContractCostThrowsNotFoundWhenMissing() {
        UpdateCostAmountRequestDto request = new UpdateCostAmountRequestDto();
        request.setContractId(404L);
        request.setCostAmount(new BigDecimal("10.00"));

        when(contractRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> contractControllerWrite.updateContractCost(request));

        assertEquals("Contract with id 404 was not found.", exception.getMessage());

        verify(contractRepository).findById(404L);
        verify(contractRepository, never()).save(Mockito.any());
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }
}
