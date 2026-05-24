package com.petbook.app.models;

public class FairAnimal {

    private final Long id;
    private final Long postId;
    private final String name;
    private final String species;
    private final String breed;
    private final String ageDescription;
    private final String imageUri;

    public FairAnimal(
            Long id,
            Long postId,
            String name,
            String species,
            String breed,
            String ageDescription,
            String imageUri
    ) {
        this.id = id;
        this.postId = postId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.ageDescription = ageDescription;
        this.imageUri = imageUri;
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public String getAgeDescription() {
        return ageDescription;
    }

    public String getImageUri() {
        return imageUri;
    }
}
