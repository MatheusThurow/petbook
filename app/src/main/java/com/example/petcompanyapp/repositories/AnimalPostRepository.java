package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.FairAnimal;
import com.petbook.app.utils.FeedFilter;
import com.petbook.app.utils.PostType;

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
                + "(SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS + " fair WHERE fair.post_id = p.id) AS fair_animal_count, "
                + "u.name AS author_name, u.email AS author_email "
                + "FROM " + AppDatabaseHelper.TABLE_POSTS + " p "
                + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON u.id = p.author_user_id";

        Cursor cursor = db.rawQuery(sql, null);
        try {
            while (cursor.moveToNext()) {
                AnimalPost post = mapPost(cursor);
                if (matchesFilter(post, filter)) {
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

    public static boolean addPost(Context context, AnimalPost post, List<FairAnimal> fairAnimals) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = toContentValues(post);
        values.put("created_at_millis", System.currentTimeMillis());
        values.put("liked", post.isLiked() ? 1 : 0);
        values.put("like_count", post.getLikeCount());
        long postId = db.insert(AppDatabaseHelper.TABLE_POSTS, null, values);
        if (postId > 0) {
            replaceFairAnimals(db, postId, fairAnimals);
        }
        db.close();
        return postId > 0;
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

    public static AnimalPost findById(Context context, long postId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT p.id, p.author_user_id, p.post_type, p.animal_name, p.species, p.breed, "
                        + "p.age_description, p.description_text, p.contact_phone, p.latitude, p.longitude, "
                        + "p.location_reference, p.image_uri, p.created_at_millis, p.liked, p.like_count, "
                        + "(SELECT COUNT(*) FROM " + AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS + " fair WHERE fair.post_id = p.id) AS fair_animal_count, "
                        + "u.name AS author_name, u.email AS author_email "
                        + "FROM " + AppDatabaseHelper.TABLE_POSTS + " p "
                        + "INNER JOIN " + AppDatabaseHelper.TABLE_USERS + " u ON u.id = p.author_user_id "
                        + "WHERE p.id = ?",
                new String[]{String.valueOf(postId)}
        );

        try {
            if (cursor.moveToFirst()) {
                return mapPost(cursor);
            }
        } finally {
            cursor.close();
            db.close();
        }
        return null;
    }

    public static boolean updatePost(Context context, AnimalPost post, List<FairAnimal> fairAnimals) {
        if (post.getId() == null || post.getAuthorUserId() == null) {
            return false;
        }

        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = toContentValues(post);
        int updatedRows = db.update(
                AppDatabaseHelper.TABLE_POSTS,
                values,
                "id = ? AND author_user_id = ?",
                new String[]{String.valueOf(post.getId()), String.valueOf(post.getAuthorUserId())}
        );
        if (updatedRows > 0) {
            replaceFairAnimals(db, post.getId(), fairAnimals);
        }
        db.close();
        return updatedRows > 0;
    }

    public static boolean deletePost(Context context, long postId, long authorUserId) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        db.delete(
                AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS,
                "post_id = ?",
                new String[]{String.valueOf(postId)}
        );
        int deletedRows = db.delete(
                AppDatabaseHelper.TABLE_POSTS,
                "id = ? AND author_user_id = ?",
                new String[]{String.valueOf(postId), String.valueOf(authorUserId)}
        );
        db.close();
        return deletedRows > 0;
    }

    public static List<FairAnimal> getFairAnimalsForPost(Context context, long postId) {
        List<FairAnimal> fairAnimals = new ArrayList<>();
        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS,
                null,
                "post_id = ?",
                new String[]{String.valueOf(postId)},
                null,
                null,
                "animal_name COLLATE NOCASE ASC"
        );

        try {
            while (cursor.moveToNext()) {
                fairAnimals.add(new FairAnimal(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("post_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("animal_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("species")),
                        cursor.getString(cursor.getColumnIndexOrThrow("breed")),
                        cursor.getString(cursor.getColumnIndexOrThrow("age_description"))
                ));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return fairAnimals;
    }

    private static ContentValues toContentValues(AnimalPost post) {
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
        return values;
    }

    private static boolean matchesFilter(AnimalPost post, String filter) {
        if (FeedFilter.ALL.equals(filter)) {
            return true;
        }
        if (FeedFilter.LOST.equals(filter)) {
            return PostType.isLost(post.getPostType());
        }
        if (FeedFilter.ADOPTION.equals(filter)) {
            return PostType.isAdoptionRelated(post.getPostType());
        }
        return post.getPostType().equals(filter);
    }

    private static void replaceFairAnimals(SQLiteDatabase db, long postId, List<FairAnimal> fairAnimals) {
        db.delete(
                AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS,
                "post_id = ?",
                new String[]{String.valueOf(postId)}
        );

        if (fairAnimals == null || fairAnimals.isEmpty()) {
            return;
        }

        for (FairAnimal fairAnimal : fairAnimals) {
            ContentValues values = new ContentValues();
            values.put("post_id", postId);
            values.put("animal_name", fairAnimal.getName());
            values.put("species", fairAnimal.getSpecies());
            values.put("breed", fairAnimal.getBreed());
            values.put("age_description", fairAnimal.getAgeDescription());
            db.insert(AppDatabaseHelper.TABLE_FAIR_POST_ANIMALS, null, values);
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
                cursor.getString(cursor.getColumnIndexOrThrow("author_email")),
                cursor.getLong(cursor.getColumnIndexOrThrow("created_at_millis")),
                cursor.getInt(cursor.getColumnIndexOrThrow("liked")) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow("like_count")),
                cursor.getInt(cursor.getColumnIndexOrThrow("fair_animal_count"))
        );
    }
}

