package com.petbook.app.utils;

public final class PostType {

    public static final String LOST = "LOST";
    public static final String ADOPTION = "ADOPTION";
    public static final String FAIR = "FAIR";

    private PostType() {
        // Constantes para tipo de post.
    }

    public static boolean isLost(String type) {
        return LOST.equals(type);
    }

    public static boolean isFair(String type) {
        return FAIR.equals(type);
    }

    public static boolean isAdoptionRelated(String type) {
        return ADOPTION.equals(type) || FAIR.equals(type);
    }
}

