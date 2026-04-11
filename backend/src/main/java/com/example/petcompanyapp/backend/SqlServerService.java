package com.example.petcompanyapp.backend;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SqlServerService {

    private final String sqlcmdPath;
    private final String sqlServer;
    private final String database;
    private final boolean trustCertificate;
    private final String dbUser;
    private final String dbPassword;

    public SqlServerService(
            String sqlcmdPath,
            String sqlServer,
            String database,
            boolean trustCertificate,
            String dbUser,
            String dbPassword
    ) {
        this.sqlcmdPath = sqlcmdPath;
        this.sqlServer = sqlServer;
        this.database = database;
        this.trustCertificate = trustCertificate;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public String authenticate(String email, String password) throws IOException {
        String query = ""
                + "SET NOCOUNT ON; "
                + "SELECT TOP 1 "
                + "u.UserId AS id, "
                + "ut.Code AS userType, "
                + "u.FullName AS name, "
                + "u.Email AS email, "
                + "u.DocumentNumber AS document, "
                + "CAST(u.IsActive AS bit) AS active "
                + "FROM app.Users u "
                + "INNER JOIN app.UserTypes ut ON ut.UserTypeId = u.UserTypeId "
                + "WHERE LOWER(u.Email) = LOWER(" + StringUtils.sql(email) + ") "
                + "AND u.PasswordHash = " + StringUtils.sql(password) + " "
                + "AND u.IsActive = 1 "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    public String registerUser(String userType, String name, String email, String password, String document) throws IOException {
        String query = ""
                + "SET NOCOUNT ON; "
                + "IF EXISTS (SELECT 1 FROM app.Users WHERE LOWER(Email) = LOWER(" + StringUtils.sql(email) + ")) "
                + "BEGIN SELECT CAST(NULL AS NVARCHAR(MAX)) AS JsonResult; RETURN; END; "
                + "DECLARE @Inserted TABLE (UserId INT); "
                + "INSERT INTO app.Users (UserTypeId, FullName, Email, PasswordHash, DocumentNumber) "
                + "OUTPUT inserted.UserId INTO @Inserted(UserId) "
                + "SELECT ut.UserTypeId, " + StringUtils.sql(name) + ", " + StringUtils.sql(email) + ", "
                + StringUtils.sql(password) + ", " + StringUtils.sql(document) + " "
                + "FROM app.UserTypes ut WHERE ut.Code = " + StringUtils.sql(userType) + "; "
                + "SELECT "
                + "u.UserId AS id, "
                + "ut.Code AS userType, "
                + "u.FullName AS name, "
                + "u.Email AS email, "
                + "u.DocumentNumber AS document, "
                + "CAST(u.IsActive AS bit) AS active "
                + "FROM @Inserted i "
                + "INNER JOIN app.Users u ON u.UserId = i.UserId "
                + "INNER JOIN app.UserTypes ut ON ut.UserTypeId = u.UserTypeId "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    public String findUserById(long userId) throws IOException {
        String query = ""
                + "SET NOCOUNT ON; "
                + "SELECT TOP 1 "
                + "u.UserId AS id, "
                + "ut.Code AS userType, "
                + "u.FullName AS name, "
                + "u.Email AS email, "
                + "u.DocumentNumber AS document, "
                + "CAST(u.IsActive AS bit) AS active "
                + "FROM app.Users u "
                + "INNER JOIN app.UserTypes ut ON ut.UserTypeId = u.UserTypeId "
                + "WHERE u.UserId = " + userId + " "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    public String updateUser(long userId, String name, String email) throws IOException {
        String query = ""
                + "SET NOCOUNT ON; "
                + "IF EXISTS (SELECT 1 FROM app.Users WHERE LOWER(Email) = LOWER(" + StringUtils.sql(email) + ") AND UserId <> " + userId + ") "
                + "BEGIN SELECT CAST(NULL AS NVARCHAR(MAX)) AS JsonResult; RETURN; END; "
                + "UPDATE app.Users "
                + "SET FullName = " + StringUtils.sql(name) + ", Email = " + StringUtils.sql(email) + " "
                + "WHERE UserId = " + userId + "; "
                + "SELECT TOP 1 "
                + "u.UserId AS id, "
                + "ut.Code AS userType, "
                + "u.FullName AS name, "
                + "u.Email AS email, "
                + "u.DocumentNumber AS document, "
                + "CAST(u.IsActive AS bit) AS active "
                + "FROM app.Users u "
                + "INNER JOIN app.UserTypes ut ON ut.UserTypeId = u.UserTypeId "
                + "WHERE u.UserId = " + userId + " "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    public String saveCompany(long ownerUserId, String companyName, String cnpj, String address, String phone) throws IOException {
        String query = ""
                + "SET NOCOUNT ON; "
                + "DECLARE @Inserted TABLE (CompanyId INT); "
                + "INSERT INTO app.Companies (OwnerUserId, CompanyName, Cnpj, AddressLine, PhoneNumber) "
                + "OUTPUT inserted.CompanyId INTO @Inserted(CompanyId) "
                + "VALUES (" + ownerUserId + ", " + StringUtils.sql(companyName) + ", " + StringUtils.sql(cnpj) + ", "
                + StringUtils.sql(address) + ", " + StringUtils.sql(phone) + "); "
                + "SELECT TOP 1 "
                + "c.CompanyId AS id, "
                + "c.OwnerUserId AS ownerUserId, "
                + "c.CompanyName AS companyName, "
                + "c.Cnpj AS cnpj, "
                + "c.AddressLine AS address, "
                + "c.PhoneNumber AS phone "
                + "FROM @Inserted i "
                + "INNER JOIN app.Companies c ON c.CompanyId = i.CompanyId "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    public String getAnimalPosts(String filter) throws IOException {
        String whereClause = "ALL".equalsIgnoreCase(filter)
                ? ""
                : "WHERE pt.Code = " + StringUtils.sql(filter) + " ";

        String query = ""
                + "SET NOCOUNT ON; "
                + "SELECT "
                + "ap.AnimalPostId AS id, "
                + "ap.AuthorUserId AS authorUserId, "
                + "pt.Code AS postType, "
                + "ap.AnimalName AS animalName, "
                + "ap.Species AS species, "
                + "ap.Breed AS breed, "
                + "ap.AgeDescription AS age, "
                + "ap.DescriptionText AS description, "
                + "ap.ContactPhone AS contactPhone, "
                + "ap.Latitude AS latitude, "
                + "ap.Longitude AS longitude, "
                + "ap.LocationReference AS locationReference, "
                + "ap.ImageUrl AS imageUri, "
                + "u.FullName AS authorName, "
                + "DATEDIFF_BIG(MILLISECOND, '1970-01-01T00:00:00', ap.CreatedAt) AS createdAtMillis, "
                + "CAST(0 AS bit) AS liked, "
                + "CAST(0 AS int) AS likeCount "
                + "FROM app.AnimalPosts ap "
                + "INNER JOIN app.PostTypes pt ON pt.PostTypeId = ap.PostTypeId "
                + "INNER JOIN app.Users u ON u.UserId = ap.AuthorUserId "
                + whereClause
                + "ORDER BY ap.CreatedAt DESC "
                + "FOR JSON PATH;";
        String json = runJsonQuery(query);
        return json == null ? "[]" : json;
    }

    public String createAnimalPost(Map<String, String> values) throws IOException {
        String authorUserId = values.get("authorUserId");
        String postType = values.get("postType");
        String animalName = values.get("animalName");
        String species = values.get("species");
        String breed = values.get("breed");
        String age = values.get("age");
        String description = values.get("description");
        String contactPhone = values.get("contactPhone");
        String imageUrl = values.get("imageUri");
        String locationReference = values.get("locationReference");
        String latitude = values.get("latitude");
        String longitude = values.get("longitude");

        if (StringUtils.isBlank(authorUserId)
                || StringUtils.isBlank(postType)
                || StringUtils.isBlank(animalName)
                || StringUtils.isBlank(species)
                || StringUtils.isBlank(breed)
                || StringUtils.isBlank(age)
                || StringUtils.isBlank(description)
                || StringUtils.isBlank(contactPhone)
                || StringUtils.isBlank(imageUrl)) {
            return null;
        }

        if ("LOST".equalsIgnoreCase(postType)
                && (StringUtils.isBlank(latitude) || StringUtils.isBlank(longitude) || StringUtils.isBlank(locationReference))) {
            return null;
        }

        String query = ""
                + "SET NOCOUNT ON; "
                + "DECLARE @Inserted TABLE (AnimalPostId INT); "
                + "INSERT INTO app.AnimalPosts "
                + "(PostTypeId, AuthorUserId, AnimalName, Species, Breed, AgeDescription, DescriptionText, ContactPhone, ImageUrl, LocationReference, Latitude, Longitude) "
                + "OUTPUT inserted.AnimalPostId INTO @Inserted(AnimalPostId) "
                + "SELECT pt.PostTypeId, " + StringUtils.sqlNumber(authorUserId) + ", "
                + StringUtils.sql(animalName) + ", " + StringUtils.sql(species) + ", " + StringUtils.sql(breed) + ", "
                + StringUtils.sql(age) + ", " + StringUtils.sql(description) + ", " + StringUtils.sql(contactPhone) + ", "
                + StringUtils.sql(imageUrl) + ", " + StringUtils.sql(locationReference) + ", "
                + StringUtils.sqlNumber(latitude) + ", " + StringUtils.sqlNumber(longitude) + " "
                + "FROM app.PostTypes pt WHERE pt.Code = " + StringUtils.sql(postType) + "; "
                + "SELECT TOP 1 "
                + "ap.AnimalPostId AS id, "
                + "ap.AuthorUserId AS authorUserId, "
                + "pt.Code AS postType, "
                + "ap.AnimalName AS animalName, "
                + "ap.Species AS species, "
                + "ap.Breed AS breed, "
                + "ap.AgeDescription AS age, "
                + "ap.DescriptionText AS description, "
                + "ap.ContactPhone AS contactPhone, "
                + "ap.Latitude AS latitude, "
                + "ap.Longitude AS longitude, "
                + "ap.LocationReference AS locationReference, "
                + "ap.ImageUrl AS imageUri, "
                + "u.FullName AS authorName, "
                + "DATEDIFF_BIG(MILLISECOND, '1970-01-01T00:00:00', ap.CreatedAt) AS createdAtMillis, "
                + "CAST(0 AS bit) AS liked, "
                + "CAST(0 AS int) AS likeCount "
                + "FROM @Inserted i "
                + "INNER JOIN app.AnimalPosts ap ON ap.AnimalPostId = i.AnimalPostId "
                + "INNER JOIN app.PostTypes pt ON pt.PostTypeId = ap.PostTypeId "
                + "INNER JOIN app.Users u ON u.UserId = ap.AuthorUserId "
                + "FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;";
        return runJsonQuery(query);
    }

    private String runJsonQuery(String query) throws IOException {
        String output = execute(query).trim();
        if (output.isEmpty() || "null".equalsIgnoreCase(output)) {
            return null;
        }

        if (output.startsWith("JsonResult")) {
            String[] lines = output.split("\\R");
            output = lines.length > 1 ? lines[1].trim() : "";
            if (output.isEmpty() || "NULL".equalsIgnoreCase(output)) {
                return null;
            }
        }

        return output;
    }

    private String execute(String query) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(sqlcmdPath);
        command.add("-S");
        command.add(sqlServer);
        command.add("-d");
        command.add(database);
        if (!StringUtils.isBlank(dbUser) && !StringUtils.isBlank(dbPassword)) {
            command.add("-U");
            command.add(dbUser);
            command.add("-P");
            command.add(dbPassword);
        }
        if (trustCertificate) {
            command.add("-C");
        }
        command.add("-Q");
        command.add(query);
        command.add("-h");
        command.add("-1");
        command.add("-W");
        command.add("-w");
        command.add("65535");

        Process process = new ProcessBuilder(command).redirectErrorStream(false).start();
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("sqlcmd failed: " + stderr);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("sqlcmd execution interrupted.", exception);
        }

        return stdout;
    }

    private static String readStream(InputStream inputStream) throws IOException {
        try (InputStream stream = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            stream.transferTo(output);
            return output.toString(StandardCharsets.UTF_8);
        }
    }
}
