package com.clientscontractsapi.app.controllers.client;

import com.clientscontractsapi.app.docs.examples.ProblemExamples;
import com.clientscontractsapi.app.exceptions.ResourceNotFoundException;
import com.clientscontractsapi.app.models.client.entity.ClientEntity;
import com.clientscontractsapi.app.persistency.client.ClientRepository;
import com.clientscontractsapi.app.docs.examples.ClientExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clients")
public class ClientControllerRead {

    private final ClientRepository clientRepository;

    public ClientControllerRead(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get client by id",
        description = "Retrieves the client details for the requested identifier.",
        parameters = {
            @Parameter(
                name = "id",
                description = "Identifier of the client to retrieve",
                example = "1"
            )
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Client found",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ClientEntity.class),
                    examples = {
                        @ExampleObject(
                            name = "ClientResponse",
                            value = ClientExamples.PERSON_CLIENT_RESPONSE
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
    public ResponseEntity<ClientEntity> getClientById(@PathVariable Long id) {
        ClientEntity client =
                clientRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Client with id %d was not found.".formatted(id)));
        return ResponseEntity.ok(client);
    }
}
