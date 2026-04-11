package com.example.petcompanyapp.repositories;

import android.content.Context;

import com.example.petcompanyapp.network.ApiHttpClient;

import org.json.JSONObject;

public final class ApiCompanyRepository {

    private ApiCompanyRepository() {
    }

    public static boolean saveCompany(
            Context context,
            long ownerUserId,
            String companyName,
            String cnpj,
            String address,
            String phone
    ) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("ownerUserId", ownerUserId);
        payload.put("companyName", companyName);
        payload.put("cnpj", cnpj);
        payload.put("address", address);
        payload.put("phone", phone);
        ApiHttpClient.post(context, "/api/companies", payload);
        return true;
    }
}
