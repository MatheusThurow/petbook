package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.models.User;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editPassword;
    private RadioGroup radioGroupAccessType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        radioGroupAccessType = findViewById(R.id.radioGroupAccessType);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        TextView textRegisterUser = findViewById(R.id.textGoToRegisterUser);
        TextView textRegisterCompany = findViewById(R.id.textGoToRegisterCompany);
        TextView textForgotPassword = findViewById(R.id.textForgotPassword);

        buttonLogin.setOnClickListener(v -> validateLogin());

        textForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });

        textRegisterUser.setOnClickListener(v ->
                openUserRegister(UserType.PERSON)
        );

        textRegisterCompany.setOnClickListener(v -> openCompanyRegister());
    }

    private void validateLogin() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            editEmail.setError(getString(R.string.error_invalid_email));
            editEmail.requestFocus();
            return;
        }

        if (!ValidationUtils.hasMinLength(password, 6)) {
            editPassword.setError(getString(R.string.error_password_length));
            editPassword.requestFocus();
            return;
        }

        User authenticatedUser = UserRepository.authenticate(email, password, getSelectedUserType());
        if (authenticatedUser == null) {
            Toast.makeText(this, R.string.error_invalid_login, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
        UserProfileStorage.saveProfile(
                this,
                authenticatedUser.getId(),
                authenticatedUser.getName(),
                authenticatedUser.getEmail(),
                authenticatedUser.getUserType()
        );

        Intent intent = new Intent(this, FeedActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_ID, authenticatedUser.getId().longValue());
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, authenticatedUser.getUserType());
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, authenticatedUser.getName());
        intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, authenticatedUser.getEmail());
        startActivity(intent);
    }

    private void openUserRegister(String userType) {
        Intent intent = new Intent(this, UserRegisterActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
        startActivity(intent);
    }

    private void openCompanyRegister() {
        Intent intent = new Intent(this, CompanyRegisterActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, UserType.COMPANY);
        startActivity(intent);
    }

    private String getSelectedUserType() {
        int checkedId = radioGroupAccessType.getCheckedRadioButtonId();
        if (checkedId == R.id.radioCompany) {
            return UserType.COMPANY;
        }
        return UserType.PERSON;
    }
}