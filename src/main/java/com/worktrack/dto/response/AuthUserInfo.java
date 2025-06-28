package com.worktrack.dto.response;

public class AuthUserInfo {
    private String username;

    public AuthUserInfo() { }

    public AuthUserInfo(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}