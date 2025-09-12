package com.fintech.authservice.model;

public record AuthCredDB(String passwordHash, String salt) {
}
