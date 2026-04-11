package com.example.petcompanyapp.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.petcompanyapp.database.AppDatabaseHelper;
import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.utils.FeedFilter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnimalPostRepository {

    private AnimalPostRepository() {
        // Repositorio persistido em SQLite local.
    }

    public static List<AnimalPost> getPosts(Context context, String filter) {
        List<AnimalPost> result = new ArrayList<>();
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        String sql = "SELECT p.id, p.author_user_id, p.post_type, p.animal_name, p.species, p.breed, "
                + "p.age_description, p.description_text, p.contact_phone, p.latitude, p.longitude, "
                + "p.location_reference, p.image_uri, p.created_at_millis, p.liked, p.like_count, "
                + "u.name AS author_name "
                + "FROM " + AppDatabaseHelper.TABLE_POSTS + " p "
                + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON u.id = p.author_user_id";

        Cursor cursor = db.rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                AnimalPost post = mapPost(cursor);
                if (FeedFilter.ALL.equals(filter) || post.getPostType().equals(filter)) {
                    result.add(post);
                }
            }
        } finally {
            cursor.close();
            db.close();
        }

        result.sort(Comparator.comparingLong(AnimalPost::getCreatedAtMillis).reversed());
        return result;
    }

    public static void addPost(Context context, AnimalPost post) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("author_user_id", post.getAuthorUserId());
        values.put("post_type", post.getPostType());
        values.put("animal_name", post.getAnimalName());
        values.put("species", post.getSpecies());
        values.put("breed", post.getBreed());
        values.put("age_description", post.getAge());
        values.put("description_text", post.getDescription());
        values.put("contact_phone", post.getContactPhone());
        if (post.getLatitude() != null) {
            values.put("latitude", post.getLatitude());
        } else {
            values.putNull("latitude");
        }
        if (post.getLongitude() != null) {
            values.put("longitude", post.getLongitude());
        } else {
            values.putNull("longitude");
        }
        values.put("location_reference", post.getLocationReference());
        values.put("image_uri", post.getImageUri());
        values.put("created_at_millis", System.currentTimeMillis());
        values.put("liked", post.isLiked() ? 1 : 0);
        values.put("like_count", post.getLikeCount());
        db.insert(AppDatabaseHelper.TABLE_POSTS, null, values);
        db.close();
    }

    public static void toggleLike(Context context, long postId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_POSTS,
                new String[]{"liked", "like_count"},
                "id = ?",
                new String[]{String.valueOf(postId)},
                null,
                null,
                null
        );

        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            boolean currentLiked = cursor.getInt(cursor.getColumnIndexOrThrow("liked")) == 1;
            int currentLikeCount = cursor.getInt(cursor.getColumnIndexOrThrow("like_count"));
            boolean newLiked = !currentLiked;
            int newLikeCount = newLiked
                    ? currentLikeCount + 1
                    : Math.max(0, currentLikeCount - 1);

            ContentValues values = new ContentValues();
            values.put("liked", newLiked ? 1 : 0);
            values.put("like_count", newLikeCount);
            db.update(
                    AppDatabaseHelper.TABLE_POSTS,
                    values,
                    "id = ?",
                    new String[]{String.valueOf(postId)}
            );
        } finally {
            cursor.close();
            db.close();
        }
    }

    private static AnimalPost mapPost(Cursor cursor) {
        int latitudeIndex = cursor.getColumnIndexOrThrow("latitude");
        int longitudeIndex = cursor.getColumnIndexOrThrow("longitude");
        Double latitude = cursor.isNull(latitudeIndex) ? null : cursor.getDouble(latitudeIndex);
        Double longitude = cursor.isNull(longitudeIndex) ? null : cursor.getDouble(longitudeIndex);

        return new AnimalPost(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getLong(cursor.getColumnIndexOrThrow("author_user_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("post_type")),
                cursor.getString(cursor.getColumnIndexOrThrow("animal_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("species")),
                cursor.getString(cursor.getColumnIndexOrThrow("breed")),
                cursor.getString(cursor.getColumnIndexOrThrow("age_description")),
                cursor.getString(cursor.getColumnIndexOrThrow("description_text")),
                cursor.getString(cursor.getColumnIndexOrThrow("contact_phone")),
                latitude,
                longitude,
                cursor.getString(cursor.getColumnIndexOrThrow("location_reference")),
                cursor.getString(cursor.getColumnIndexOrThrow("image_uri")),
                cursor.getString(cursor.getColumnIndexOrThrow("author_name")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at_millis")),
                cursor.getInt(cursor.getColumnIndexOrThrow("liked")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("like_count"))
        );
    }
}
