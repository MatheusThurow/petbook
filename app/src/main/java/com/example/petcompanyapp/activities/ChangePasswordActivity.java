package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.models.User;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.ValidationUtils;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText editCurrentPassword;
    private EditText editNewPassword;
    private EditText editConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        editCurrentPassword = findViewById(R.id.editCurrentPassword);
        editNewPassword = findViewById(R.id.editNewPassword);
        editConfirmPassword = findViewById(R.id.editConfirmNewPassword);
        TextView textBack = findViewById(R.id.textBackChangePassword);
        Button buttonSavePassword = findViewById(R.id.buttonSavePassword);

        textBack.setOnClickListener(v -> finish());
        buttonSavePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String currentPassword = editCurrentPassword.getText().toString().trim();
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        String email = UserProfileStorage.getEmail(this, "");

        if (!ValidationUtils.hasMinLength(currentPassword, 6)) {
            editCurrentPassword.setError(getString(R.string.error_current_password_required));
            editCurrentPassword.requestFocus();
            return;
        }

        if (!ValidationUtils.hasMinLength(newPassword, 6)) {
            editNewPassword.setError(getString(R.string.error_password_length));
            editNewPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            editConfirmPassword.setError(getString(R.string.error_password_match));
            editConfirmPassword.requestFocus();
            return;
        }

        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseUserRepository.changePassword(this, email, currentPassword, newPassword, new FirebaseUserRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangePasswordActivity.this, R.string.change_password_success, Toast.LENGTH_LONG).show();
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(
                            ChangePasswordActivity.this,
                            message == null ? getString(R.string.error_change_password_failed) : message,
                            Toast.LENGTH_LONG
                    ).show());
                }
            });
            return;
        }

        User currentUser = UserRepository.authenticate(this, email, currentPassword);
        if (currentUser == null) {
            editCurrentPassword.setError(getString(R.string.error_invalid_current_password));
            editCurrentPassword.requestFocus();
            return;
        }

        boolean updated = UserRepository.resetPassword(this, email, currentUser.getDocument(), newPassword);
        if (!updated) {
            Toast.makeText(this, R.string.error_change_password_failed, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, R.string.change_password_success, Toast.LENGTH_LONG).show();
        finish();
    }
}
