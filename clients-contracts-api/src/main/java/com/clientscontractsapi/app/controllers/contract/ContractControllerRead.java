package com.clientscontractsapi.app.controllers.contract;

import com.clientscontractsapi.app.docs.examples.ProblemExamples;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.docs.examples.ContractExamples;
import com.clientscontractsapi.app.models.contract.dto.ActiveContractsCostResponseDto;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractControllerRead {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;

    public ContractControllerRead(ContractRepository contractRepository, ClientRepository clientRepository) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
    }

    @GetMapping("/clients/{clientId}/active-cost")
    @Operation(
        summary = "Get active contracts total cost",
        description = "Returns the sum of cost amounts for the client's contracts that are currently active.",
        parameters = {
            @Parameter(
                name = "clientId",
                description = "Identifier of the client whose active contracts are queried",
                example = "1"
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Cost sum successfully calculated",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ActiveContractsCostResponseDto.class),
                    examples = {
                        @ExampleObject(
                            name = "ActiveContractsCostResponse",
                            value = ContractExamples.ACTIVE_CONTRACTS_COST_RESPONSE
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Client not found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(
                            name = "ClientNotFound",
                            value = ProblemExamples.CLIENT_NOT_FOUND
                        )
                    }
                )
            )
        }
    )
    public ResponseEntity<ActiveContractsCostResponseDto> getActiveContractsCost(@PathVariable Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client with id %d was not found.".formatted(clientId));
        }

        BigDecimal sum =
                contractRepository.sumActiveCostAmountByClient(clientId, LocalDate.now());
        BigDecimal value = sum != null ? sum : BigDecimal.ZERO;
        return ResponseEntity.ok(new ActiveContractsCostResponseDto(clientId, value));
    }

    @GetMapping("/clients/{clientId}/contracts")
    @Operation(
        summary = "List active contracts",
        description = "Returns the active contracts for the client and can be filtered by last update timestamp.",
        parameters = {
            @Parameter(
                name = "clientId",
                description = "Identifier of the client whose contracts are listed",
                example = "1"
            ),
            @Parameter(
                name = "updatedSince",
                description = "When provided, returns contracts updated since this timestamp",
                example = "2024-07-01T00:00:00Z"
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Contracts successfully retrieved",
                content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ContractDto.class)),
                    examples = {
                        @ExampleObject(
                            name = "ActiveContractsResponse",
                            value = ContractExamples.ACTIVE_CONTRACTS_RESPONSE
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Client not found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(
                            name = "ClientNotFound",
                            value = ProblemExamples.CLIENT_NOT_FOUND
                        )
                    }
                )
            )
        }
    )
    public ResponseEntity<List<ContractDto>> getActiveContractsForClient(
            @PathVariable Long clientId,
            @RequestParam(value = "updatedSince", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime updatedSince) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client with id %d was not found.".formatted(clientId));
        }

        LocalDate today = LocalDate.now();
        List<ContractEntity> contracts =
                updatedSince == null
                        ? contractRepository.findActiveContractsByClient(clientId, today)
                        : contractRepository.findActiveContractsByClientAndUpdatedSince(
                                clientId, today, updatedSince);

        List<ContractDto> dtos = contracts.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ContractDto toDto(ContractEntity entity) {
        return new ContractDto(
                entity.getId(),
                entity.getClient().getId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCostAmount(),
                entity.getCreatedAt());
    }
}
