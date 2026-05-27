package com.petbook.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.models.User;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.petbook.app.repositories.ApiUserRepository;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.repositories.FirebaseUserDirectoryRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.BottomNavigationHelper;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.SwipeNavigationHelper;
import com.petbook.app.utils.ThemePreferenceManager;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
import com.petbook.app.utils.ValidationUtils;

public class ProfileActivity extends AppCompatActivity {

    private EditText editProfileName;
    private EditText editProfileEmail;
    private Long userId;
    private String userType;
    private SwipeNavigationHelper swipeNavigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        TextView textLogout = findViewById(R.id.textLogoutProfile);
        Button buttonOpenChangePassword = findViewById(R.id.buttonOpenChangePassword);
        Button buttonSaveProfile = findViewById(R.id.buttonSaveProfile);
        MaterialSwitch switchDarkMode = findViewById(R.id.switchDarkMode);

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
        BottomNavigationHelper.bind(this, BottomNavigationHelper.DESTINATION_PROFILE);
        swipeNavigationHelper = new SwipeNavigationHelper(this, BottomNavigationHelper.DESTINATION_PROFILE);
        switchDarkMode.setChecked(ThemePreferenceManager.isDarkModeEnabled(this));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                ThemePreferenceManager.setDarkModeEnabled(this, isChecked)
        );

        if (FirebaseUserRepository.isEnabled(this)) {
            loadProfileFromFirebase();
        } else if (FeatureFlags.useRemoteApi(this)) {
            loadProfileFromApi();
        }
        textLogout.setOnClickListener(v -> logout());
        buttonOpenChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePasswordActivity.class))
        );
        buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (swipeNavigationHelper != null) {
            swipeNavigationHelper.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.refreshNotificationBadge(this);
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

    private void loadProfileFromFirebase() {
        if (userId == null) {
            return;
        }

        FirebaseUserRepository.findById(this, userId, new FirebaseUserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    editProfileName.setText(user.getName());
                    editProfileEmail.setText(user.getEmail());
                    userType = user.getUserType();
                    UserProfileStorage.saveProfile(ProfileActivity.this, userId, user.getName(), user.getEmail(), userType);
                });
            }

            @Override
            public void onError(String message) {
            }
        });
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

        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseUserRepository.updateProfile(this, userId, name, email, new FirebaseUserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    runOnUiThread(() -> {
                        FirebaseUserDirectoryRepository.syncUser(ProfileActivity.this, user);
                        UserProfileStorage.saveProfile(ProfileActivity.this, userId, user.getName(), user.getEmail(), user.getUserType());
                        Toast.makeText(ProfileActivity.this, R.string.profile_save_success, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(
                            ProfileActivity.this,
                            message == null ? getString(R.string.error_profile_save_failed) : message,
                            Toast.LENGTH_LONG
                    ).show());
                }
            });
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

