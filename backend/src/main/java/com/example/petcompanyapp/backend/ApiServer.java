package com.example.petcompanyapp.backend;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public final class ApiServer {

    private final HttpServer server;
    private final SqlServerService sqlServerService;

    public ApiServer(int port, SqlServerService sqlServerService) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.sqlServerService = sqlServerService;
        this.server.setExecutor(Executors.newCachedThreadPool());
        registerRoutes();
    }

    public void start() {
        server.start();
        System.out.println("PetCompany API running on http://0.0.0.0:" + server.getAddress().getPort());
    }

    private void registerRoutes() {
        server.createContext("/api/health", exchange -> {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            writeJson(exchange, 200, "{\"status\":\"ok\"}");
        });

        server.createContext("/api/auth/login", jsonHandler("POST", exchange -> {
            Map<String, String> body = JsonUtils.parseFlatJson(readBody(exchange));
            String email = body.get("email");
            String password = body.get("password");

            if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
                writeJson(exchange, 400, "{\"error\":\"Email and password are required.\"}");
                return;
            }

            String userJson = sqlServerService.authenticate(email, password);
            if (userJson == null) {
                writeJson(exchange, 401, "{\"error\":\"Invalid credentials.\"}");
                return;
            }

            writeJson(exchange, 200, userJson);
        }));

        server.createContext("/api/users/register", jsonHandler("POST", exchange -> {
            Map<String, String> body = JsonUtils.parseFlatJson(readBody(exchange));
            String userType = body.get("userType");
            String name = body.get("name");
            String email = body.get("email");
            String password = body.get("password");
            String document = body.get("document");

            if (StringUtils.isBlank(userType)
                    || StringUtils.isBlank(name)
                    || StringUtils.isBlank(email)
                    || StringUtils.isBlank(password)
                    || StringUtils.isBlank(document)) {
                writeJson(exchange, 400, "{\"error\":\"Required fields are missing.\"}");
                return;
            }

            String createdUserJson = sqlServerService.registerUser(userType, name, email, password, document);
            if (createdUserJson == null) {
                writeJson(exchange, 409, "{\"error\":\"Unable to create user. Email may already exist.\"}");
                return;
            }

            writeJson(exchange, 201, createdUserJson);
        }));

        server.createContext("/api/users", jsonHandler("*", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] segments = path.split("/");

            if (segments.length < 4) {
                writeJson(exchange, 404, "{\"error\":\"User endpoint not found.\"}");
                return;
            }

            long userId;
            try {
                userId = Long.parseLong(segments[3]);
            } catch (NumberFormatException exception) {
                writeJson(exchange, 400, "{\"error\":\"Invalid user id.\"}");
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                String userJson = sqlServerService.findUserById(userId);
                if (userJson == null) {
                    writeJson(exchange, 404, "{\"error\":\"User not found.\"}");
                    return;
                }
                writeJson(exchange, 200, userJson);
                return;
            }

            if ("PUT".equalsIgnoreCase(method)) {
                Map<String, String> body = JsonUtils.parseFlatJson(readBody(exchange));
                String name = body.get("name");
                String email = body.get("email");
                if (StringUtils.isBlank(name) || StringUtils.isBlank(email)) {
                    writeJson(exchange, 400, "{\"error\":\"Name and email are required.\"}");
                    return;
                }

                String updatedJson = sqlServerService.updateUser(userId, name, email);
                if (updatedJson == null) {
                    writeJson(exchange, 404, "{\"error\":\"User not found or email already in use.\"}");
                    return;
                }
                writeJson(exchange, 200, updatedJson);
                return;
            }

            writeJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        }));

        server.createContext("/api/companies", jsonHandler("POST", exchange -> {
            Map<String, String> body = JsonUtils.parseFlatJson(readBody(exchange));
            String ownerUserId = body.get("ownerUserId");
            String companyName = body.get("companyName");
            String cnpj = body.get("cnpj");
            String address = body.get("address");
            String phone = body.get("phone");

            if (StringUtils.isBlank(ownerUserId)
                    || StringUtils.isBlank(companyName)
                    || StringUtils.isBlank(cnpj)
                    || StringUtils.isBlank(address)
                    || StringUtils.isBlank(phone)) {
                writeJson(exchange, 400, "{\"error\":\"Required company fields are missing.\"}");
                return;
            }

            String companyJson = sqlServerService.saveCompany(
                    Long.parseLong(ownerUserId),
                    companyName,
                    cnpj,
                    address,
                    phone
            );

            if (companyJson == null) {
                writeJson(exchange, 409, "{\"error\":\"Unable to save company.\"}");
                return;
            }

            writeJson(exchange, 201, companyJson);
        }));

        server.createContext("/api/posts", jsonHandler("*", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String filter = "ALL";
                if (query != null && query.startsWith("filter=")) {
                    filter = query.substring("filter=".length()).toUpperCase();
                }
                writeJson(exchange, 200, sqlServerService.getAnimalPosts(filter));
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> body = JsonUtils.parseFlatJson(readBody(exchange));
                String createdPostJson = sqlServerService.createAnimalPost(body);
                if (createdPostJson == null) {
                    writeJson(exchange, 400, "{\"error\":\"Unable to create post. Check author and required fields.\"}");
                    return;
                }
                writeJson(exchange, 201, createdPostJson);
                return;
            }

            writeJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        }));
    }

    private HttpHandler jsonHandler(String expectedMethod, HttpHandler next) {
        return exchange -> {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            headers.add("Access-Control-Allow-Methods", "GET,POST,PUT,OPTIONS");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            if (!"*".equals(expectedMethod) && !expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
                writeJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
                return;
            }

            next.handle(exchange);
        };
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, payload.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(payload);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getProperty("api.port", "8080"));
        String sqlcmdPath = System.getProperty(
                "sqlcmd.path",
                "C:\\Program Files\\Microsoft SQL Server\\Client SDK\\ODBC\\180\\Tools\\Binn\\SQLCMD.EXE"
        );
        String sqlServer = System.getProperty("db.server", ".\\SQLEXPRESS");
        String database = System.getProperty("db.name", "PetCompanyDB");
        boolean trustCertificate = Boolean.parseBoolean(System.getProperty("db.trustCertificate", "true"));
        String dbUser = System.getProperty("db.user", "");
        String dbPassword = System.getProperty("db.password", "");

        SqlServerService service = new SqlServerService(
                sqlcmdPath,
                sqlServer,
                database,
                trustCertificate,
                dbUser,
                dbPassword
        );
        new ApiServer(port, service).start();
    }
}
