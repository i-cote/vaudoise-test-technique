package com.clientscontractsapi.app.controllers;

import org.springframework.web.bind.annotation.*;

@RestController
public class MainController {

    @GetMapping("/")
    public String root() {
        return "Welcome to Clients Contracts API!";
    }
}
