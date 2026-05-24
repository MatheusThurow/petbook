package com.petbook.app.repositories;

import android.content.Context;

import androidx.annotation.NonNull;

import com.petbook.app.models.CompanyProfile;
import com.petbook.app.utils.FirebaseChatConfig;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public final class FirebaseCompanyRepository {

    public interface CompanyIdCallback {
        void onSuccess(long companyId);
        void onError(String message);
    }

    public interface CompanyProfileCallback {
        void onSuccess(CompanyProfile companyProfile);
        void onEmpty();
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
        usersCollection(context)
                .whereEqualTo("localUserId", ownerUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onError("Nao foi possivel localizar o usuario da empresa no Firebase.");
                        return;
                    }

                    Map<String, Object> values = new HashMap<>();
                    values.put("companyProfile", buildCompanyMap(companyId, ownerUserId, companyName, cnpj, address, phone));
                    values.put("updatedAtMillis", System.currentTimeMillis());

                    result.getDocuments().get(0).getReference()
                            .set(values, SetOptions.merge())
                            .addOnSuccessListener(unused -> callback.onSuccess(companyId))
                            .addOnFailureListener(exception -> callback.onError(
                                    exception.getMessage() == null
                                            ? "Nao foi possivel salvar a empresa no Firebase."
                                            : exception.getMessage()
                            ));
                })
                .addOnFailureListener(exception -> callback.onError(
                        exception.getMessage() == null
                                ? "Nao foi possivel salvar a empresa no Firebase."
                                : exception.getMessage()
                ));
    }

    public static void findByOwnerUserId(
            Context context,
            Long ownerUserId,
            @NonNull CompanyProfileCallback callback
    ) {
        if (ownerUserId == null) {
            callback.onEmpty();
            return;
        }

        usersCollection(context)
                .whereEqualTo("localUserId", ownerUserId)
                .limit(1)
                .get()
                .addOnSuccessListener(result -> {
                    if (result.isEmpty()) {
                        callback.onEmpty();
                        return;
                    }

                    CompanyProfile companyProfile = mapCompany(result.getDocuments().get(0));
                    if (companyProfile == null) {
                        callback.onEmpty();
                        return;
                    }
                    callback.onSuccess(companyProfile);
                })
                .addOnFailureListener(exception -> callback.onError(
                        exception.getMessage() == null
                                ? "Nao foi possivel carregar a empresa no Firebase."
                                : exception.getMessage()
                ));
    }

    private static CollectionReference usersCollection(Context context) {
        FirebaseFirestore db = FirebaseChatConfig.getFirestore(context);
        return db.collection("users");
    }

    private static Map<String, Object> buildCompanyMap(
            long companyId,
            Long ownerUserId,
            String companyName,
            String cnpj,
            String address,
            String phone
    ) {
        Map<String, Object> values = new HashMap<>();
        values.put("id", companyId);
        values.put("ownerUserId", ownerUserId);
        values.put("companyName", companyName);
        values.put("cnpj", cnpj);
        values.put("addressLine", address);
        values.put("phoneNumber", phone);
        values.put("updatedAtMillis", System.currentTimeMillis());
        return values;
    }

    private static CompanyProfile mapCompany(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Map<String, Object> companyData = (Map<String, Object>) snapshot.get("companyProfile");
        if (companyData == null || companyData.isEmpty()) {
            return null;
        }

        Long id = asLong(companyData.get("id"));
        Long ownerUserId = asLong(companyData.get("ownerUserId"));
        String companyName = safe(companyData.get("companyName"));
        String cnpj = safe(companyData.get("cnpj"));
        String address = safe(companyData.get("addressLine"));
        String phone = safe(companyData.get("phoneNumber"));

        return new CompanyProfile(id, ownerUserId, companyName, cnpj, address, phone);
    }

    private static Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
