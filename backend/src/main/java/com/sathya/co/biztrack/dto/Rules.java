package com.sathya.co.biztrack.dto;

public final class Rules {
    public static final String PASSWORD_REGEX = "^(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String PASSWORD_MESSAGE = "Password needs 8+ characters with a number and a symbol";
}