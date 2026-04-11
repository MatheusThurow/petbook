package com.example.petcompanyapp.backend;

import java.util.HashMap;
import java.util.Map;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static Map<String, String> parseFlatJson(String rawJson) {
        Map<String, String> values = new HashMap<>();
        if (rawJson == null) {
            return values;
        }

        String json = rawJson.trim();
        if (json.isEmpty() || "{}".equals(json)) {
            return values;
        }

        if (json.startsWith("{")) {
            json = json.substring(1);
        }
        if (json.endsWith("}")) {
            json = json.substring(0, json.length() - 1);
        }

        StringBuilder current = new StringBuilder();
        boolean insideQuotes = false;
        java.util.List<String> entries = new java.util.ArrayList<>();
        for (int index = 0; index < json.length(); index++) {
            char currentChar = json.charAt(index);
            if (currentChar == '"' && (index == 0 || json.charAt(index - 1) != '\\')) {
                insideQuotes = !insideQuotes;
            }
            if (currentChar == ',' && !insideQuotes) {
                entries.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }
        if (current.length() > 0) {
            entries.add(current.toString());
        }

        for (String entry : entries) {
            int colonIndex = findColonOutsideQuotes(entry);
            if (colonIndex <= 0) {
                continue;
            }

            String key = cleanJsonToken(entry.substring(0, colonIndex));
            String value = cleanJsonToken(entry.substring(colonIndex + 1));
            values.put(key, value);
        }

        return values;
    }

    private static int findColonOutsideQuotes(String value) {
        boolean insideQuotes = false;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == '"' && (index == 0 || value.charAt(index - 1) != '\\')) {
                insideQuotes = !insideQuotes;
            }
            if (current == ':' && !insideQuotes) {
                return index;
            }
        }
        return -1;
    }

    private static String cleanJsonToken(String token) {
        String cleaned = token == null ? "" : token.trim();
        if (cleaned.startsWith("\"")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        cleaned = cleaned.replace("\\\"", "\"");
        cleaned = cleaned.replace("\\n", "\n");
        cleaned = cleaned.replace("\\r", "\r");
        return "null".equalsIgnoreCase(cleaned) ? null : cleaned;
    }
}
