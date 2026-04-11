USE PetCompanyDB;
GO

IF OBJECT_ID('app.PostTypes', 'U') IS NULL
BEGIN
    CREATE TABLE app.PostTypes (
        PostTypeId INT IDENTITY(1,1) PRIMARY KEY,
        Code VARCHAR(20) NOT NULL UNIQUE,
        Name VARCHAR(60) NOT NULL,
        RequiresLocation BIT NOT NULL
    );
END;
GO

IF OBJECT_ID('app.AnimalPosts', 'U') IS NULL
BEGIN
    CREATE TABLE app.AnimalPosts (
        AnimalPostId INT IDENTITY(1,1) PRIMARY KEY,
        PostTypeId INT NOT NULL,
        AuthorUserId INT NOT NULL,
        AnimalId INT NULL,
        AnimalName VARCHAR(120) NOT NULL,
        Species VARCHAR(50) NOT NULL,
        Breed VARCHAR(80) NOT NULL,
        AgeDescription VARCHAR(40) NOT NULL,
        DescriptionText VARCHAR(800) NOT NULL,
        ContactPhone VARCHAR(20) NOT NULL,
        ImageUrl VARCHAR(500) NOT NULL,
        LocationReference VARCHAR(250) NULL,
        Latitude DECIMAL(9,6) NULL,
        Longitude DECIMAL(9,6) NULL,
        IsActive BIT NOT NULL CONSTRAINT DF_AnimalPosts_IsActive DEFAULT 1,
        CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_AnimalPosts_CreatedAt DEFAULT SYSDATETIME(),
        CONSTRAINT FK_AnimalPosts_PostTypes FOREIGN KEY (PostTypeId) REFERENCES app.PostTypes(PostTypeId),
        CONSTRAINT FK_AnimalPosts_Users FOREIGN KEY (AuthorUserId) REFERENCES app.Users(UserId),
        CONSTRAINT FK_AnimalPosts_Animals FOREIGN KEY (AnimalId) REFERENCES app.Animals(AnimalId)
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM app.PostTypes WHERE Code = 'LOST')
BEGIN
    INSERT INTO app.PostTypes (Code, Name, RequiresLocation)
    VALUES ('LOST', 'Animal perdido', 1);
END;
GO

IF NOT EXISTS (SELECT 1 FROM app.PostTypes WHERE Code = 'ADOPTION')
BEGIN
    INSERT INTO app.PostTypes (Code, Name, RequiresLocation)
    VALUES ('ADOPTION', 'Animal para adocao', 0);
END;
GO

CREATE OR ALTER TRIGGER app.TR_AnimalPosts_ValidateIntegrity
ON app.AnimalPosts
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        LEFT JOIN app.Users u ON u.UserId = i.AuthorUserId
        WHERE u.UserId IS NULL OR u.IsActive = 0
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 51000, 'Nao e permitido salvar post sem usuario valido e ativo.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        INNER JOIN app.PostTypes pt ON pt.PostTypeId = i.PostTypeId
        WHERE pt.RequiresLocation = 1
          AND (
              i.Latitude IS NULL
              OR i.Longitude IS NULL
              OR NULLIF(LTRIM(RTRIM(i.LocationReference)), '') IS NULL
          )
    )
    BEGIN
        ROLLBACK TRANSACTION;
        THROW 51001, 'Posts de animal perdido exigem latitude, longitude e referencia do local.', 1;
    END;
END;
GO

CREATE OR ALTER VIEW app.vwAnimalPosts
AS
SELECT
    ap.AnimalPostId,
    pt.Code AS PostTypeCode,
    pt.Name AS PostTypeName,
    ap.AnimalName,
    ap.Species,
    ap.Breed,
    ap.AgeDescription,
    ap.DescriptionText,
    ap.ContactPhone,
    ap.ImageUrl,
    ap.LocationReference,
    ap.Latitude,
    ap.Longitude,
    ap.AuthorUserId,
    u.FullName AS AuthorName,
    ap.CreatedAt
FROM app.AnimalPosts ap
INNER JOIN app.PostTypes pt ON pt.PostTypeId = ap.PostTypeId
INNER JOIN app.Users u ON u.UserId = ap.AuthorUserId
WHERE ap.IsActive = 1;
GO

IF NOT EXISTS (SELECT 1 FROM app.AnimalPosts)
BEGIN
    DECLARE @LostPostTypeId INT = (SELECT TOP 1 PostTypeId FROM app.PostTypes WHERE Code = 'LOST');
    DECLARE @AdoptionPostTypeId INT = (SELECT TOP 1 PostTypeId FROM app.PostTypes WHERE Code = 'ADOPTION');
    DECLARE @AnaUserId INT = (SELECT TOP 1 UserId FROM app.Users WHERE Email = 'ana@petcompany.com');
    DECLARE @CompanyUserId INT = (SELECT TOP 1 UserId FROM app.Users WHERE Email = 'contato@clinicafeliz.com');

    IF @AnaUserId IS NOT NULL AND @LostPostTypeId IS NOT NULL
    BEGIN
        INSERT INTO app.AnimalPosts (
            PostTypeId,
            AuthorUserId,
            AnimalName,
            Species,
            Breed,
            AgeDescription,
            DescriptionText,
            ContactPhone,
            ImageUrl,
            LocationReference,
            Latitude,
            Longitude
        )
        VALUES (
            @LostPostTypeId,
            @AnaUserId,
            'Thor',
            'Cachorro',
            'Labrador',
            '4 anos',
            'Animal perdido na regiao central, usa coleira azul e atende pelo nome Thor.',
            '(11) 98888-0001',
            '',
            'Ultima vez visto proximo a praca central.',
            -23.550520,
            -46.633308
        );
    END;

    IF @CompanyUserId IS NOT NULL AND @AdoptionPostTypeId IS NOT NULL
    BEGIN
        INSERT INTO app.AnimalPosts (
            PostTypeId,
            AuthorUserId,
            AnimalName,
            Species,
            Breed,
            AgeDescription,
            DescriptionText,
            ContactPhone,
            ImageUrl,
            LocationReference,
            Latitude,
            Longitude
        )
        VALUES (
            @AdoptionPostTypeId,
            @CompanyUserId,
            'Luna',
            'Gato',
            'Siames',
            '2 anos',
            'Gata docil, castrada e pronta para um novo lar responsavel.',
            '(11) 99999-1234',
            '',
            '',
            NULL,
            NULL
        );
    END;
END;
GO
