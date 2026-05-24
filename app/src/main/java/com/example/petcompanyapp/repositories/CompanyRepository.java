package com.petbook.app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.petbook.app.database.AppDatabaseHelper;
import com.petbook.app.models.CompanyProfile;

public final class CompanyRepository {

    private CompanyRepository() {
        // Repositorio SQLite para empresas.
    }

    public static long saveCompany(
            Context context,
            Long ownerUserId,
            String companyName,
            String cnpj,
            String address,
            String phone
    ) {
        SQLiteDatabase db = new AppDatabaseHelper(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        if (ownerUserId != null) {
            values.put("owner_user_id", ownerUserId);
        } else {
            values.putNull("owner_user_id");
        }
        values.put("company_name", companyName);
        values.put("cnpj", cnpj);
        values.put("address_line", address);
        values.put("phone_number", phone);
        long companyId = db.insertWithOnConflict(
                AppDatabaseHelper.TABLE_COMPANIES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );
        db.close();
        return companyId;
    }

    public static CompanyProfile findByOwnerUserId(Context context, Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }

        SQLiteDatabase db = new AppDatabaseHelper(context).getReadableDatabase();
        Cursor cursor = db.query(
                AppDatabaseHelper.TABLE_COMPANIES,
                new String[]{"id", "owner_user_id", "company_name", "cnpj", "address_line", "phone_number"},
                "owner_user_id = ?",
                new String[]{String.valueOf(ownerUserId)},
                null,
                null,
                "id DESC",
                "1"
        );

        CompanyProfile company = null;
        if (cursor.moveToFirst()) {
            company = new CompanyProfile(
                    cursor.getLong(0),
                    cursor.isNull(1) ? null : cursor.getLong(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5)
            );
        }

        cursor.close();
        db.close();
        return company;
    }
}

