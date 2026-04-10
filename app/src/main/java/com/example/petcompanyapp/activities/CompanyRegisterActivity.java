package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.MaskUtils;
import com.example.petcompanyapp.utils.UserType;
import com.example.petcompanyapp.utils.ValidationUtils;

public class CompanyRegisterActivity extends AppCompatActivity {

    private EditText editCompanyName;
    private EditText editCnpj;
    private EditText editAddress;
    private EditText editPhone;
    private String userType;
    private Long userId;
    private String userName;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_register);

        editCompanyName = findViewById(R.id.editCompanyName);
        editCnpj = findViewById(R.id.editCompanyCnpj);
        editAddress = findViewById(R.id.editCompanyAddress);
        editPhone = findViewById(R.id.editCompanyPhone);
        TextView textCompanySubtitle = findViewById(R.id.textCompanyRegisterSubtitle);
        Button buttonSave = findViewById(R.id.buttonCompanySave);

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

        if (UserType.isCompany(userType)) {
            textCompanySubtitle.setText(R.string.company_register_subtitle_company);
        } else {
            textCompanySubtitle.setText(R.string.company_register_subtitle_person);
        }

        buttonSave.setOnClickListener(v -> saveCompany());
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

        Toast.makeText(this, R.string.company_register_success, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, FeedActivity.class);
        if (userId != null) {
            intent.putExtra(IntentKeys.EXTRA_USER_ID, userId.longValue());
        }
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, UserType.COMPANY);
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName != null ? userName : companyName);
        if (userEmail != null) {
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
        }
        startActivity(intent);
        finish();
    }
}
