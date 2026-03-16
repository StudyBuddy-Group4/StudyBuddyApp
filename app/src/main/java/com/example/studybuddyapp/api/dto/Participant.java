package com.example.studybuddyapp.api.dto;

public class Participant {

    private final Long id;
    private final String username;

    public Participant(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}

