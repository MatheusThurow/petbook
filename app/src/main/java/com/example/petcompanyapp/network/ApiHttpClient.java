package com.example.petcompanyapp.network;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiHttpClient {

    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private ApiHttpClient() {
    }

    public static String get(Context context, String path) throws ApiException {
        return request(context, "GET", path, null);
    }

    public static String post(Context context, String path, JSONObject body) throws ApiException {
        return request(context, "POST", path, body);
    }

    public static String put(Context context, String path, JSONObject body) throws ApiException {
        return request(context, "PUT", path, body);
    }

    public static String request(Context context, String method, String path, JSONObject body) throws ApiException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(ApiConfig.getBaseUrl(context) + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            if (body != null) {
                connection.setDoOutput(true);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }
            }

            int statusCode = connection.getResponseCode();
            String response = readResponse(connection, statusCode < 400);

            if (statusCode >= 200 && statusCode < 300) {
                return response;
            }

            throw new ApiException(extractErrorMessage(response, statusCode));
        } catch (IOException exception) {
            throw new ApiException("Nao foi possivel conectar ao servidor local.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readResponse(HttpURLConnection connection, boolean success) throws IOException {
        if (success) {
            try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream())) {
                return readAll(inputStream);
            }
        }

        if (connection.getErrorStream() == null) {
            return "";
        }

        try (BufferedInputStream errorStream = new BufferedInputStream(connection.getErrorStream())) {
            return readAll(errorStream);
        }
    }

    private static String readAll(BufferedInputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private static String extractErrorMessage(String response, int statusCode) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            String errorMessage = jsonObject.optString("error");
            if (!errorMessage.trim().isEmpty()) {
                return errorMessage;
            }
        } catch (Exception ignored) {
            // Usa fallback simples quando o servidor nao devolve JSON esperado.
        }
        return "Erro do servidor (" + statusCode + ").";
    }
}
