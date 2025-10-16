package com.clientscontractsapi.app.controllers.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.clientscontractsapi.app.models.contract.dto.ActiveContractsCostResponseDto;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
