package com.example.petcompanyapp.models;

public class User {
    private final Long id;
    private final String userType;
    private final String name;
    private final String email;
    private final String password;
    private final String document;
    private final boolean active;

    public User(String name, String email) {
        this(null, null, name, email, null, null, true);
    }

    public User(
            Long id,
            String userType,
            String name,
            String email,
            String password,
            String document,
            boolean active
    ) {
        this.id = id;
        this.userType = userType;
        this.name = name;
        this.email = email;
        this.password = password;
        this.document = document;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUserType() {
        return userType;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getDocument() {
        return document;
    }

    public boolean isActive() {
        return active;
    }
}
