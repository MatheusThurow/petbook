package com.example.petcompanyapp.utils;

public final class PostType {

    public static final String LOST = "LOST";
    public static final String ADOPTION = "ADOPTION";

    private PostType() {
        // Constantes para tipo de post.
    }

    public static boolean isLost(String type) {
        return LOST.equals(type);
    }
}
