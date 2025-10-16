package com.clientscontractsapi.app.controllers.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.dto.ActiveContractsCostResponseDto;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ContractControllerReadTest {

    private ContractRepository contractRepository;
    private ClientRepository clientRepository;
    private ContractControllerRead contractControllerRead;

    @BeforeEach
    void setUp() {
        contractRepository = Mockito.mock(ContractRepository.class);
        clientRepository = Mockito.mock(ClientRepository.class);
        contractControllerRead = new ContractControllerRead(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsCostReturnsSum() {
        when(clientRepository.existsById(5L)).thenReturn(true);
        when(contractRepository.sumActiveCostAmountByClient(Mockito.eq(5L), Mockito.any(LocalDate.class)))
                .thenReturn(new BigDecimal("2500.75"));

        ResponseEntity<ActiveContractsCostResponseDto> response =
                contractControllerRead.getActiveContractsCost(5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5L, response.getBody().getClientId());
        assertEquals(new BigDecimal("2500.75"), response.getBody().getActiveCostAmount());

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(contractRepository).sumActiveCostAmountByClient(Mockito.eq(5L), dateCaptor.capture());
        assertEquals(LocalDate.now(), dateCaptor.getValue());

        verify(clientRepository).existsById(5L);
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsCostReturnsZeroWhenNull() {
        when(clientRepository.existsById(6L)).thenReturn(true);
        when(contractRepository.sumActiveCostAmountByClient(Mockito.eq(6L), Mockito.any(LocalDate.class)))
                .thenReturn(null);

        ResponseEntity<ActiveContractsCostResponseDto> response =
                contractControllerRead.getActiveContractsCost(6L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(6L, response.getBody().getClientId());
        assertEquals(BigDecimal.ZERO, response.getBody().getActiveCostAmount());

        verify(contractRepository).sumActiveCostAmountByClient(Mockito.eq(6L), Mockito.any(LocalDate.class));
        verify(clientRepository).existsById(6L);
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsCostReturnsNotFoundWhenClientMissing() {
        when(clientRepository.existsById(404L)).thenReturn(false);

        ResponseEntity<ActiveContractsCostResponseDto> response =
                contractControllerRead.getActiveContractsCost(404L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(clientRepository).existsById(404L);
        verify(contractRepository, never())
                .sumActiveCostAmountByClient(Mockito.anyLong(), Mockito.any(LocalDate.class));
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsForClientReturnsDtos() {
        when(clientRepository.existsById(10L)).thenReturn(true);

        ClientEntity client = new ClientEntity();
        client.setId(10L);

        ContractEntity first = new ContractEntity();
        first.setId(1L);
        first.setClient(client);
        first.setStartDate(LocalDate.of(2024, 1, 10));
        first.setEndDate(LocalDate.of(2025, 1, 10));
        first.setCostAmount(new BigDecimal("100.00"));
        first.setCreatedAt(OffsetDateTime.now().minusDays(5));
        first.setUpdatedAt(OffsetDateTime.now().minusDays(2));

        ContractEntity second = new ContractEntity();
        second.setId(2L);
        second.setClient(client);
        second.setStartDate(LocalDate.of(2024, 3, 5));
        second.setEndDate(LocalDate.of(2025, 3, 5));
        second.setCostAmount(new BigDecimal("250.00"));
        second.setCreatedAt(OffsetDateTime.now().minusDays(4));
        second.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        when(contractRepository.findActiveContractsByClient(Mockito.eq(10L), Mockito.any(LocalDate.class)))
                .thenReturn(List.of(first, second));

        ResponseEntity<List<ContractDto>> response =
                contractControllerRead.getActiveContractsForClient(10L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        ContractDto firstDto = response.getBody().get(0);
        assertEquals(first.getId(), firstDto.getId());
        assertEquals(first.getClient().getId(), firstDto.getClientId());
        assertEquals(first.getStartDate(), firstDto.getStartDate());
        assertEquals(first.getEndDate(), firstDto.getEndDate());

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(contractRepository).findActiveContractsByClient(Mockito.eq(10L), dateCaptor.capture());
        assertEquals(LocalDate.now(), dateCaptor.getValue());

        verify(clientRepository).existsById(10L);
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsForClientWithUpdatedSinceFilters() {
        when(clientRepository.existsById(11L)).thenReturn(true);

        ClientEntity client = new ClientEntity();
        client.setId(11L);

        ContractEntity contract = new ContractEntity();
        contract.setId(5L);
        contract.setClient(client);
        contract.setStartDate(LocalDate.of(2024, 6, 1));
        contract.setEndDate(LocalDate.of(2025, 6, 1));
        contract.setCostAmount(new BigDecimal("500.00"));
        contract.setCreatedAt(OffsetDateTime.now().minusDays(3));
        contract.setUpdatedAt(OffsetDateTime.now().minusHours(10));

        OffsetDateTime updatedSince = OffsetDateTime.now().minusDays(1);

        when(contractRepository.findActiveContractsByClientAndUpdatedSince(
                        Mockito.eq(11L), Mockito.any(LocalDate.class), Mockito.eq(updatedSince)))
                .thenReturn(List.of(contract));

        ResponseEntity<List<ContractDto>> response =
                contractControllerRead.getActiveContractsForClient(11L, updatedSince);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(contract.getId(), response.getBody().get(0).getId());

        verify(contractRepository)
                .findActiveContractsByClientAndUpdatedSince(
                        Mockito.eq(11L), Mockito.any(LocalDate.class), Mockito.eq(updatedSince));
        verify(clientRepository).existsById(11L);
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }

    @Test
    void getActiveContractsForClientReturnsNotFoundWhenMissing() {
        when(clientRepository.existsById(88L)).thenReturn(false);

        ResponseEntity<List<ContractDto>> response =
                contractControllerRead.getActiveContractsForClient(88L, null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(clientRepository).existsById(88L);
        verify(contractRepository, never())
                .findActiveContractsByClient(Mockito.anyLong(), Mockito.any(LocalDate.class));
        verify(contractRepository, never())
                .findActiveContractsByClientAndUpdatedSince(
                        Mockito.anyLong(), Mockito.any(LocalDate.class), Mockito.any());
        verifyNoMoreInteractions(contractRepository, clientRepository);
    }
}
