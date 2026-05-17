package com.petbook.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.models.User;
import com.petbook.app.repositories.ApiUserRepository;
import com.petbook.app.repositories.FirebaseUserDirectoryRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
import com.petbook.app.utils.ValidationUtils;

public class ProfileActivity extends AppCompatActivity {

    private EditText editProfileName;
    private EditText editProfileEmail;
    private Long userId;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        TextView textProfileType = findViewById(R.id.textProfileType);
        TextView textBack = findViewById(R.id.textBackProfile);
        TextView textLogout = findViewById(R.id.textLogoutProfile);
        Button buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserProfileStorage.getUserType(this, UserType.PERSON);
        }
        userId = UserProfileStorage.getUserId(this);

        String fallbackName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = getString(R.string.default_user_name);
        }

        String fallbackEmail = getIntent().getStringExtra(IntentKeys.EXTRA_USER_EMAIL);
        if (fallbackEmail == null || fallbackEmail.trim().isEmpty()) {
            fallbackEmail = fallbackName;
        }

        editProfileName.setText(UserProfileStorage.getName(this, fallbackName));
        editProfileEmail.setText(UserProfileStorage.getEmail(this, fallbackEmail));
        textProfileType.setText(UserType.isCompany(userType)
                ? R.string.profile_type_company
                : R.string.profile_type_person);

        if (FeatureFlags.useRemoteApi(this)) {
            loadProfileFromApi();
        }
        textBack.setOnClickListener(v -> finish());
        textLogout.setOnClickListener(v -> logout());
        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void loadProfileFromApi() {
        if (userId == null) {
            return;
        }

        AsyncRunner.run(
                () -> ApiUserRepository.findById(this, userId),
                user -> {
                    editProfileName.setText(user.getName());
                    editProfileEmail.setText(user.getEmail());
                    userType = user.getUserType();
                    UserProfileStorage.saveProfile(this, userId, user.getName(), user.getEmail(), userType);
                },
                exception -> {
                    // Mantem o fallback local quando o servidor nao responde.
                }
        );
    }

    private void saveProfile() {
        String name = editProfileName.getText().toString().trim();
        String email = editProfileEmail.getText().toString().trim();

        if (ValidationUtils.isEmpty(name)) {
            editProfileName.setError(getString(R.string.error_required_field));
            editProfileName.requestFocus();
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            editProfileEmail.setError(getString(R.string.error_invalid_email));
            editProfileEmail.requestFocus();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            boolean updated = UserRepository.updateProfile(this, userId, name, email);
            if (!updated) {
                Toast.makeText(this, R.string.error_profile_save_failed, Toast.LENGTH_LONG).show();
                return;
            }
            FirebaseUserDirectoryRepository.syncUser(
                    this,
                    new User(userId, userType, name, email, null, null, true)
            );
            UserProfileStorage.saveProfile(this, userId, name, email, userType);
            Toast.makeText(this, R.string.profile_save_success, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        AsyncRunner.run(
                () -> ApiUserRepository.updateProfile(this, userId, name, email),
                user -> {
                    FirebaseUserDirectoryRepository.syncUser(this, user);
                    UserProfileStorage.saveProfile(this, userId, user.getName(), user.getEmail(), user.getUserType());
                    Toast.makeText(this, R.string.profile_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                },
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_server_unavailable) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void logout() {
        UserProfileStorage.clearProfile(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

