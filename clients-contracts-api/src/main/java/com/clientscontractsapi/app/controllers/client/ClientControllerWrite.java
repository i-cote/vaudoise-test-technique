package com.clientscontractsapi.app.controllers.client;

import com.clientscontractsapi.app.docs.examples.ProblemExamples;
import com.clientscontractsapi.app.exceptions.BadRequestException;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.docs.examples.ClientExamples;
import com.clientscontractsapi.app.models.client.dto.CreateClientRequestDto;
import com.clientscontractsapi.app.models.client.dto.UpdateClientRequestDto;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.models.contract.entity.ContractEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.persistency.contract.ContractRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientControllerWrite {

    private final ClientRepository clientRepository;
    private final ContractRepository contractRepository;

    public ClientControllerWrite(ClientRepository clientRepository, ContractRepository contractRepository) {
        this.clientRepository = clientRepository;
        this.contractRepository = contractRepository;
    }

    @Operation(
        summary = "Create a new client",
        description = "Creates a client as either a person or a company based on the provided payload.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = CreateClientRequestDto.class),
                examples = {
                    @ExampleObject(
                        name = "PersonClientRequest",
                        description = "Example payload for creating a person client",
                        value = ClientExamples.PERSON_CLIENT_REQUEST
                    )
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Client successfully created",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClientEntity.class),
                    examples = {
                        @ExampleObject(
                            name = "PersonClientResponse",
                            description = "Example response body for a newly created person client",
                            value = ClientExamples.PERSON_CLIENT_RESPONSE
                        )
                    }
                )
            ),
            @ApiResponse(
                responseCode = "400",
                description = "Invalid client data provided",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = {
                        @ExampleObject(
                            name = "PersonMissingBirthdate",
                            value = ProblemExamples.PERSON_MISSING_BIRTHDATE
                        ),
                        @ExampleObject(
                            name = "CompanyIncludesBirthdate",
                            value = ProblemExamples.COMPANY_WITH_BIRTHDATE
                        ),
                        @ExampleObject(
                            name = "EmailAlreadyExists",
                            value = ProblemExamples.CLIENT_EMAIL_EXISTS
                        )
                    }
                )
            )
        }
    )
    @PostMapping("/create-client")
    public ResponseEntity<ClientEntity> createClient(@Valid @RequestBody CreateClientRequestDto request) {
        boolean isCompany = StringUtils.hasText(request.getCompanyIdentifier());

        if (isCompany && request.getBirthdate() != null) {
            throw new BadRequestException("Companies must not include a birthdate.");
        }
        if (!isCompany && request.getBirthdate() == null) {
            throw new BadRequestException("Persons must include a birthdate.");
        }
        if (clientRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BadRequestException(
                    "Client with email %s already exists.".formatted(request.getEmail()));
        }

        ClientEntity client = new ClientEntity();
        client.setClientType(isCompany ? "COMPANY" : "PERSON");
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setName(request.getName());
        client.setBirthdate(isCompany ? null : request.getBirthdate());
        client.setCompanyIdentifier(isCompany ? request.getCompanyIdentifier() : null);

        OffsetDateTime now = OffsetDateTime.now();
        client.setCreatedAt(now);
        client.setUpdatedAt(now);

        ClientEntity saved = clientRepository.save(client);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/update-client")
    @Operation(
        summary = "Update an existing client",
        description = "Updates the contact details for an existing client.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = UpdateClientRequestDto.class),
                examples = {
                    @ExampleObject(
                        name = "UpdateClientRequest",
                        value = ClientExamples.UPDATE_CLIENT_REQUEST
                    )
                }
            )
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Client successfully updated",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClientEntity.class),
                    examples = {
                        @ExampleObject(
                            name = "UpdateClientResponse",
                            value = ClientExamples.UPDATE_CLIENT_RESPONSE
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
    public ResponseEntity<ClientEntity> updateClient(@Valid @RequestBody UpdateClientRequestDto request) {
        ClientEntity client =
                clientRepository
                        .findById(request.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Client with id %d was not found.".formatted(request.getId())));
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setName(request.getName());

        ClientEntity saved = clientRepository.save(client);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/delete-client/{id}")
    @Operation(
        summary = "Delete a client",
        description = "Deletes a client and ends any active contracts by setting their end date to the current day.",
        parameters = {
            @Parameter(
                name = "id",
                description = "Identifier of the client to delete",
                example = "1"
            )
        },
        responses = {
            @ApiResponse(responseCode = "204", description = "Client successfully deleted", content = @Content()),
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
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        ClientEntity client =
                clientRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Client with id %d was not found.".formatted(id)));

        LocalDate today = LocalDate.now();
        List<ContractEntity> contracts = contractRepository.findByClientId(id);
        if (!contracts.isEmpty()) {
            contracts.forEach(contract -> contract.setEndDate(today));
            contractRepository.saveAll(contracts);
        }

        clientRepository.delete(client);
        return ResponseEntity.noContent().build();
    }
}
