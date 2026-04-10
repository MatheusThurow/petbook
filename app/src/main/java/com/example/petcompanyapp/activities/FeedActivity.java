package com.example.petcompanyapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.petcompanyapp.R;
import com.example.petcompanyapp.adapters.AnimalPostAdapter;
import com.example.petcompanyapp.models.AnimalPost;
import com.example.petcompanyapp.models.User;
import com.example.petcompanyapp.repositories.AnimalPostRepository;
import com.example.petcompanyapp.repositories.UserRepository;
import com.example.petcompanyapp.utils.FeedFilter;
import com.example.petcompanyapp.utils.IntentKeys;
import com.example.petcompanyapp.utils.UserProfileStorage;
import com.example.petcompanyapp.utils.UserType;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class FeedActivity extends AppCompatActivity implements AnimalPostAdapter.OnPostActionListener {

    private Long userId;
    private String userType;
    private String userName;
    private String userEmail;
    private String selectedFilter = FeedFilter.ALL;
    private AnimalPostAdapter animalPostAdapter;
    private TextView textEmptyFeed;
    private TextView textFeedTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        long extraUserId = getIntent().getLongExtra(IntentKeys.EXTRA_USER_ID, -1L);
        userId = extraUserId >= 0 ? extraUserId : UserProfileStorage.getUserId(this);
        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserType.PERSON;
        }

        userName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (userName == null || userName.trim().isEmpty()) {
            userName = getString(R.string.default_user_name);
        }
        userEmail = getIntent().getStringExtra(IntentKeys.EXTRA_USER_EMAIL);
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = userName;
        }
        User activeUser = UserRepository.findById(userId);
        if (activeUser != null) {
            userName = activeUser.getName();
            userEmail = activeUser.getEmail();
            userType = activeUser.getUserType();
        }
        UserProfileStorage.saveProfile(this, userId, userName, userEmail, userType);

        textFeedTitle = findViewById(R.id.textFeedTitle);
        Button buttonCreatePost = findViewById(R.id.buttonCreatePost);
        Button buttonGoAnimal = findViewById(R.id.buttonGoAnimalRegister);
        Button buttonGoCompany = findViewById(R.id.buttonGoCompanyRegister);
        ImageButton buttonProfile = findViewById(R.id.buttonProfile);
        MaterialButtonToggleGroup toggleGroupFeedFilter = findViewById(R.id.toggleGroupFeedFilter);
        RecyclerView recyclerFeedPosts = findViewById(R.id.recyclerFeedPosts);
        textEmptyFeed = findViewById(R.id.textEmptyFeed);

        bindProfileHeader();
        buttonGoCompany.setText(UserType.isCompany(userType)
                ? R.string.button_edit_company
                : R.string.button_register_company);

        animalPostAdapter = new AnimalPostAdapter(this);
        recyclerFeedPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeedPosts.setAdapter(animalPostAdapter);

        buttonCreatePost.setOnClickListener(v ->
                {
                    Intent intent = new Intent(this, PostCreateActivity.class);
                    if (userId != null) {
                        intent.putExtra(IntentKeys.EXTRA_USER_ID, userId.longValue());
                    }
                    intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
                    intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
                    intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
                    startActivity(intent);
                }
        );

        buttonGoAnimal.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnimalRegisterActivity.class);
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
            startActivity(intent);
        });

        buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
            startActivity(intent);
        });

        buttonGoCompany.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompanyRegisterActivity.class);
            if (userId != null) {
                intent.putExtra(IntentKeys.EXTRA_USER_ID, userId.longValue());
            }
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
            startActivity(intent);
        });

        toggleGroupFeedFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.buttonFilterLost) {
                selectedFilter = FeedFilter.LOST;
            } else if (checkedId == R.id.buttonFilterAdoption) {
                selectedFilter = FeedFilter.ADOPTION;
            } else {
                selectedFilter = FeedFilter.ALL;
            }
            loadFeedPosts();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        userName = UserProfileStorage.getName(this, userName);
        userEmail = UserProfileStorage.getEmail(this, userEmail);
        userId = UserProfileStorage.getUserId(this);
        bindProfileHeader();
        loadFeedPosts();
    }

    private void loadFeedPosts() {
        java.util.List<com.example.petcompanyapp.models.AnimalPost> posts =
                AnimalPostRepository.getPosts(selectedFilter);
        animalPostAdapter.submitList(posts);
        textEmptyFeed.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void bindProfileHeader() {
        textFeedTitle.setText(getString(R.string.feed_title, UserProfileStorage.getName(this, userName)));
    }

    @Override
    public void onLikeClicked(AnimalPost post) {
        if (post.getId() == null) {
            return;
        }
        AnimalPostRepository.toggleLike(post.getId());
        loadFeedPosts();
    }

    @Override
    public void onShareClicked(AnimalPost post) {
        String shareText = getString(
                R.string.share_post_text,
                post.getAnimalName(),
                PostTypeLabel(post),
                post.getDescription()
        );
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share_post)));
    }

    @Override
    public void onMapClicked(AnimalPost post) {
        if (post.getLatitude() == null || post.getLongitude() == null) {
            Toast.makeText(this, R.string.error_location_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PostLocationMapActivity.class);
        intent.putExtra(IntentKeys.EXTRA_LATITUDE, post.getLatitude());
        intent.putExtra(IntentKeys.EXTRA_LONGITUDE, post.getLongitude());
        intent.putExtra(IntentKeys.EXTRA_LOCATION_REFERENCE, post.getLocationReference());
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, post.getAnimalName());
        startActivity(intent);
    }

    private String PostTypeLabel(AnimalPost post) {
        return com.example.petcompanyapp.utils.PostType.isLost(post.getPostType())
                ? getString(R.string.post_type_lost)
                : getString(R.string.post_type_adoption);
    }
}
