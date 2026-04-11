package com.example.petcompanyapp.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.ValidationUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editEmail;
    private EditText editDocument;
    private EditText editNewPassword;
    private EditText editConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        editEmail = findViewById(R.id.editForgotPasswordEmail);
        editDocument = findViewById(R.id.editForgotPasswordDocument);
        editNewPassword = findViewById(R.id.editForgotPasswordNewPassword);
        editConfirmPassword = findViewById(R.id.editForgotPasswordConfirmPassword);
        TextView textBack = findViewById(R.id.textBackForgotPassword);
        Button buttonResetPassword = findViewById(R.id.buttonResetPassword);

        MaskUtils.applyCpfOrCnpjAutoMask(editDocument);
        textBack.setOnClickListener(v -> finish());
        buttonResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = editEmail.getText().toString().trim();
        String document = editDocument.getText().toString().replaceAll("\\D", "");
        String newPassword = editNewPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (!ValidationUtils.isValidEmail(email)) {
            editEmail.setError(getString(R.string.error_invalid_email));
            editEmail.requestFocus();
            return;
        }

        if (document.length() != 11 && document.length() != 14) {
            editDocument.setError(getString(R.string.error_invalid_document));
            editDocument.requestFocus();
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

        boolean updated = UserRepository.resetPassword(this, email, document, newPassword);
        if (!updated) {
            Toast.makeText(this, R.string.error_forgot_password_user_not_found, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, R.string.forgot_password_success, Toast.LENGTH_LONG).show();
        finish();
    }
}
