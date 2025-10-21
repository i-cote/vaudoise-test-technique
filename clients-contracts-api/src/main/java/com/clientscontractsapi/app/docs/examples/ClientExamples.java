package com.clientscontractsapi.app.docs.examples;

public final class ClientExamples {

    private ClientExamples() {
        // Utility class
    }

    public static final String PERSON_CLIENT_REQUEST = "{\n"
            + "  \"phone\": \"+15551234567\",\n"
            + "  \"email\": \"jane.doe@example.com\",\n"
            + "  \"name\": \"Jane Doe\",\n"
            + "  \"birthdate\": \"1990-05-14\"\n"
            + "}";

    public static final String PERSON_CLIENT_RESPONSE = "{\n"
            + "  \"id\": 1,\n"
            + "  \"clientType\": \"PERSON\",\n"
            + "  \"email\": \"jane.doe@example.com\",\n"
            + "  \"phone\": \"+15551234567\",\n"
            + "  \"name\": \"Jane Doe\",\n"
            + "  \"birthdate\": \"1990-05-14\",\n"
            + "  \"companyIdentifier\": null,\n"
            + "  \"createdAt\": \"2024-07-15T10:15:30Z\",\n"
            + "  \"updatedAt\": \"2024-07-15T10:15:30Z\"\n"
            + "}";

    public static final String UPDATE_CLIENT_REQUEST = "{\n"
            + "  \"id\": 1,\n"
            + "  \"email\": \"jane.smith@example.com\",\n"
            + "  \"phone\": \"+15557654321\",\n"
            + "  \"name\": \"Jane Smith\"\n"
            + "}";

    public static final String UPDATE_CLIENT_RESPONSE = "{\n"
            + "  \"id\": 1,\n"
            + "  \"clientType\": \"PERSON\",\n"
            + "  \"email\": \"jane.smith@example.com\",\n"
            + "  \"phone\": \"+15557654321\",\n"
            + "  \"name\": \"Jane Smith\",\n"
            + "  \"birthdate\": \"1990-05-14\",\n"
            + "  \"companyIdentifier\": null,\n"
            + "  \"createdAt\": \"2024-07-15T10:15:30Z\",\n"
            + "  \"updatedAt\": \"2024-08-01T09:00:00Z\"\n"
            + "}";
}
