package com.petbook.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "petcompany.db";
    private static final int DATABASE_VERSION = 8;

    public static final String TABLE_USERS = "users";
    public static final String TABLE_COMPANIES = "companies";
    public static final String TABLE_ANIMALS = "animals";
    public static final String TABLE_POSTS = "animal_posts";
    public static final String TABLE_FAIR_POST_ANIMALS = "fair_post_animals";
    public static final String TABLE_POST_COMMENTS = "post_comments";
    public static final String TABLE_ADOPTION_INTERESTS = "adoption_interests";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_CONVERSATIONS = "chat_conversations";
    public static final String TABLE_MESSAGES = "chat_messages";

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createUsersTable(db);
        createCompaniesTable(db);
        createAnimalsTable(db);
        createPostsTable(db);
        createFairPostAnimalsTable(db);
        createPostCommentsTable(db);
        createAdoptionInterestsTable(db);
        createNotificationsTable(db);
        createConversationsTable(db);
        createMessagesTable(db);

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createConversationsTable(db);
            createMessagesTable(db);
        }
        if (oldVersion < 3) {
            createFairPostAnimalsTable(db);
        }
        if (oldVersion < 4) {
            createPostCommentsTable(db);
            createAdoptionInterestsTable(db);
            createNotificationsTable(db);
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_POST_COMMENTS + " ADD COLUMN parent_comment_id INTEGER");
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_FAIR_POST_ANIMALS + " ADD COLUMN image_uri TEXT");
        }
        if (oldVersion < 7) {
            removeSamplePosts(db);
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE " + TABLE_ANIMALS + " ADD COLUMN age_months INTEGER NOT NULL DEFAULT 0");
        }
    }

    private void createUsersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_type TEXT NOT NULL,"
                + "name TEXT NOT NULL,"
                + "email TEXT NOT NULL UNIQUE,"
                + "password TEXT NOT NULL,"
                + "document_number TEXT NOT NULL,"
                + "is_active INTEGER NOT NULL DEFAULT 1"
                + ")");
    }

    private void createCompaniesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_COMPANIES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "owner_user_id INTEGER,"
                + "company_name TEXT NOT NULL,"
                + "cnpj TEXT NOT NULL UNIQUE,"
                + "address_line TEXT NOT NULL,"
                + "phone_number TEXT NOT NULL"
                + ")");
    }

    private void createAnimalsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ANIMALS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "owner_user_id INTEGER,"
                + "company_id INTEGER,"
                + "animal_name TEXT NOT NULL,"
                + "species TEXT NOT NULL,"
                + "breed TEXT NOT NULL,"
                + "age_years INTEGER NOT NULL,"
                + "age_months INTEGER NOT NULL DEFAULT 0,"
                + "weight_kg REAL NOT NULL"
                + ")");
    }

    private void createPostsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POSTS + " ("
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
    }

    private void createConversationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONVERSATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "user_one_id INTEGER NOT NULL,"
                + "user_two_id INTEGER NOT NULL,"
                + "created_at_millis INTEGER NOT NULL,"
                + "last_message_text TEXT,"
                + "last_message_at_millis INTEGER,"
                + "UNIQUE(user_one_id, user_two_id)"
                + ")");
    }

    private void createFairPostAnimalsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FAIR_POST_ANIMALS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "post_id INTEGER NOT NULL,"
                + "animal_name TEXT NOT NULL,"
                + "species TEXT NOT NULL,"
                + "breed TEXT NOT NULL,"
                + "age_description TEXT NOT NULL,"
                + "image_uri TEXT,"
                + "FOREIGN KEY(post_id) REFERENCES " + TABLE_POSTS + "(id)"
                + ")");
    }

    private void createPostCommentsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POST_COMMENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "post_id INTEGER NOT NULL,"
                + "author_user_id INTEGER NOT NULL,"
                + "message_text TEXT NOT NULL,"
                + "parent_comment_id INTEGER,"
                + "created_at_millis INTEGER NOT NULL,"
                + "FOREIGN KEY(post_id) REFERENCES " + TABLE_POSTS + "(id),"
                + "FOREIGN KEY(author_user_id) REFERENCES " + TABLE_USERS + "(id),"
                + "FOREIGN KEY(parent_comment_id) REFERENCES " + TABLE_POST_COMMENTS + "(id)"
                + ")");
    }

    private void createAdoptionInterestsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADOPTION_INTERESTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "post_id INTEGER NOT NULL,"
                + "interested_user_id INTEGER NOT NULL,"
                + "animal_name TEXT,"
                + "created_at_millis INTEGER NOT NULL,"
                + "UNIQUE(post_id, interested_user_id, animal_name)"
                + ")");
    }

    private void createNotificationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "recipient_user_id INTEGER NOT NULL,"
                + "notification_type TEXT NOT NULL,"
                + "title TEXT NOT NULL,"
                + "message_text TEXT NOT NULL,"
                + "related_post_id INTEGER,"
                + "related_post_type TEXT,"
                + "related_user_id INTEGER,"
                + "related_user_name TEXT,"
                + "related_user_email TEXT,"
                + "related_conversation_id INTEGER,"
                + "created_at_millis INTEGER NOT NULL,"
                + "is_read INTEGER NOT NULL DEFAULT 0"
                + ")");
    }

    private void createMessagesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MESSAGES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "conversation_id INTEGER NOT NULL,"
                + "sender_user_id INTEGER NOT NULL,"
                + "receiver_user_id INTEGER NOT NULL,"
                + "message_text TEXT NOT NULL,"
                + "sent_at_millis INTEGER NOT NULL,"
                + "is_read INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY(conversation_id) REFERENCES " + TABLE_CONVERSATIONS + "(id)"
                + ")");
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
        thorAnimal.put("age_months", 0);
        thorAnimal.put("weight_kg", 28.5);
        db.insert(TABLE_ANIMALS, null, thorAnimal);

        ContentValues lunaAnimal = new ContentValues();
        lunaAnimal.put("owner_user_id", clinicUserId);
        lunaAnimal.put("company_id", companyId);
        lunaAnimal.put("animal_name", "Luna");
        lunaAnimal.put("species", "Gato");
        lunaAnimal.put("breed", "Siames");
        lunaAnimal.put("age_years", 2);
        lunaAnimal.put("age_months", 0);
        lunaAnimal.put("weight_kg", 4.3);
        db.insert(TABLE_ANIMALS, null, lunaAnimal);

        removeSamplePosts(db);
    }

    private void removeSamplePosts(SQLiteDatabase db) {
        db.delete(
                TABLE_FAIR_POST_ANIMALS,
                "post_id IN (SELECT id FROM " + TABLE_POSTS + " WHERE animal_name IN (?, ?, ?))",
                new String[]{"Thor", "Luna", "Feira de Adocao de Sabado"}
        );
        db.delete(
                TABLE_POSTS,
                "animal_name IN (?, ?, ?)",
                new String[]{"Thor", "Luna", "Feira de Adocao de Sabado"}
        );
    }

    private void insertFairAnimal(
            SQLiteDatabase db,
            long postId,
            String animalName,
            String species,
            String breed,
            String ageDescription,
            String imageUri
    ) {
        ContentValues values = new ContentValues();
        values.put("post_id", postId);
        values.put("animal_name", animalName);
        values.put("species", species);
        values.put("breed", breed);
        values.put("age_description", ageDescription);
        values.put("image_uri", imageUri);
        db.insert(TABLE_FAIR_POST_ANIMALS, null, values);
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

