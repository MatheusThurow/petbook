USE PetCompanyDB;
GO

CREATE OR ALTER VIEW app.vwUserSummary
AS
SELECT
    u.UserId,
    ut.Code AS UserTypeCode,
    ut.Name AS UserTypeName,
    u.FullName,
    u.Email,
    u.DocumentNumber,
    u.IsActive,
    u.CreatedAt
FROM app.Users u
INNER JOIN app.UserTypes ut ON ut.UserTypeId = u.UserTypeId;
GO

CREATE OR ALTER VIEW app.vwAnimalSummary
AS
SELECT
    a.AnimalId,
    a.AnimalName,
    a.Species,
    a.Breed,
    a.AgeYears,
    a.WeightKg,
    u.FullName AS OwnerName,
    c.CompanyName
FROM app.Animals a
INNER JOIN app.Users u ON u.UserId = a.OwnerUserId
LEFT JOIN app.Companies c ON c.CompanyId = a.CompanyId;
GO

CREATE OR ALTER VIEW app.vwActiveFeed
AS
SELECT
    fp.FeedPostId,
    ISNULL(ut.Code, 'ALL') AS TargetCode,
    fp.Title,
    fp.Body,
    fp.DisplayOrder,
    fp.PublishedAt
FROM app.FeedPosts fp
LEFT JOIN app.UserTypes ut ON ut.UserTypeId = fp.TargetUserTypeId
WHERE fp.IsActive = 1;
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
