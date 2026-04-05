package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.UserType;

public class FeedActivity extends AppCompatActivity {

    private String userType;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserType.PERSON;
        }

        userName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (userName == null || userName.trim().isEmpty()) {
            userName = getString(R.string.default_user_name);
        }

        TextView textFeedTitle = findViewById(R.id.textFeedTitle);
        TextView textFeedSubtitle = findViewById(R.id.textFeedSubtitle);
        TextView textAudience = findViewById(R.id.textAudience);
        TextView textCardOneBody = findViewById(R.id.textCardOneBody);
        TextView textCardTwoBody = findViewById(R.id.textCardTwoBody);
        TextView textCardThreeBody = findViewById(R.id.textCardThreeBody);
        Button buttonGoAnimal = findViewById(R.id.buttonGoAnimalRegister);
        Button buttonGoCompany = findViewById(R.id.buttonGoCompanyRegister);

        textFeedTitle.setText(getString(R.string.feed_title, userName));
        textFeedSubtitle.setText(UserType.isCompany(userType)
                ? getString(R.string.feed_subtitle_company)
                : getString(R.string.feed_subtitle_person));
        textAudience.setText(UserType.isCompany(userType)
                ? getString(R.string.feed_audience_company)
                : getString(R.string.feed_audience_person));

        if (UserType.isCompany(userType)) {
            textCardOneBody.setText(R.string.feed_company_card_one);
            textCardTwoBody.setText(R.string.feed_company_card_two);
            textCardThreeBody.setText(R.string.feed_common_card_three);
            buttonGoCompany.setText(R.string.button_edit_company);
        } else {
            textCardOneBody.setText(R.string.feed_person_card_one);
            textCardTwoBody.setText(R.string.feed_person_card_two);
            textCardThreeBody.setText(R.string.feed_common_card_three);
            buttonGoCompany.setText(R.string.button_register_company);
        }

        buttonGoAnimal.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalRegisterActivity.class);
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
            startActivity(intent);
        });

        buttonGoCompany.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompanyRegisterActivity.class);
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
            startActivity(intent);
        });
    }
}
