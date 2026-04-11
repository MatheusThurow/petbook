package com.example.petcompanyapp.backend;

public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String sql(String value) {
        if (value == null) {
            return "NULL";
        }
        String sanitized = value
                .replace("'", "''")
                .replace("\r", " ")
                .replace("\n", " ")
                .trim();
        return "'" + sanitized + "'";
    }

    public static String sqlNumber(String value) {
        if (isBlank(value)) {
            return "NULL";
        }
        return value.trim();
    }
}
