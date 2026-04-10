package com.example.petcompanyapp.models;

public class AnimalPost {
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
            String locationReference
    ) {
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
}
