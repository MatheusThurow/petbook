package com.example.petcompanyapp.repositories;

import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.utils.FeedFilter;
import com.example.petcompanyapp.utils.PostType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnimalPostRepository {

    private static final List<AnimalPost> POSTS = new ArrayList<>();
    private static long nextId = 3L;

    static {
        POSTS.add(new AnimalPost(
                1L,
                1L,
                PostType.LOST,
                "Thor",
                "Cachorro",
                "Labrador",
                "4 anos",
                "Animal perdido na regiao central, usa coleira azul e atende pelo nome Thor.",
                "(11) 98888-0001",
                -23.550520,
                -46.633308,
                "Ultima vez visto proximo a praca central.",
                null,
                "Ana Souza",
                System.currentTimeMillis() - 1000L * 60L * 30L,
                false,
                12
        ));

        POSTS.add(new AnimalPost(
                2L,
                2L,
                PostType.ADOPTION,
                "Luna",
                "Gato",
                "Siames",
                "2 anos",
                "Gata docil, castrada e pronta para um novo lar responsavel.",
                "(11) 99999-1234",
                null,
                null,
                "",
                null,
                "Clinica Feliz",
                System.currentTimeMillis() - 1000L * 60L * 90L,
                true,
                27
        ));
    }

    private AnimalPostRepository() {
        // Repositorio local em memoria para a timeline do app.
    }

    public static List<AnimalPost> getPosts(String filter) {
        List<AnimalPost> result = new ArrayList<>();

        for (AnimalPost post : POSTS) {
            if (FeedFilter.ALL.equals(filter) || post.getPostType().equals(filter)) {
                result.add(post);
            }
        }

        result.sort(Comparator.comparingLong(AnimalPost::getCreatedAtMillis).reversed());
        return result;
    }

    public static void addPost(AnimalPost post) {
        AnimalPost postWithId = new AnimalPost(
                nextId++,
                post.getAuthorUserId(),
                post.getPostType(),
                post.getAnimalName(),
                post.getSpecies(),
                post.getBreed(),
                post.getAge(),
                post.getDescription(),
                post.getContactPhone(),
                post.getLatitude(),
                post.getLongitude(),
                post.getLocationReference(),
                post.getImageUri(),
                post.getAuthorName(),
                System.currentTimeMillis(),
                post.isLiked(),
                post.getLikeCount()
        );
        POSTS.add(postWithId);
    }

    public static void toggleLike(long postId) {
        for (int i = 0; i < POSTS.size(); i++) {
            AnimalPost current = POSTS.get(i);
            if (current.getId() != null && current.getId() == postId) {
                boolean newLiked = !current.isLiked();
                int newLikeCount = newLiked
                        ? current.getLikeCount() + 1
                        : Math.max(0, current.getLikeCount() - 1);

                POSTS.set(i, new AnimalPost(
                        current.getId(),
                        current.getAuthorUserId(),
                        current.getPostType(),
                        current.getAnimalName(),
                        current.getSpecies(),
                        current.getBreed(),
                        current.getAge(),
                        current.getDescription(),
                        current.getContactPhone(),
                        current.getLatitude(),
                        current.getLongitude(),
                        current.getLocationReference(),
                        current.getImageUri(),
                        current.getAuthorName(),
                        current.getCreatedAtMillis(),
                        newLiked,
                        newLikeCount
                ));
                return;
            }
        }
    }
}
