USE PetCompanyDB;
GO

IF OBJECT_ID('app.vwActiveFeed', 'V') IS NOT NULL DROP VIEW app.vwActiveFeed;
IF OBJECT_ID('app.vwAnimalSummary', 'V') IS NOT NULL DROP VIEW app.vwAnimalSummary;
IF OBJECT_ID('app.vwUserSummary', 'V') IS NOT NULL DROP VIEW app.vwUserSummary;
IF OBJECT_ID('app.FeedPosts', 'U') IS NOT NULL DROP TABLE app.FeedPosts;
IF OBJECT_ID('app.Animals', 'U') IS NOT NULL DROP TABLE app.Animals;
IF OBJECT_ID('app.Companies', 'U') IS NOT NULL DROP TABLE app.Companies;
IF OBJECT_ID('app.Users', 'U') IS NOT NULL DROP TABLE app.Users;
IF OBJECT_ID('app.UserTypes', 'U') IS NOT NULL DROP TABLE app.UserTypes;
GO

CREATE TABLE app.UserTypes (
    UserTypeId INT IDENTITY(1,1) PRIMARY KEY,
    Code VARCHAR(20) NOT NULL UNIQUE,
    Name VARCHAR(60) NOT NULL
);
GO

CREATE TABLE app.Users (
    UserId INT IDENTITY(1,1) PRIMARY KEY,
    UserTypeId INT NOT NULL,
    FullName VARCHAR(150) NOT NULL,
    Email VARCHAR(150) NOT NULL UNIQUE,
    PasswordHash VARCHAR(255) NOT NULL,
    DocumentNumber VARCHAR(20) NOT NULL,
    IsActive BIT NOT NULL CONSTRAINT DF_Users_IsActive DEFAULT 1,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Users_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Users_UserTypes FOREIGN KEY (UserTypeId) REFERENCES app.UserTypes(UserTypeId)
);
GO

CREATE TABLE app.Companies (
    CompanyId INT IDENTITY(1,1) PRIMARY KEY,
    OwnerUserId INT NOT NULL,
    CompanyName VARCHAR(150) NOT NULL,
    Cnpj VARCHAR(18) NOT NULL UNIQUE,
    AddressLine VARCHAR(200) NOT NULL,
    PhoneNumber VARCHAR(20) NOT NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Companies_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Companies_Users FOREIGN KEY (OwnerUserId) REFERENCES app.Users(UserId)
);
GO

CREATE TABLE app.Animals (
    AnimalId INT IDENTITY(1,1) PRIMARY KEY,
    OwnerUserId INT NOT NULL,
    CompanyId INT NULL,
    AnimalName VARCHAR(120) NOT NULL,
    Species VARCHAR(50) NOT NULL,
    Breed VARCHAR(80) NOT NULL,
    AgeYears INT NOT NULL,
    WeightKg DECIMAL(10,2) NOT NULL,
    CreatedAt DATETIME2 NOT NULL CONSTRAINT DF_Animals_CreatedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Animals_Users FOREIGN KEY (OwnerUserId) REFERENCES app.Users(UserId),
    CONSTRAINT FK_Animals_Companies FOREIGN KEY (CompanyId) REFERENCES app.Companies(CompanyId)
);
GO

CREATE TABLE app.FeedPosts (
    FeedPostId INT IDENTITY(1,1) PRIMARY KEY,
    TargetUserTypeId INT NULL,
    Title VARCHAR(120) NOT NULL,
    Body VARCHAR(500) NOT NULL,
    DisplayOrder INT NOT NULL CONSTRAINT DF_FeedPosts_DisplayOrder DEFAULT 1,
    IsActive BIT NOT NULL CONSTRAINT DF_FeedPosts_IsActive DEFAULT 1,
    PublishedAt DATETIME2 NOT NULL CONSTRAINT DF_FeedPosts_PublishedAt DEFAULT SYSDATETIME(),
    CONSTRAINT FK_FeedPosts_UserTypes FOREIGN KEY (TargetUserTypeId) REFERENCES app.UserTypes(UserTypeId)
);
GO
