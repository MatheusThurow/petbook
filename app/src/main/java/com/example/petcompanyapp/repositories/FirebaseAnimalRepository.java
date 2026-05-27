package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public final class FirebaseAnimalRepository {

    public interface AnimalIdCallback {
        void onSuccess(long animalId);
        void onError(String message);
    }

    private FirebaseAnimalRepository() {
    }

    public static void saveAnimal(
            Context context,
            Long ownerUserId,
            Long companyId,
            String animalName,
            String species,
            String breed,
            int ageYears,
            int ageMonths,
            double weightKg,
            @NonNull AnimalIdCallback callback
    ) {
        long animalId = System.currentTimeMillis();
        Map<String, Object> values = new HashMap<>();
        values.put("id", animalId);
        values.put("ownerUserId", ownerUserId);
        values.put("companyId", companyId);
        values.put("animalName", animalName);
        values.put("species", species);
        values.put("breed", breed);
        values.put("ageYears", ageYears);
        values.put("ageMonths", ageMonths);
        values.put("weightKg", weightKg);
        values.put("createdAtMillis", System.currentTimeMillis());

        animalsCollection(context)
                .document(String.valueOf(animalId))
                .set(values)
                .addOnSuccessListener(unused -> callback.onSuccess(animalId))
                .addOnFailureListener(exception -> callback.onError(
                        exception.getMessage() == null
                                ? "Nao foi possivel salvar o animal no Firebase."
                                : exception.getMessage()
                ));
    }

    private static CollectionReference animalsCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("animals");
    }
}
