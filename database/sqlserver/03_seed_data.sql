USE PetCompanyDB;
GO

INSERT INTO app.UserTypes (Code, Name)
SELECT 'PERSON', 'Pessoa fisica'
WHERE NOT EXISTS (SELECT 1 FROM app.UserTypes WHERE Code = 'PERSON');

INSERT INTO app.UserTypes (Code, Name)
SELECT 'COMPANY', 'Empresa'
WHERE NOT EXISTS (SELECT 1 FROM app.UserTypes WHERE Code = 'COMPANY');
GO

INSERT INTO app.PostTypes (Code, Name, RequiresLocation)
SELECT 'LOST', 'Animal perdido', 1
WHERE NOT EXISTS (SELECT 1 FROM app.PostTypes WHERE Code = 'LOST');

INSERT INTO app.PostTypes (Code, Name, RequiresLocation)
SELECT 'ADOPTION', 'Animal para adocao', 0
WHERE NOT EXISTS (SELECT 1 FROM app.PostTypes WHERE Code = 'ADOPTION');
GO

DECLARE @PersonTypeId INT = (SELECT UserTypeId FROM app.UserTypes WHERE Code = 'PERSON');
DECLARE @CompanyTypeId INT = (SELECT UserTypeId FROM app.UserTypes WHERE Code = 'COMPANY');

IF NOT EXISTS (SELECT 1 FROM app.Users WHERE Email = 'ana@petcompany.com')
BEGIN
    INSERT INTO app.Users (UserTypeId, FullName, Email, PasswordHash, DocumentNumber)
    VALUES (@PersonTypeId, 'Ana Souza', 'ana@petcompany.com', '123456', '12345678901');
END;

IF NOT EXISTS (SELECT 1 FROM app.Users WHERE Email = 'contato@clinicafeliz.com')
BEGIN
    INSERT INTO app.Users (UserTypeId, FullName, Email, PasswordHash, DocumentNumber)
    VALUES (@CompanyTypeId, 'Clinica Feliz', 'contato@clinicafeliz.com', '123456', '12345678000199');
END;

DECLARE @CompanyOwnerId INT = (
    SELECT TOP 1 UserId
    FROM app.Users
    WHERE Email = 'contato@clinicafeliz.com'
);

IF NOT EXISTS (SELECT 1 FROM app.Companies WHERE Cnpj = '12.345.678/0001-99')
BEGIN
    INSERT INTO app.Companies (OwnerUserId, CompanyName, Cnpj, AddressLine, PhoneNumber)
    VALUES (@CompanyOwnerId, 'Clinica Feliz', '12.345.678/0001-99', 'Rua das Flores, 150', '(11) 99999-1234');
END;

DECLARE @CompanyId INT = (
    SELECT TOP 1 CompanyId
    FROM app.Companies
    WHERE Cnpj = '12.345.678/0001-99'
);

DECLARE @PersonUserId INT = (
    SELECT TOP 1 UserId
    FROM app.Users
    WHERE Email = 'ana@petcompany.com'
);

IF NOT EXISTS (SELECT 1 FROM app.Animals WHERE AnimalName = 'Thor')
BEGIN
    INSERT INTO app.Animals (OwnerUserId, CompanyId, AnimalName, Species, Breed, AgeYears, WeightKg)
    VALUES (@PersonUserId, NULL, 'Thor', 'Cachorro', 'Labrador', 4, 28.50);
END;

IF NOT EXISTS (SELECT 1 FROM app.Animals WHERE AnimalName = 'Luna')
BEGIN
    INSERT INTO app.Animals (OwnerUserId, CompanyId, AnimalName, Species, Breed, AgeYears, WeightKg)
    VALUES (@CompanyOwnerId, @CompanyId, 'Luna', 'Gato', 'Siames', 2, 4.30);
END;
GO

DECLARE @PersonFeedTypeId INT = (SELECT UserTypeId FROM app.UserTypes WHERE Code = 'PERSON');
DECLARE @CompanyFeedTypeId INT = (SELECT UserTypeId FROM app.UserTypes WHERE Code = 'COMPANY');
DECLARE @LostPostTypeId INT = (SELECT PostTypeId FROM app.PostTypes WHERE Code = 'LOST');
DECLARE @AdoptionPostTypeId INT = (SELECT PostTypeId FROM app.PostTypes WHERE Code = 'ADOPTION');
DECLARE @ThorAnimalId INT = (SELECT TOP 1 AnimalId FROM app.Animals WHERE AnimalName = 'Thor');
DECLARE @LunaAnimalId INT = (SELECT TOP 1 AnimalId FROM app.Animals WHERE AnimalName = 'Luna');

IF NOT EXISTS (SELECT 1 FROM app.FeedPosts WHERE Title = 'Campanha de vacinacao')
BEGIN
    INSERT INTO app.FeedPosts (TargetUserTypeId, Title, Body, DisplayOrder)
    VALUES (@PersonFeedTypeId, 'Campanha de vacinacao', 'Confira os postos parceiros e mantenha a carteira de vacinacao em dia.', 1);
END;

IF NOT EXISTS (SELECT 1 FROM app.FeedPosts WHERE Title = 'Atualize o cadastro empresarial')
BEGIN
    INSERT INTO app.FeedPosts (TargetUserTypeId, Title, Body, DisplayOrder)
    VALUES (@CompanyFeedTypeId, 'Atualize o cadastro empresarial', 'Revise endereco, telefone e dados da empresa para manter o perfil completo.', 1);
END;

IF NOT EXISTS (SELECT 1 FROM app.FeedPosts WHERE Title = 'Cadastro de animais')
BEGIN
    INSERT INTO app.FeedPosts (TargetUserTypeId, Title, Body, DisplayOrder)
    VALUES (NULL, 'Cadastro de animais', 'O cadastro de animais fica disponivel para pessoa fisica e empresa.', 2);
END;

IF NOT EXISTS (SELECT 1 FROM app.AnimalPosts WHERE AnimalName = 'Thor' AND PostTypeId = @LostPostTypeId)
BEGIN
    INSERT INTO app.AnimalPosts (
        PostTypeId,
        AuthorUserId,
        AnimalId,
        AnimalName,
        Species,
        Breed,
        AgeDescription,
        DescriptionText,
        ContactPhone,
        LocationReference,
        Latitude,
        Longitude
    )
    VALUES (
        @LostPostTypeId,
        @PersonUserId,
        @ThorAnimalId,
        'Thor',
        'Cachorro',
        'Labrador',
        '4 anos',
        'Animal perdido na regiao central, usa coleira azul e atende pelo nome Thor.',
        '(11) 98888-0001',
        'Ultima vez visto proximo a praca central.',
        -23.550520,
        -46.633308
    );
END;

IF NOT EXISTS (SELECT 1 FROM app.AnimalPosts WHERE AnimalName = 'Luna' AND PostTypeId = @AdoptionPostTypeId)
BEGIN
    INSERT INTO app.AnimalPosts (
        PostTypeId,
        AuthorUserId,
        AnimalId,
        AnimalName,
        Species,
        Breed,
        AgeDescription,
        DescriptionText,
        ContactPhone,
        LocationReference,
        Latitude,
        Longitude
    )
    VALUES (
        @AdoptionPostTypeId,
        @CompanyOwnerId,
        @LunaAnimalId,
        'Luna',
        'Gato',
        'Siames',
        '2 anos',
        'Gata docil, castrada e pronta para um novo lar responsavel.',
        '(11) 99999-1234',
        NULL,
        NULL,
        NULL
    );
END;
GO
