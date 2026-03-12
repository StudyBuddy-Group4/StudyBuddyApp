package com.example.studybuddyapp.api.dto;

public class DeleteAccountRequest {
    private String password;

    public DeleteAccountRequest(String password) {
        this.password = password;
    }
}
