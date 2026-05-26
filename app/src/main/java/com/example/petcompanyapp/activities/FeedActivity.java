package com.petbook.app.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.AnimalPostAdapter;
import com.petbook.app.models.AnimalPost;
import com.petbook.app.models.FairAnimal;
import com.petbook.app.models.User;
import com.petbook.app.notifications.PushNotificationHelper;
import com.petbook.app.repositories.AdoptionInterestRepository;
import com.petbook.app.repositories.ApiPostRepository;
import com.petbook.app.repositories.ApiUserRepository;
import com.petbook.app.repositories.AnimalPostRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.repositories.FirebasePushRepository;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.repositories.NotificationRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.FeedFilter;
import com.petbook.app.utils.AsyncRunner;
import com.petbook.app.utils.BottomNavigationHelper;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.PostType;
import com.petbook.app.utils.SwipeNavigationHelper;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
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
    private SwipeNavigationHelper swipeNavigationHelper;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    FirebasePushRepository.syncCurrentSessionToken(this);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);
        PushNotificationHelper.ensureNotificationChannel(this);

        long extraUserId = getIntent().getLongExtra(IntentKeys.EXTRA_USER_ID, -1L);
        userId = extraUserId >= 0 ? extraUserId : UserProfileStorage.getUserId(this);
        userType = getIntent().getStringExtra(IntentKeys.EXTRA_USER_TYPE);
        if (userType == null) {
            userType = UserProfileStorage.getUserType(this, UserType.PERSON);
        }

        userName = getIntent().getStringExtra(IntentKeys.EXTRA_USER_NAME);
        if (userName == null || userName.trim().isEmpty()) {
            userName = getString(R.string.default_user_name);
        }
        userEmail = getIntent().getStringExtra(IntentKeys.EXTRA_USER_EMAIL);
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = userName;
        }
        if (FirebaseUserRepository.isEnabled(this)) {
            loadActiveUserFromFirebase();
        } else if (!FeatureFlags.useRemoteApi(this)) {
            User activeUser = UserRepository.findById(this, userId);
            if (activeUser != null) {
                userName = activeUser.getName();
                userEmail = activeUser.getEmail();
                userType = activeUser.getUserType();
            }
        }
        UserProfileStorage.saveProfile(this, userId, userName, userEmail, userType);

        textFeedTitle = findViewById(R.id.textFeedTitle);
        TextView textLogout = findViewById(R.id.textLogout);
        MaterialButtonToggleGroup toggleGroupFeedFilter = findViewById(R.id.toggleGroupFeedFilter);
        RecyclerView recyclerFeedPosts = findViewById(R.id.recyclerFeedPosts);
        textEmptyFeed = findViewById(R.id.textEmptyFeed);

        bindProfileHeader();
        BottomNavigationHelper.bind(this, BottomNavigationHelper.DESTINATION_FEED);
        swipeNavigationHelper = new SwipeNavigationHelper(this, BottomNavigationHelper.DESTINATION_FEED);


        animalPostAdapter = new AnimalPostAdapter(this, userId);
        recyclerFeedPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeedPosts.setAdapter(animalPostAdapter);

        textLogout.setOnClickListener(v -> logout());

       

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
        ensureNotificationPermissionIfNeeded();
        FirebasePushRepository.syncCurrentSessionToken(this);
        animalPostAdapter.setCurrentUserId(userId);
        bindProfileHeader();
        if (FirebaseUserRepository.isEnabled(this)) {
            loadActiveUserFromFirebase();
        } else if (FeatureFlags.useRemoteApi(this)) {
            loadActiveUser();
        }
        loadFeedPosts();
        BottomNavigationHelper.refreshNotificationBadge(this);
    }

    private void loadFeedPosts() {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.cleanupSamplePostsIfNeeded(this, new FirebasePostRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    FirebasePostRepository.bootstrapLocalPostsIfNeeded(FeedActivity.this, new FirebasePostRepository.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            FirebasePostRepository.loadPosts(FeedActivity.this, selectedFilter, UserProfileStorage.getEmail(FeedActivity.this, userEmail), new FirebasePostRepository.PostsCallback() {
                                @Override
                                public void onSuccess(java.util.List<AnimalPost> posts) {
                                    animalPostAdapter.submitList(posts);
                                    textEmptyFeed.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                                }

                                @Override
                                public void onError(String message) {
                                    Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            java.util.List<com.petbook.app.models.AnimalPost> posts =
                    AnimalPostRepository.getPosts(this, selectedFilter);
            animalPostAdapter.submitList(posts);
            textEmptyFeed.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
            return;
        }

        AsyncRunner.run(
                () -> ApiPostRepository.getPosts(this, selectedFilter),
                posts -> {
                    animalPostAdapter.submitList(posts);
                    textEmptyFeed.setVisibility(posts.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                },
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_server_unavailable) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void loadActiveUser() {
        if (userId == null) {
            return;
        }

        AsyncRunner.run(
                () -> ApiUserRepository.findById(this, userId),
                activeUser -> {
                    userName = activeUser.getName();
                    userEmail = activeUser.getEmail();
                    userType = activeUser.getUserType();
                    UserProfileStorage.saveProfile(this, userId, userName, userEmail, userType);
                    bindProfileHeader();
                },
                exception -> {
                    // Mantem dados de sessao atuais se a leitura remota falhar.
                }
        );
    }

    private void bindProfileHeader() {
        textFeedTitle.setText(getString(R.string.feed_title, UserProfileStorage.getName(this, userName)));
    }

    private void loadActiveUserFromFirebase() {
        if (userId == null) {
            return;
        }

        FirebaseUserRepository.findById(this, userId, new FirebaseUserRepository.UserCallback() {
            @Override
            public void onSuccess(User activeUser) {
                runOnUiThread(() -> {
                    userName = activeUser.getName();
                    userEmail = activeUser.getEmail();
                    userType = activeUser.getUserType();
                    UserProfileStorage.saveProfile(FeedActivity.this, userId, userName, userEmail, userType);
                    bindProfileHeader();
                });
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (swipeNavigationHelper != null) {
            swipeNavigationHelper.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    private void ensureNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void logout() {
        UserProfileStorage.clearProfile(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onLikeClicked(AnimalPost post) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.toggleLike(
                    this,
                    post,
                    UserProfileStorage.getEmail(this, userEmail),
                    userName,
                    userId,
                    new FirebasePostRepository.CompletionCallback() {
                        @Override
                        public void onSuccess() {
                            loadFeedPosts();
                            BottomNavigationHelper.refreshNotificationBadge(FeedActivity.this);
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            if (post.getId() == null) {
                return;
            }
            boolean wasLiked = post.isLiked();
            AnimalPostRepository.toggleLike(this, post.getId());
            if (!wasLiked
                    && userId != null
                    && post.getAuthorUserId() != null
                    && userId.longValue() != post.getAuthorUserId().longValue()) {
                NotificationRepository.addNotification(
                        this,
                        post.getAuthorUserId(),
                        NotificationType.LIKE,
                        getString(R.string.notification_like_title),
                        getString(R.string.notification_like_message, userName, post.getAnimalName()),
                        post.getId(),
                        post.getPostType(),
                        userId,
                        userName,
                        userEmail,
                        null
                );
            }
            loadFeedPosts();
            BottomNavigationHelper.refreshNotificationBadge(this);
            return;
        }
        Toast.makeText(this, R.string.info_like_sync_pending, Toast.LENGTH_SHORT).show();
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
        if (PostType.isFair(post.getPostType())) {
            if (post.getId() == null) {
                return;
            }
            Intent intent = new Intent(this, FairPostDetailActivity.class);
            intent.putExtra(IntentKeys.EXTRA_POST_ID, post.getId());
            startActivity(intent);
            return;
        }

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

    @Override
    public void onCommentClicked(AnimalPost post) {
        boolean isOwner = userId != null
                && post.getAuthorUserId() != null
                && userId.longValue() == post.getAuthorUserId().longValue();

        if (PostType.isLost(post.getPostType()) || isOwner) {
            if (post.getId() == null) {
                return;
            }
            Intent intent = new Intent(this, PostCommentsActivity.class);
            intent.putExtra(IntentKeys.EXTRA_POST_ID, post.getId());
            intent.putExtra(IntentKeys.EXTRA_POST_TYPE, post.getPostType());
            startActivity(intent);
            return;
        }

        if (userId == null || post.getAuthorUserId() == null) {
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_ID, post.getAuthorUserId());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_NAME, post.getAuthorName());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL, post.getAuthorEmail());
        startActivity(intent);
    }

    @Override
    public void onInterestClicked(AnimalPost post) {
        if (userId == null || post.getId() == null || post.getAuthorUserId() == null) {
            return;
        }
        if (userId.longValue() == post.getAuthorUserId().longValue()) {
            return;
        }

        if (PostType.isFair(post.getPostType())) {
            showFairInterestDialog(post);
            return;
        }

        registerInterest(post, post.getAnimalName());
    }

    @Override
    public void onEditClicked(AnimalPost post) {
        if (post.getId() == null || userId == null) {
            return;
        }

        Intent intent = new Intent(this, PostCreateActivity.class);
        intent.putExtra(IntentKeys.EXTRA_USER_ID, userId);
        intent.putExtra(IntentKeys.EXTRA_USER_TYPE, userType);
        intent.putExtra(IntentKeys.EXTRA_USER_NAME, userName);
        intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, userEmail);
        intent.putExtra(IntentKeys.EXTRA_POST_ID, post.getId());
        intent.putExtra(IntentKeys.EXTRA_POST_TYPE, post.getPostType());
        intent.putExtra(IntentKeys.EXTRA_POST_ANIMAL_NAME, post.getAnimalName());
        intent.putExtra(IntentKeys.EXTRA_POST_SPECIES, post.getSpecies());
        intent.putExtra(IntentKeys.EXTRA_POST_BREED, post.getBreed());
        intent.putExtra(IntentKeys.EXTRA_POST_AGE, post.getAge());
        intent.putExtra(IntentKeys.EXTRA_POST_DESCRIPTION, post.getDescription());
        intent.putExtra(IntentKeys.EXTRA_POST_CONTACT_PHONE, post.getContactPhone());
        intent.putExtra(IntentKeys.EXTRA_LOCATION_REFERENCE, post.getLocationReference());
        intent.putExtra(IntentKeys.EXTRA_POST_IMAGE_URI, post.getImageUri());
        if (post.getLatitude() != null) {
            intent.putExtra(IntentKeys.EXTRA_LATITUDE, post.getLatitude());
        }
        if (post.getLongitude() != null) {
            intent.putExtra(IntentKeys.EXTRA_LONGITUDE, post.getLongitude());
        }
        startActivity(intent);
    }

    @Override
    public void onDeleteClicked(AnimalPost post) {
        if (post.getId() == null || userId == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_post_title)
                .setMessage(R.string.delete_post_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete_post, (dialog, which) -> deletePost(post))
                .show();
    }

    private void deletePost(AnimalPost post) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.deletePost(this, post.getId(), new FirebasePostRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(FeedActivity.this, R.string.post_delete_success, Toast.LENGTH_SHORT).show();
                    loadFeedPosts();
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(
                            FeedActivity.this,
                            message == null ? getString(R.string.error_delete_post_failed) : message,
                            Toast.LENGTH_LONG
                    ).show();
                }
            });
            return;
        }

        if (!FeatureFlags.useRemoteApi(this)) {
            boolean deleted = AnimalPostRepository.deletePost(this, post.getId(), userId);
            if (!deleted) {
                Toast.makeText(this, R.string.error_delete_post_failed, Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.post_delete_success, Toast.LENGTH_SHORT).show();
            loadFeedPosts();
            return;
        }

        AsyncRunner.run(
                () -> {
                    ApiPostRepository.deletePost(this, post.getId(), userId);
                    return true;
                },
                ignored -> {
                    Toast.makeText(this, R.string.post_delete_success, Toast.LENGTH_SHORT).show();
                    loadFeedPosts();
                },
                exception -> Toast.makeText(
                        this,
                        exception.getMessage() == null ? getString(R.string.error_delete_post_failed) : exception.getMessage(),
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void showFairInterestDialog(AnimalPost post) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.getFairAnimals(this, post.getId(), new FirebasePostRepository.FairAnimalsCallback() {
                @Override
                public void onSuccess(java.util.List<FairAnimal> fairAnimals) {
                    showFairInterestDialogOptions(post, fairAnimals);
                }

                @Override
                public void onError(String message) {
                    Toast.makeText(
                            FeedActivity.this,
                            message == null ? getString(R.string.fair_animals_empty) : message,
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
            return;
        }

        java.util.List<FairAnimal> fairAnimals = AnimalPostRepository.getFairAnimalsForPost(this, post.getId());
        showFairInterestDialogOptions(post, fairAnimals);
    }

    private void showFairInterestDialogOptions(AnimalPost post, java.util.List<FairAnimal> fairAnimals) {
        if (fairAnimals == null || fairAnimals.isEmpty()) {
            Toast.makeText(this, R.string.fair_animals_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = new String[fairAnimals.size()];
        for (int index = 0; index < fairAnimals.size(); index++) {
            FairAnimal fairAnimal = fairAnimals.get(index);
            options[index] = fairAnimal.getName() + " - " + fairAnimal.getSpecies();
        }

        final int[] selectedIndex = {0};
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_interest_select_animal)
                .setSingleChoiceItems(options, 0, (dialog, which) -> selectedIndex[0] = which)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_interest, (dialog, which) -> {
                    FairAnimal fairAnimal = fairAnimals.get(selectedIndex[0]);
                    registerInterest(post, fairAnimal.getName());
                })
                .show();
    }

    private void registerInterest(AnimalPost post, String animalName) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebasePostRepository.registerInterest(
                    this,
                    post,
                    userId,
                    userName,
                    UserProfileStorage.getEmail(this, userEmail),
                    animalName,
                    new FirebasePostRepository.BooleanCallback() {
                        @Override
                        public void onSuccess(boolean created) {
                            if (!created) {
                                Toast.makeText(FeedActivity.this, R.string.info_interest_already_registered, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            Toast.makeText(FeedActivity.this, R.string.interest_success, Toast.LENGTH_SHORT).show();
                            BottomNavigationHelper.refreshNotificationBadge(FeedActivity.this);
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(FeedActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
            return;
        }

        boolean saved = AdoptionInterestRepository.registerInterest(this, post.getId(), userId, animalName);
        if (!saved) {
            Toast.makeText(this, R.string.info_interest_already_registered, Toast.LENGTH_SHORT).show();
            return;
        }

        NotificationRepository.addNotification(
                this,
                post.getAuthorUserId(),
                NotificationType.ADOPTION_INTEREST,
                getString(R.string.notification_interest_title),
                getString(
                        PostType.isFair(post.getPostType())
                                ? R.string.notification_interest_fair_message
                                : R.string.notification_interest_message,
                        userName,
                        animalName
                ),
                post.getId(),
                post.getPostType(),
                userId,
                userName,
                userEmail,
                null
        );
        Toast.makeText(this, R.string.interest_success, Toast.LENGTH_SHORT).show();
        BottomNavigationHelper.refreshNotificationBadge(this);
    }

    private String PostTypeLabel(AnimalPost post) {
        if (com.petbook.app.utils.PostType.isLost(post.getPostType())) {
            return getString(R.string.post_type_lost);
        }
        if (com.petbook.app.utils.PostType.isFair(post.getPostType())) {
            return getString(R.string.post_type_fair);
        }
        return getString(R.string.post_type_adoption);
    }
}

