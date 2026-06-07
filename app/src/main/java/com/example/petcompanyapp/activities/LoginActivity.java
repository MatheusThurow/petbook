package com.petbook.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.Button;
import android.widget.EditText;
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

import com.petbook.app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.petbook.app.models.User;
import com.petbook.app.repositories.ApiUserRepository;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.repositories.FirebaseUserDirectoryRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.SessionUtils;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.ValidationUtils;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String GOOGLE_WEB_CLIENT_ID_PLACEHOLDER = "ADD_YOUR_GOOGLE_WEB_CLIENT_ID";

    private EditText editEmail;
    private EditText editPassword;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionUtils.isAuthenticated(this)) {
            openExistingSession();
            return;
        }
        setContentView(R.layout.activity_login);

        credentialManager = CredentialManager.create(this);
        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseUserRepository.bootstrapLocalUsers(this, new FirebaseUserRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError(String message) {
                }
            });
        }
        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        Button buttonLogin = findViewById(R.id.buttonLogin);
        Button buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin);
        TextView textRegister = findViewById(R.id.textGoToRegister);
        TextView textForgotPassword = findViewById(R.id.textForgotPassword);

        buttonLogin.setOnClickListener(v -> validateLogin());
        buttonGoogleLogin.setOnClickListener(v -> signInWithGoogle());
        textForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );
        textRegister.setOnClickListener(v -> openUserRegister());
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

        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseUserRepository.authenticate(this, email, password, new FirebaseUserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    runOnUiThread(() -> openFeed(user));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(
                            LoginActivity.this,
                            message == null ? getString(R.string.error_invalid_login) : message,
                            Toast.LENGTH_SHORT
                    ).show());
                }
            });
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
        FirebaseUserDirectoryRepository.syncUser(this, authenticatedUser);
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
        SessionUtils.openMainFlow(this, intent);
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
        String idToken = googleCredential.getIdToken();
        String email = googleCredential.getId();
        String displayName = googleCredential.getDisplayName();
        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                    .addOnSuccessListener(authResult ->
                            FirebaseUserRepository.findByEmail(this, email, new FirebaseUserRepository.UserCallback() {
                                @Override
                                public void onSuccess(User user) {
                                    runOnUiThread(() -> openFeedWithGoogleSuccess(user));
                                }

                                @Override
                                public void onError(String message) {
                                    if (message != null && message.toLowerCase().contains("nao encontrado")) {
                                        runOnUiThread(() -> openGoogleRegistrationCompletion(displayName, email));
                                        return;
                                    }
                                    runOnUiThread(() -> Toast.makeText(
                                            LoginActivity.this,
                                            message == null ? getString(R.string.error_google_sign_in_failed) : message,
                                            Toast.LENGTH_LONG
                                    ).show());
                                }
                            })
                    )
                    .addOnFailureListener(exception ->
                            runOnUiThread(() -> Toast.makeText(
                                    LoginActivity.this,
                                    exception.getMessage() == null ? getString(R.string.error_google_sign_in_failed) : exception.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show())
                    );
            return;
        }

        User user = UserRepository.findByEmail(this, email);
        if (user == null) {
            runOnUiThread(() -> Toast.makeText(
                    this,
                    R.string.error_google_account_not_registered,
                    Toast.LENGTH_LONG
            ).show());
            return;
        }

        runOnUiThread(() -> {
            FirebaseUserDirectoryRepository.syncUser(this, user);
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
            SessionUtils.openMainFlow(this, intent);
        });
    }

    private void openUserRegister() {
        startActivity(new Intent(this, UserRegisterActivity.class));
    }

    private void openGoogleRegistrationCompletion(String displayName, String email) {
        Intent intent = new Intent(this, UserRegisterActivity.class);
        intent.putExtra(IntentKeys.EXTRA_GOOGLE_SIGNUP_FLOW, true);
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, displayName);
        intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, email);
        startActivity(intent);
    }

    private void openFeedWithGoogleSuccess(User user) {
        FirebaseUserDirectoryRepository.syncUser(this, user);
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
        SessionUtils.openMainFlow(this, intent);
    }

    private void openExistingSession() {
        Intent intent = new Intent(this, FeedActivity.class);
        Long userId = UserProfileStorage.getUserId(this);
        if (userId != null) {
            intent.putExtra(IntentKeys.EXTRA_USER_ID, userId);
        }
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, UserProfileStorage.getUserType(this, ""));
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, UserProfileStorage.getName(this, getString(R.string.default_user_name)));
        intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, UserProfileStorage.getEmail(this, ""));
        SessionUtils.openMainFlow(this, intent);
    }
}

