package com.example.petcompanyapp.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.petcompanyapp.database.AppDatabaseHelper;

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
}
