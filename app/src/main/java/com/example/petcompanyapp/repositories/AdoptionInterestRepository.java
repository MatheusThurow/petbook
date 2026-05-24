package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;

public final class AdoptionInterestRepository {

    private AdoptionInterestRepository() {
    }

    public static boolean registerInterest(
            Context context,
            long postId,
            long interestedUserId,
            String animalName
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put("post_id", postId);
            values.put("interested_user_id", interestedUserId);
            values.put("animal_name", animalName == null ? "" : animalName);
            values.put("created_at_millis", System.currentTimeMillis());
            return db.insertWithOnConflict(
                    AppDatabaseHelper.TABLE_ADOPTION_INTERESTS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_IGNORE
            ) > 0;
        } finally {
            db.close();
        }
    }
}
