package com.example.petcompanyapp.utils;

import android.text.TextUtils;
import android.util.Patterns;

public final class ValidationUtils {

    private ValidationUtils() {
        // Classe utilitaria: nao deve ser instanciada.
    }

    public static boolean isEmpty(String value) {
        return TextUtils.isEmpty(value) || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean hasMinLength(String value, int minLength) {
        return !isEmpty(value) && value.trim().length() >= minLength;
    }
}
