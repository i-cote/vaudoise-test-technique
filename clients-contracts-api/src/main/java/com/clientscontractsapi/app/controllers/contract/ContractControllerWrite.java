package com.clientscontractsapi.app.controllers.contract;

import com.clientscontractsapi.app.docs.examples.ProblemExamples;
import com.clientscontractsapi.app.exceptions.BadRequestException;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.docs.examples.ContractExamples;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.dto.ContractDto;
import com.clientscontractsapi.app.models.contract.dto.CreateContractRequestDto;
import com.clientscontractsapi.app.models.contract.dto.UpdateCostAmountRequestDto;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts")
public class ContractControllerWrite {

    private final ContractRepository contractRepository;
    private final ClientRepository clientRepository;

    public ContractControllerWrite(ContractRepository contractRepository, ClientRepository clientRepository) {
        this.contractRepository = contractRepository;
        this.clientRepository = clientRepository;
    }

    @PostMapping("/create-contract")
    @Operation(
        summary = "Create a contract",
        description = "Creates a contract for an existing client. The start date defaults to today when not provided.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateContractRequestDto.class),
                examples = {
                    @ExampleObject(
                        name = "CreateContractRequest",
                        value = ContractExamples.CREATE_CONTRACT_REQUEST
                    )
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Contract successfully created",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ContractDto.class),
                    examples = {
                        @ExampleObject(
                            name = "CreateContractResponse",
                            value = ContractExamples.CONTRACT_RESPONSE
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid contract data provided",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(
                            name = "EndDateBeforeStartDate",
                            value = ProblemExamples.CONTRACT_INVALID_DATES
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
    public ResponseEntity<ContractDto> createContract(@Valid @RequestBody CreateContractRequestDto request) {
        ClientEntity client =
                clientRepository
                        .findById(request.getClientId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Client with id %d was not found.".formatted(request.getClientId())));

        LocalDate startDate = Optional.ofNullable(request.getStartDate()).orElse(LocalDate.now());
        LocalDate endDate = request.getEndDate();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after the start date.");
        }

        ContractEntity contract = new ContractEntity();
        contract.setClient(client);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setCostAmount(request.getCostAmount());

        OffsetDateTime now = OffsetDateTime.now();
        contract.setCreatedAt(now);
        contract.setUpdatedAt(now);

        ContractEntity saved = contractRepository.save(contract);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PatchMapping("/update-contract")
    @Operation(
        summary = "Update contract cost amount",
        description = "Updates the cost amount of an existing contract.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateCostAmountRequestDto.class),
                examples = {
                    @ExampleObject(
                        name = "UpdateContractRequest",
                        value = ContractExamples.UPDATE_CONTRACT_REQUEST
                    )
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Contract successfully updated",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ContractDto.class),
                    examples = {
                        @ExampleObject(
                            name = "UpdateContractResponse",
                            value = ContractExamples.UPDATED_CONTRACT_RESPONSE
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "404",
                description = "Contract not found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(
                            name = "ContractNotFound",
                            value = ProblemExamples.CONTRACT_NOT_FOUND
                        )
                    }
                )
            )
        }
    )
    public ResponseEntity<ContractDto> updateContractCost(
            @Valid @RequestBody UpdateCostAmountRequestDto request) {
        ContractEntity contract =
                contractRepository
                        .findById(request.getContractId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Contract with id %d was not found.".formatted(request.getContractId())));
        contract.setCostAmount(request.getCostAmount());

        ContractEntity saved = contractRepository.save(contract);
        return ResponseEntity.ok(toDto(saved));
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
