package com.example.petcompanyapp.utils;

public final class UserType {

    public static final String PERSON = "PERSON";
    public static final String COMPANY = "COMPANY";

    private UserType() {
        // Constantes de tipo de usuario.
    }

    public static boolean isCompany(String userType) {
        return COMPANY.equals(userType);
    }
}
