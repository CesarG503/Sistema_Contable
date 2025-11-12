package com.example.LoginCliente.DTO;

public class MailValuesDTO {
    private String from;
    private String to;
    private String subject;
    private String body;
    private String username;
    private String token;

    public MailValuesDTO(String from, String to, String subject, String body, String username, String token) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.username = username;
        this.token = token;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
