package com.example.petcompanyapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

public class ProfileActivity extends AppCompatActivity {

    private EditText editProfileName;
    private EditText editProfileEmail;
    private String userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editProfileName = findViewById(R.id.editProfileName);
        editProfileEmail = findViewById(R.id.editProfileEmail);
        TextView textProfileType = findViewById(R.id.textProfileType);
        Button buttonSaveProfile = findViewById(R.id.buttonSaveProfile);

        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserType.PERSON;
        }

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

        buttonSaveProfile.setOnClickListener(v -> saveProfile());
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

        UserProfileStorage.saveProfile(this, name, email, userType);
        Toast.makeText(this, R.string.profile_save_success, Toast.LENGTH_SHORT).show();
        finish();
    }
}
