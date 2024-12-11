package com.zodiac.homehealthdevicedataloggerserver.Data;


public class ClientInfo {
    private final String clientId; // Client's unique ID (e.g., IP Address)
    private final String status;  // Connection status (e.g., "Connected" or "Disconnected")

    public ClientInfo(String clientId, String status) {
        this.clientId = clientId;
        this.status = status;
    }

    public String getClientId() {
        return clientId;
    }

    public String getStatus() {
        return status;
    }
}