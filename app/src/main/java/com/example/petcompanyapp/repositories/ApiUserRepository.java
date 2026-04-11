package com.example.petcompanyapp.repositories;

import android.content.Context;

import com.example.petcompanyapp.models.User;
import com.example.petcompanyapp.network.ApiHttpClient;

import org.json.JSONException;
import org.json.JSONObject;

public final class ApiUserRepository {

    private ApiUserRepository() {
    }

    public static User login(Context context, String email, String password) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("email", email);
        payload.put("password", password);
        String response = ApiHttpClient.post(context, "/api/auth/login", payload);
        return mapUser(new JSONObject(response));
    }

    public static User register(
            Context context,
            String userType,
            String name,
            String email,
            String password,
            String document
    ) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("userType", userType);
        payload.put("name", name);
        payload.put("email", email);
        payload.put("password", password);
        payload.put("document", document);
        String response = ApiHttpClient.post(context, "/api/users/register", payload);
        return mapUser(new JSONObject(response));
    }

    public static User findById(Context context, long userId) throws Exception {
        String response = ApiHttpClient.get(context, "/api/users/" + userId);
        return mapUser(new JSONObject(response));
    }

    public static User updateProfile(Context context, long userId, String name, String email) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("email", email);
        String response = ApiHttpClient.put(context, "/api/users/" + userId, payload);
        return mapUser(new JSONObject(response));
    }

    private static User mapUser(JSONObject jsonObject) throws JSONException {
        return new User(
                jsonObject.getLong("id"),
                jsonObject.getString("userType"),
                jsonObject.getString("name"),
                jsonObject.getString("email"),
                null,
                jsonObject.optString("document"),
                jsonObject.optBoolean("active", true)
        );
    }
}
