package com.example.petcompanyapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "petcompany.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_COMPANIES = "companies";
    public static final String TABLE_ANIMALS = "animals";
    public static final String TABLE_POSTS = "animal_posts";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_type TEXT NOT NULL,"
                + "name TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "password TEXT NOT NULL,"
                + "document_number TEXT NOT NULL,"
                + "is_active INTEGER NOT NULL DEFAULT 1"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_COMPANIES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "owner_user_id INTEGER,"
                + "company_name TEXT NOT NULL,"
                + "cnpj TEXT NOT NULL UNIQUE,"
                + "address_line TEXT NOT NULL,"
                + "phone_number TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_ANIMALS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "owner_user_id INTEGER,"
                + "company_id INTEGER,"
                + "animal_name TEXT NOT NULL,"
                + "species TEXT NOT NULL,"
                + "breed TEXT NOT NULL,"
                + "age_years INTEGER NOT NULL,"
                + "weight_kg REAL NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_POSTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "author_user_id INTEGER NOT NULL,"
                + "post_type TEXT NOT NULL,"
                + "animal_name TEXT NOT NULL,"
                + "species TEXT NOT NULL,"
                + "breed TEXT NOT NULL,"
                + "age_description TEXT NOT NULL,"
                + "description_text TEXT NOT NULL,"
                + "contact_phone TEXT NOT NULL,"
                + "latitude REAL,"
                + "longitude REAL,"
                + "location_reference TEXT,"
                + "image_uri TEXT,"
                + "created_at_millis INTEGER NOT NULL,"
                + "liked INTEGER NOT NULL DEFAULT 0,"
                + "like_count INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY(author_user_id) REFERENCES " + TABLE_USERS + "(id)"
                + ")");

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Sem migracao por enquanto.
    }

    private void seedInitialData(SQLiteDatabase db) {
        long anaUserId = insertUser(
                db,
                "PERSON",
                "Ana Souza",
                "ana@petcompany.com",
                "123456",
                "12345678901"
        );

        long clinicUserId = insertUser(
                db,
                "COMPANY",
                "Clinica Feliz",
                "contato@clinicafeliz.com",
                "123456",
                "12345678000199"
        );

        ContentValues companyValues = new ContentValues();
        companyValues.put("owner_user_id", clinicUserId);
        companyValues.put("company_name", "Clinica Feliz");
        companyValues.put("cnpj", "12.345.678/0001-99");
        companyValues.put("address_line", "Rua das Flores, 150");
        companyValues.put("phone_number", "(11) 99999-1234");
        long companyId = db.insert(TABLE_COMPANIES, null, companyValues);

        ContentValues thorAnimal = new ContentValues();
        thorAnimal.put("owner_user_id", anaUserId);
        thorAnimal.putNull("company_id");
        thorAnimal.put("animal_name", "Thor");
        thorAnimal.put("species", "Cachorro");
        thorAnimal.put("breed", "Labrador");
        thorAnimal.put("age_years", 4);
        thorAnimal.put("weight_kg", 28.5);
        db.insert(TABLE_ANIMALS, null, thorAnimal);

        ContentValues lunaAnimal = new ContentValues();
        lunaAnimal.put("owner_user_id", clinicUserId);
        lunaAnimal.put("company_id", companyId);
        lunaAnimal.put("animal_name", "Luna");
        lunaAnimal.put("species", "Gato");
        lunaAnimal.put("breed", "Siames");
        lunaAnimal.put("age_years", 2);
        lunaAnimal.put("weight_kg", 4.3);
        db.insert(TABLE_ANIMALS, null, lunaAnimal);

        ContentValues thorPost = new ContentValues();
        thorPost.put("author_user_id", anaUserId);
        thorPost.put("post_type", "LOST");
        thorPost.put("animal_name", "Thor");
        thorPost.put("species", "Cachorro");
        thorPost.put("breed", "Labrador");
        thorPost.put("age_description", "4 anos");
        thorPost.put("description_text", "Animal perdido na regiao central, usa coleira azul e atende pelo nome Thor.");
        thorPost.put("contact_phone", "(11) 98888-0001");
        thorPost.put("latitude", -23.550520d);
        thorPost.put("longitude", -46.633308d);
        thorPost.put("location_reference", "Ultima vez visto proximo a praca central.");
        thorPost.put("image_uri", "");
        thorPost.put("created_at_millis", System.currentTimeMillis() - 1000L * 60L * 30L);
        thorPost.put("liked", 0);
        thorPost.put("like_count", 12);
        db.insert(TABLE_POSTS, null, thorPost);

        ContentValues lunaPost = new ContentValues();
        lunaPost.put("author_user_id", clinicUserId);
        lunaPost.put("post_type", "ADOPTION");
        lunaPost.put("animal_name", "Luna");
        lunaPost.put("species", "Gato");
        lunaPost.put("breed", "Siames");
        lunaPost.put("age_description", "2 anos");
        lunaPost.put("description_text", "Gata docil, castrada e pronta para um novo lar responsavel.");
        lunaPost.put("contact_phone", "(11) 99999-1234");
        lunaPost.putNull("latitude");
        lunaPost.putNull("longitude");
        lunaPost.put("location_reference", "");
        lunaPost.put("image_uri", "");
        lunaPost.put("created_at_millis", System.currentTimeMillis() - 1000L * 60L * 90L);
        lunaPost.put("liked", 1);
        lunaPost.put("like_count", 27);
        db.insert(TABLE_POSTS, null, lunaPost);
    }

    private long insertUser(
            SQLiteDatabase db,
            String userType,
            String name,
            String email,
            String password,
            String document
    ) {
        ContentValues values = new ContentValues();
        values.put("user_type", userType);
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        values.put("document_number", document);
        values.put("is_active", 1);
        return db.insert(TABLE_USERS, null, values);
    }
}
