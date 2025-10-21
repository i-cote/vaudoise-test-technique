package com.clientscontractsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
  servers = {
    @Server(url = "https://clients-contracts-api.icote.dev", description = "public kubernetes endpoint"),
    @Server(url = "http://localhost:8080", description = "Local docker compose development")
  }
)
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}