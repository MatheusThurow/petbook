package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.petcompanyapp.R;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.example.petcompanyapp.models.User;
import com.example.petcompanyapp.repositories.ApiUserRepository;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.AsyncRunner;
import com.example.petcompanyapp.utils.FeatureFlags;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String GOOGLE_WEB_CLIENT_ID_PLACEHOLDER = "ADD_YOUR_GOOGLE_WEB_CLIENT_ID";

    private EditText editEmail;
    private EditText editPassword;
    private RadioGroup radioGroupAccessType;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        credentialManager = CredentialManager.create(this);
        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        radioGroupAccessType = findViewById(R.id.radioGroupAccessType);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        TextView textRegister = findViewById(R.id.textGoToRegister);
        TextView textForgotPassword = findViewById(R.id.textForgotPassword);

        buttonLogin.setOnClickListener(v -> validateLogin());
        buttonGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        textForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        textRegister.setOnClickListener(v -> openUserRegister(getSelectedUserType()));
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

        if (!FeatureFlags.useRemoteApi(this)) {
            User authenticatedUser = UserRepository.authenticate(this, email, password);
            if (authenticatedUser == null) {
                Toast.makeText(this, R.string.error_invalid_login, Toast.LENGTH_SHORT).show();
                return;
            }
            openFeed(authenticatedUser);
            return;
        }

        AsyncRunner.run(
                () -> ApiUserRepository.login(this, email, password),
                this::openFeed,
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_server_unavailable) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void openFeed(User authenticatedUser) {
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

    private void signInWithGoogle() {
        String webClientId = getString(R.string.google_web_client_id).trim();
        if (webClientId.isEmpty() || GOOGLE_WEB_CLIENT_ID_PLACEHOLDER.equals(webClientId)) {
            Toast.makeText(this, R.string.error_google_sign_in_unavailable, Toast.LENGTH_LONG).show();
            return;
        }

        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(webClientId).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleSignInResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> Toast.makeText(
                                LoginActivity.this,
                                R.string.error_google_sign_in_failed,
                                Toast.LENGTH_LONG
                        ).show());
                    }
                }
        );
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        Credential credential = response.getCredential();
        if (!(credential instanceof CustomCredential)) {
            runOnUiThread(() -> Toast.makeText(
                    this,
                    R.string.error_google_sign_in_failed,
                    Toast.LENGTH_LONG
            ).show());
            return;
        }

        CustomCredential customCredential = (CustomCredential) credential;
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
            runOnUiThread(() -> Toast.makeText(
                    this,
                    R.string.error_google_sign_in_failed,
                    Toast.LENGTH_LONG
            ).show());
            return;
        }

        GoogleIdTokenCredential googleCredential =
                GoogleIdTokenCredential.createFrom(customCredential.getData());
        String email = googleCredential.getId();
        String displayName = googleCredential.getDisplayName();
        User user = UserRepository.findOrCreateGoogleUser(
                this,
                getSelectedUserType(),
                displayName,
                email
        );

        if (user == null) {
            runOnUiThread(() -> Toast.makeText(
                    this,
                    R.string.error_google_sign_in_failed,
                    Toast.LENGTH_LONG
            ).show());
            return;
        }

        runOnUiThread(() -> {
            Toast.makeText(this, R.string.login_google_success, Toast.LENGTH_SHORT).show();
            UserProfileStorage.saveProfile(
                    this,
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getUserType()
            );

            Intent intent = new Intent(this, FeedActivity.class);
            intent.putExtra(IntentKeys.EXTRA_USER_ID, user.getId().longValue());
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, user.getUserType());
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, user.getName());
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, user.getEmail());
            startActivity(intent);
        });
    }

    private void openUserRegister(String userType) {
        Intent intent = new Intent(this, UserRegisterActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
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
