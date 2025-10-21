package com.clientscontractsapi.app.docs.examples;

public final class ProblemExamples {

    private ProblemExamples() {
        // Utility class
    }

    public static final String PERSON_MISSING_BIRTHDATE = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Bad Request\",\n"
            + "  \"status\": 400,\n"
            + "  \"detail\": \"Persons must include a birthdate.\"\n"
            + "}";

    public static final String COMPANY_WITH_BIRTHDATE = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Bad Request\",\n"
            + "  \"status\": 400,\n"
            + "  \"detail\": \"Companies must not include a birthdate.\"\n"
            + "}";

    public static final String CLIENT_EMAIL_EXISTS = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Bad Request\",\n"
            + "  \"status\": 400,\n"
            + "  \"detail\": \"Client with email existing@example.com already exists.\"\n"
            + "}";

    public static final String CLIENT_NOT_FOUND = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Not Found\",\n"
            + "  \"status\": 404,\n"
            + "  \"detail\": \"Client with id 1 was not found.\"\n"
            + "}";

    public static final String CONTRACT_NOT_FOUND = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Not Found\",\n"
            + "  \"status\": 404,\n"
            + "  \"detail\": \"Contract with id 1 was not found.\"\n"
            + "}";

    public static final String CONTRACT_INVALID_DATES = "{\n"
            + "  \"type\": \"about:blank\",\n"
            + "  \"title\": \"Bad Request\",\n"
            + "  \"status\": 400,\n"
            + "  \"detail\": \"End date must be after the start date.\"\n"
            + "}";
}
