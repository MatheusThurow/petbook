package com.example.petcompanyapp.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.petcompanyapp.database.AppDatabaseHelper;

public final class AnimalRepository {

    private AnimalRepository() {
        // Repositorio SQLite para animais.
    }

    public static long saveAnimal(
            Context context,
            Long ownerUserId,
            Long companyId,
            String animalName,
            String species,
            String breed,
            int ageYears,
            double weightKg
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        if (ownerUserId != null) {
            values.put("owner_user_id", ownerUserId);
        } else {
            values.putNull("owner_user_id");
        }
        if (companyId != null) {
            values.put("company_id", companyId);
        } else {
            values.putNull("company_id");
        }
        values.put("animal_name", animalName);
        values.put("species", species);
        values.put("breed", breed);
        values.put("age_years", ageYears);
        values.put("weight_kg", weightKg);
        long animalId = db.insert(AppDatabaseHelper.TABLE_ANIMALS, null, values);
        db.close();
        return animalId;
    }
}
