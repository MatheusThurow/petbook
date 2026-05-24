package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class FirebaseCompanyRepository {

    public interface CompanyIdCallback {
        void onSuccess(long companyId);
        void onError(String message);
    }

    private FirebaseCompanyRepository() {
    }

    public static void saveCompany(
            Context context,
            Long ownerUserId,
            String companyName,
            String cnpj,
            String address,
            String phone,
            @NonNull CompanyIdCallback callback
    ) {
        long companyId = ownerUserId == null ? System.currentTimeMillis() : ownerUserId;
        Map<String, Object> values = new HashMap<>();
        values.put("id", companyId);
        values.put("ownerUserId", ownerUserId);
        values.put("companyName", companyName);
        values.put("cnpj", cnpj);
        values.put("addressLine", address);
        values.put("phoneNumber", phone);
        values.put("updatedAtMillis", System.currentTimeMillis());

        companiesCollection(context)
                .document(String.valueOf(companyId))
                .set(values, SetOptions.merge())
                .addOnSuccessListener(unused -> callback.onSuccess(companyId))
                .addOnFailureListener(exception -> callback.onError(
                        exception.getMessage() == null
                                ? "Nao foi possivel salvar a empresa no Firebase."
                                : exception.getMessage()
                ));
    }

    private static CollectionReference companiesCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("companies");
    }
}
