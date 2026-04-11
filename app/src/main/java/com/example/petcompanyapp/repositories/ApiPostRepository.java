package com.example.petcompanyapp.repositories;

import android.content.Context;

import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.network.ApiHttpClient;
import com.example.petcompanyapp.utils.FeedFilter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class ApiPostRepository {

    private ApiPostRepository() {
    }

    public static List<AnimalPost> getPosts(Context context, String filter) throws Exception {
        String serverFilter = FeedFilter.ALL.equals(filter) ? "ALL" : filter;
        String response = ApiHttpClient.get(context, "/api/posts?filter=" + serverFilter);
        JSONArray array = new JSONArray(response);
        List<AnimalPost> posts = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            posts.add(mapPost(array.getJSONObject(index)));
        }
        return posts;
    }

    public static AnimalPost createPost(
            Context context,
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
            String imageUri
    ) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("authorUserId", authorUserId);
        payload.put("postType", postType);
        payload.put("animalName", animalName);
        payload.put("species", species);
        payload.put("breed", breed);
        payload.put("age", age);
        payload.put("description", description);
        payload.put("contactPhone", contactPhone);
        payload.put("imageUri", imageUri);
        payload.put("locationReference", locationReference == null ? "" : locationReference);
        if (latitude != null) {
            payload.put("latitude", latitude);
        } else {
            payload.put("latitude", JSONObject.NULL);
        }
        if (longitude != null) {
            payload.put("longitude", longitude);
        } else {
            payload.put("longitude", JSONObject.NULL);
        }

        String response = ApiHttpClient.post(context, "/api/posts", payload);
        return mapPost(new JSONObject(response));
    }

    private static AnimalPost mapPost(JSONObject jsonObject) throws Exception {
        return new AnimalPost(
                jsonObject.getLong("id"),
                jsonObject.getLong("authorUserId"),
                jsonObject.getString("postType"),
                jsonObject.getString("animalName"),
                jsonObject.getString("species"),
                jsonObject.getString("breed"),
                jsonObject.getString("age"),
                jsonObject.getString("description"),
                jsonObject.getString("contactPhone"),
                jsonObject.isNull("latitude") ? null : jsonObject.getDouble("latitude"),
                jsonObject.isNull("longitude") ? null : jsonObject.getDouble("longitude"),
                jsonObject.optString("locationReference"),
                jsonObject.optString("imageUri"),
                jsonObject.optString("authorName", "usuario"),
                jsonObject.optLong("createdAtMillis", System.currentTimeMillis()),
                jsonObject.optBoolean("liked", false),
                jsonObject.optInt("likeCount", 0)
        );
    }
}
