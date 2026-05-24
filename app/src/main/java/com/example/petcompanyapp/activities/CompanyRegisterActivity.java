package com.petbook.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.models.CompanyProfile;
import com.petbook.app.repositories.ApiCompanyRepository;
import com.petbook.app.repositories.CompanyRepository;
import com.petbook.app.repositories.FirebaseCompanyRepository;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.MaskUtils;
import com.petbook.app.utils.UserType;
import com.petbook.app.utils.ValidationUtils;

public class CompanyRegisterActivity extends AppCompatActivity {

    private EditText editCompanyName;
    private EditText editCnpj;
    private EditText editAddress;
    private EditText editPhone;
    private TextView textCompanyRegisterTitle;
    private TextView textCompanyRegisterSubtitle;
    private Button buttonSave;
    private String userType;
    private Long userId;
    private String userName;
    private String userEmail;
    private boolean editingExistingCompany;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_register);

        editCompanyName = findViewById(R.id.editCompanyName);
        editCnpj = findViewById(R.id.editCompanyCnpj);
        editAddress = findViewById(R.id.editCompanyAddress);
        editPhone = findViewById(R.id.editCompanyPhone);
        textCompanyRegisterTitle = findViewById(R.id.textCompanyRegisterTitle);
        textCompanyRegisterSubtitle = findViewById(R.id.textCompanyRegisterSubtitle);
        TextView textBack = findViewById(R.id.textBackCompanyRegister);
        buttonSave = findViewById(R.id.buttonCompanySave);

        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserType.COMPANY;
        }
        long extraUserId = getIntent().getLongExtra(IntentKeys.EXTRA_USER_ID, -1L);
        userId = extraUserId >= 0 ? extraUserId : null;
        userName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        userEmail = getIntent().getStringExtra(IntentKeys.EXTRA_USER_EMAIL);

        MaskUtils.applyCnpjMask(editCnpj);
        MaskUtils.applyPhoneMask(editPhone);
        loadExistingCompany();

        textBack.setOnClickListener(v -> finish());
        buttonSave.setOnClickListener(v -> saveCompany());
    }

    private void loadExistingCompany() {
        if (userId == null) {
            return;
        }

        if (FirebaseChatConfig.isEnabled(this)) {
            FirebaseCompanyRepository.findByOwnerUserId(this, userId, new FirebaseCompanyRepository.CompanyProfileCallback() {
                @Override
                public void onSuccess(CompanyProfile companyProfile) {
                    runOnUiThread(() -> bindExistingCompany(companyProfile));
                }

                @Override
                public void onEmpty() {
                }

                @Override
                public void onError(String message) {
                }
            });
            return;
        }

        CompanyProfile companyProfile = CompanyRepository.findByOwnerUserId(this, userId);
        if (companyProfile != null) {
            bindExistingCompany(companyProfile);
        }
    }

    private void bindExistingCompany(CompanyProfile companyProfile) {
        if (companyProfile == null) {
            return;
        }

        editingExistingCompany = true;
        editCompanyName.setText(companyProfile.getCompanyName());
        editCnpj.setText(companyProfile.getCnpj());
        editAddress.setText(companyProfile.getAddress());
        editPhone.setText(companyProfile.getPhone());
        textCompanyRegisterTitle.setText(R.string.company_register_edit_title);
        textCompanyRegisterSubtitle.setText(R.string.company_register_edit_subtitle);
        buttonSave.setText(R.string.button_save_post_changes);
    }

    private void saveCompany() {
        String companyName = editCompanyName.getText().toString().trim();
        String cnpjDigits = editCnpj.getText().toString().replaceAll("\\D", "");
        String address = editAddress.getText().toString().trim();
        String phoneDigits = editPhone.getText().toString().replaceAll("\\D", "");

        if (ValidationUtils.isEmpty(companyName)) {
            editCompanyName.setError(getString(R.string.error_required_field));
            editCompanyName.requestFocus();
            return;
        }

        if (cnpjDigits.length() != 14) {
            editCnpj.setError(getString(R.string.error_invalid_cnpj));
            editCnpj.requestFocus();
            return;
        }

        if (ValidationUtils.isEmpty(address)) {
            editAddress.setError(getString(R.string.error_required_field));
            editAddress.requestFocus();
            return;
        }

        if (phoneDigits.length() < 10) {
            editPhone.setError(getString(R.string.error_invalid_phone));
            editPhone.requestFocus();
            return;
        }

        if (userId == null) {
            Toast.makeText(this, R.string.error_post_user_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        buttonSave.setEnabled(false);

        if (FirebaseChatConfig.isEnabled(this)) {
            FirebaseCompanyRepository.saveCompany(
                    this,
                    userId,
                    companyName,
                    editCnpj.getText().toString().trim(),
                    address,
                    editPhone.getText().toString().trim(),
                    new FirebaseCompanyRepository.CompanyIdCallback() {
                        @Override
                        public void onSuccess(long companyId) {
                            runOnUiThread(() -> openFeed(companyName));
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                buttonSave.setEnabled(true);
                                Toast.makeText(
                                        CompanyRegisterActivity.this,
                                        message == null ? getString(R.string.error_company_save_failed) : message,
                                        Toast.LENGTH_LONG
                                ).show();
                            });
                        }
                    }
            );
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            long companyId = CompanyRepository.saveCompany(
                    this,
                    userId,
                    companyName,
                    editCnpj.getText().toString().trim(),
                    address,
                    editPhone.getText().toString().trim()
            );
            if (companyId < 0) {
                buttonSave.setEnabled(true);
                Toast.makeText(this, R.string.error_company_save_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            openFeed(companyName);
            return;
        }

        AsyncRunner.run(
                () -> ApiCompanyRepository.saveCompany(
                        this,
                        userId,
                        companyName,
                        editCnpj.getText().toString().trim(),
                        address,
                        editPhone.getText().toString().trim()
                ),
                ignored -> openFeed(companyName),
                exception -> {
                    buttonSave.setEnabled(true);
                    Toast.makeText(
                            this,
                            exception.getMessage() == null ? getString(R.string.error_company_save_failed) : exception.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                }
        );
    }

    private void openFeed(String companyName) {
        buttonSave.setEnabled(true);
        Toast.makeText(this, editingExistingCompany ? R.string.company_update_success : R.string.company_register_success, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, FeedActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_ID, userId.longValue());
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, UserType.COMPANY);
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName != null ? userName : companyName);
        if (userEmail != null) {
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
        }
        startActivity(intent);
        finish();
    }
}

