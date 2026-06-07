package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.utils.ActionStateHelper;
import com.petbook.app.utils.BackNavigationUtils;
import com.petbook.app.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editConfirmEmail;
    private Button buttonResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editEmail = findViewById(R.id.editForgotPasswordEmail);
        editConfirmEmail = findViewById(R.id.editForgotPasswordConfirmEmail);
        TextView textBack = findViewById(R.id.textBackForgotPassword);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);

        BackNavigationUtils.bind(this, textBack);
        buttonResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = editEmail.getText().toString().trim();
        String confirmEmail = editConfirmEmail.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            editEmail.setError(getString(R.string.error_invalid_email));
            editEmail.requestFocus();
            return;
        }

        if (!email.equalsIgnoreCase(confirmEmail)) {
            editConfirmEmail.setError(getString(R.string.error_email_match));
            editConfirmEmail.requestFocus();
            return;
        }

        setResetLoading(true);

        if (FirebaseUserRepository.isEnabled(this)) {
            FirebaseUserRepository.sendPasswordResetEmail(this, email, new FirebaseUserRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        setResetLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this, R.string.forgot_password_email_sent_success, Toast.LENGTH_LONG).show();
                        BackNavigationUtils.navigateBack(ForgotPasswordActivity.this);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        setResetLoading(false);
                        Toast.makeText(
                                ForgotPasswordActivity.this,
                                message == null ? getString(R.string.error_forgot_password_user_not_found) : message,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            });
            return;
        }

        setResetLoading(false);
        Toast.makeText(this, R.string.error_password_reset_email_unavailable, Toast.LENGTH_LONG).show();
    }

    private void setResetLoading(boolean isLoading) {
        ActionStateHelper.setEnabled(!isLoading, editEmail, editConfirmEmail);
        ActionStateHelper.setLoading(
                buttonResetPassword,
                isLoading,
                getString(R.string.forgot_password_send_button),
                getString(R.string.action_processing)
        );
    }
}

