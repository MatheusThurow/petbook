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
import com.example.petcompanyapp.repositories.ApiUserRepository;
import com.example.petcompanyapp.repositories.UserRepository;
import com.google.android.material.textfield.TextInputLayout;
import com.example.petcompanyapp.utils.AsyncRunner;
import com.example.petcompanyapp.utils.FeatureFlags;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

public class UserRegisterActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private EditText editDocument;
    private TextView textUserRegisterSubtitle;
    private RadioGroup radioGroupUserType;
    private TextInputLayout inputLayoutUserName;
    private TextInputLayout inputLayoutUserDocument;
    private String selectedUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_register);

        editName = findViewById(R.id.editUserName);
        editEmail = findViewById(R.id.editUserEmail);
        editPassword = findViewById(R.id.editUserPassword);
        editConfirmPassword = findViewById(R.id.editUserConfirmPassword);
        editDocument = findViewById(R.id.editUserDocument);
        textUserRegisterSubtitle = findViewById(R.id.textUserRegisterSubtitle);
        radioGroupUserType = findViewById(R.id.radioGroupUserType);
        inputLayoutUserName = findViewById(R.id.inputLayoutUserName);
        inputLayoutUserDocument = findViewById(R.id.inputLayoutUserDocument);
        TextView textBack = findViewById(R.id.textBackUserRegister);
        Button buttonRegister = findViewById(R.id.buttonUserRegister);

        selectedUserType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (selectedUserType == null) {
            selectedUserType = UserType.PERSON;
        }

        radioGroupUserType.check(UserType.isCompany(selectedUserType) ? R.id.radioUserTypeCompany : R.id.radioUserTypePerson);
        updateFormByUserType(selectedUserType);
        MaskUtils.applyCpfOrCnpjMask(editDocument, UserType.isCompany(selectedUserType));

        radioGroupUserType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedUserType = checkedId == R.id.radioUserTypeCompany ? UserType.COMPANY : UserType.PERSON;
            editDocument.setText("");
            MaskUtils.applyCpfOrCnpjMask(editDocument, UserType.isCompany(selectedUserType));
            updateFormByUserType(selectedUserType);
        });

        textBack.setOnClickListener(v -> finish());
        buttonRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        String document = editDocument.getText().toString().replaceAll("\\D", "");

        if (ValidationUtils.isEmpty(name)) {
            editName.setError(getString(R.string.error_required_field));
            editName.requestFocus();
            return;
        }

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

        if (!password.equals(confirmPassword)) {
            editConfirmPassword.setError(getString(R.string.error_password_match));
            editConfirmPassword.requestFocus();
            return;
        }

        if (UserType.isCompany(selectedUserType) && document.length() != 14) {
            editDocument.setError(getString(R.string.error_invalid_cnpj));
            editDocument.requestFocus();
            return;
        }

        if (!UserType.isCompany(selectedUserType) && document.length() != 11) {
            editDocument.setError(getString(R.string.error_invalid_cpf));
            editDocument.requestFocus();
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            User registeredUser = UserRepository.register(this, selectedUserType, name, email, password, document);
            if (registeredUser == null) {
                editEmail.setError(getString(R.string.error_email_already_registered));
                editEmail.requestFocus();
                return;
            }
            completeRegistration(registeredUser);
            return;
        }

        AsyncRunner.run(
                () -> ApiUserRepository.register(this, selectedUserType, name, email, password, document),
                this::completeRegistration,
                exception -> {
                    String message = exception.getMessage();
                    if (message != null && message.toLowerCase().contains("email")) {
                        editEmail.setError(getString(R.string.error_email_already_registered));
                        editEmail.requestFocus();
                        return;
                    }

                    Toast.makeText(
                            this,
                            message == null ? getString(R.string.error_server_unavailable) : message,
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void completeRegistration(User registeredUser) {
        Toast.makeText(this, R.string.user_register_success, Toast.LENGTH_SHORT).show();
        Intent intent;
        if (UserType.isCompany(selectedUserType)) {
            intent = new Intent(this, CompanyRegisterActivity.class);
        } else {
            intent = new Intent(this, FeedActivity.class);
        }
        UserProfileStorage.saveProfile(
                this,
                registeredUser.getId(),
                registeredUser.getName(),
                registeredUser.getEmail(),
                registeredUser.getUserType()
        );
        intent.putExtra(IntentKeys.EXTRA_USER_ID, registeredUser.getId().longValue());
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, registeredUser.getUserType());
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, registeredUser.getName());
        intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, registeredUser.getEmail());
        startActivity(intent);
        finish();
    }

    private void updateFormByUserType(String userType) {
        if (UserType.isCompany(userType)) {
            inputLayoutUserName.setHint(getString(R.string.hint_company_responsible_name));
            inputLayoutUserDocument.setHint(getString(R.string.hint_cnpj));
            textUserRegisterSubtitle.setText(R.string.user_register_subtitle_company);
        } else {
            inputLayoutUserName.setHint(getString(R.string.hint_name));
            inputLayoutUserDocument.setHint(getString(R.string.hint_cpf));
            textUserRegisterSubtitle.setText(R.string.user_register_subtitle_person);
        }
    }
}
