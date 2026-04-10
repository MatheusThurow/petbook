package com.example.petcompanyapp.models;

public class AnimalPost {
    private final Long id;
    private final Long authorUserId;
    private final String postType;
    private final String animalName;
    private final String species;
    private final String breed;
    private final String age;
    private final String description;
    private final String contactPhone;
    private final Double latitude;
    private final Double longitude;
    private final String locationReference;
    private final String imageUri;
    private final String authorName;
    private final long createdAtMillis;
    private final boolean liked;
    private final int likeCount;

    public AnimalPost(
            String postType,
            String animalName,
            String species,
            String breed,
            String age,
            String description,
            String contactPhone,
            Double latitude,
            Double longitude,
            String locationReference,
            String imageUri
    ) {
        this(
                null,
                null,
                postType,
                animalName,
                species,
                breed,
                age,
                description,
                contactPhone,
                latitude,
                longitude,
                locationReference,
                imageUri,
                "usuario",
                System.currentTimeMillis(),
                false,
                0
        );
    }

    public AnimalPost(
            Long id,
            Long authorUserId,
            String postType,
            String animalName,
            String species,
            String breed,
            String age,
            String description,
            String contactPhone,
            Double latitude,
            Double longitude,
            String locationReference,
            String imageUri,
            String authorName,
            long createdAtMillis,
            boolean liked,
            int likeCount
    ) {
        this.id = id;
        this.authorUserId = authorUserId;
        this.postType = postType;
        this.animalName = animalName;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.description = description;
        this.contactPhone = contactPhone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationReference = locationReference;
        this.imageUri = imageUri;
        this.authorName = authorName;
        this.createdAtMillis = createdAtMillis;
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public Long getId() {
        return id;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getPostType() {
        return postType;
    }

    public String getAnimalName() {
        return animalName;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public String getAge() {
        return age;
    }

    public String getDescription() {
        return description;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getLocationReference() {
        return locationReference;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getAuthorName() {
        return authorName;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public boolean isLiked() {
        return liked;
    }

    public int getLikeCount() {
        return likeCount;
    }
}
