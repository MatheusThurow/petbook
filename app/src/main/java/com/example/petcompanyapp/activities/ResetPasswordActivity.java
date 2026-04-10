package com.example.petcompanyapp.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        MaterialButton buttonResetPassword = findViewById(R.id.buttonResetPassword);

        buttonResetPassword.setOnClickListener(v -> validateReset());
    }

    private void validateReset() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

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
            editConfirmPassword.setError("As senhas não coincidem");
            editConfirmPassword.requestFocus();
            return;
        }

        boolean success = UserRepository.resetPassword(email, password);

        if (!success) {
            Toast.makeText(this, "Erro ao redefinir senha", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Senha redefinida com sucesso", Toast.LENGTH_SHORT).show();
        finish();
    }
}