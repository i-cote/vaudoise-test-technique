package com.clientscontractsapi.app.docs.examples;

public final class ContractExamples {

    private ContractExamples() {
        // Utility class
    }

    public static final String CREATE_CONTRACT_REQUEST = "{\n"
            + "  \"clientId\": 1,\n"
            + "  \"startDate\": \"2024-08-01\",\n"
            + "  \"endDate\": \"2026-08-01\",\n"
            + "  \"costAmount\": 1200.50\n"
            + "}";

    public static final String CONTRACT_RESPONSE = "{\n"
            + "  \"id\": 1,\n"
            + "  \"clientId\": 1,\n"
            + "  \"startDate\": \"2024-08-01\",\n"
            + "  \"endDate\": \"2026-08-01\",\n"
            + "  \"costAmount\": 1200.50,\n"
            + "  \"createdAt\": \"2024-07-15T10:15:30Z\"\n"
            + "}";

    public static final String UPDATE_CONTRACT_REQUEST = "{\n"
            + "  \"contractId\": 1,\n"
            + "  \"costAmount\": 1350.00\n"
            + "}";

    public static final String UPDATED_CONTRACT_RESPONSE = "{\n"
            + "  \"id\": 1,\n"
            + "  \"clientId\": 1,\n"
            + "  \"startDate\": \"2024-08-01\",\n"
            + "  \"endDate\": \"2026-08-01\",\n"
            + "  \"costAmount\": 1350.00,\n"
            + "  \"createdAt\": \"2024-07-15T10:15:30Z\"\n"
            + "}";

    public static final String ACTIVE_CONTRACTS_COST_RESPONSE = "{\n"
            + "  \"clientId\": 1,\n"
            + "  \"activeCostAmount\": 3250.75\n"
            + "}";

    public static final String ACTIVE_CONTRACTS_RESPONSE = "[\n"
            + "  {\n"
            + "    \"id\": 1,\n"
            + "    \"clientId\": 1,\n"
            + "    \"startDate\": \"2024-08-01\",\n"
            + "    \"endDate\": \"2026-08-01\",\n"
            + "    \"costAmount\": 1350.00,\n"
            + "    \"createdAt\": \"2024-07-15T10:15:30Z\"\n"
            + "  }\n"
            + "]";
}
