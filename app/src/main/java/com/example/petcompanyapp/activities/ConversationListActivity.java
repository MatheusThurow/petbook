package com.petbook.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.ConversationAdapter;
import com.petbook.app.adapters.UserSearchAdapter;
import com.petbook.app.models.ConversationSummary;
import com.petbook.app.models.User;
import com.petbook.app.repositories.ChatRepository;
import com.petbook.app.repositories.FirebaseChatRepository;
import com.petbook.app.repositories.FirebaseUserDirectoryRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.BottomNavigationHelper;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.SwipeNavigationHelper;
import com.petbook.app.utils.UserProfileStorage;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class ConversationListActivity extends AppCompatActivity implements
        ConversationAdapter.OnConversationClickListener,
        UserSearchAdapter.OnUserClickListener {

    private Long currentUserId;
    private ConversationAdapter conversationAdapter;
    private UserSearchAdapter userSearchAdapter;
    private TextView textEmptyConversations;
    private TextView textEmptyUserSearch;
    private TextView textRecentConversationsToggle;
    private EditText editSearchUsers;
    private RecyclerView recyclerConversations;
    private ListenerRegistration conversationsRegistration;
    private boolean recentConversationsExpanded;
    private SwipeNavigationHelper swipeNavigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_list);

        currentUserId = UserProfileStorage.getUserId(this);

        ImageButton buttonBack = findViewById(R.id.buttonBackConversations);
        editSearchUsers = findViewById(R.id.editSearchUsers);
        RecyclerView recyclerUsers = findViewById(R.id.recyclerUsers);
        recyclerConversations = findViewById(R.id.recyclerConversations);
        textEmptyConversations = findViewById(R.id.textEmptyConversations);
        textEmptyUserSearch = findViewById(R.id.textEmptyUserSearch);
        textRecentConversationsToggle = findViewById(R.id.textRecentConversationsToggle);
        View layoutRecentConversationsHeader = findViewById(R.id.layoutRecentConversationsHeader);

        buttonBack.setOnClickListener(v -> finish());
        BottomNavigationHelper.bind(this, BottomNavigationHelper.DESTINATION_CONVERSATIONS);
        swipeNavigationHelper = new SwipeNavigationHelper(this, BottomNavigationHelper.DESTINATION_CONVERSATIONS);

        userSearchAdapter = new UserSearchAdapter(this);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(userSearchAdapter);

        conversationAdapter = new ConversationAdapter(this);
        recyclerConversations.setLayoutManager(new LinearLayoutManager(this));
        recyclerConversations.setAdapter(conversationAdapter);
        updateRecentConversationsVisibility();
        layoutRecentConversationsHeader.setOnClickListener(v -> toggleRecentConversations());

        editSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Sem acao.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadUserSearch(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Sem acao.
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = swipeNavigationHelper != null && swipeNavigationHelper.onTouchEvent(event);
        return handled || super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.refreshNotificationBadge(this);
        if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
            FirebaseUserDirectoryRepository.bootstrapLocalUsersIfNeeded(this, new FirebaseUserDirectoryRepository.CompletionCallback() {
                @Override
                public void onSuccess() {
                    loadUserSearch(editSearchUsers.getText() == null ? "" : editSearchUsers.getText().toString());
                    loadConversations();
                }

                @Override
                public void onError(String message) {
                    loadUserSearch(editSearchUsers.getText() == null ? "" : editSearchUsers.getText().toString());
                    loadConversations();
                }
            });
            return;
        }

        loadUserSearch(editSearchUsers.getText() == null ? "" : editSearchUsers.getText().toString());
        loadConversations();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (conversationsRegistration != null) {
            conversationsRegistration.remove();
            conversationsRegistration = null;
        }
    }

    private void loadConversations() {
        if (currentUserId == null) {
            conversationAdapter.submitList(java.util.Collections.emptyList());
            updateRecentConversationsVisibility();
            return;
        }

        if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
            if (conversationsRegistration != null) {
                conversationsRegistration.remove();
            }
            conversationsRegistration = FirebaseChatRepository.listenConversations(
                    this,
                    UserProfileStorage.getEmail(this, ""),
                    new FirebaseChatRepository.ConversationsCallback() {
                        @Override
                        public void onSuccess(List<ConversationSummary> conversations) {
                            conversationAdapter.submitList(conversations);
                            updateRecentConversationsVisibility();
                        }

                        @Override
                        public void onError(String message) {
                            updateRecentConversationsVisibility();
                        }
                    }
            );
            return;
        }

        List<ConversationSummary> conversations = ChatRepository.getConversations(this, currentUserId);
        conversationAdapter.submitList(conversations);
        updateRecentConversationsVisibility();
    }

    private void loadUserSearch(String query) {
        if (currentUserId == null) {
            userSearchAdapter.submitList(java.util.Collections.emptyList());
            textEmptyUserSearch.setVisibility(android.view.View.VISIBLE);
            return;
        }

        if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
            FirebaseUserDirectoryRepository.searchUsers(
                    this,
                    query,
                    UserProfileStorage.getEmail(this, ""),
                    new FirebaseUserDirectoryRepository.UserListCallback() {
                        @Override
                        public void onSuccess(List<User> users) {
                            userSearchAdapter.submitList(users);
                            boolean shouldShowEmpty = query != null && !query.trim().isEmpty() && users.isEmpty();
                            textEmptyUserSearch.setVisibility(shouldShowEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
                        }

                        @Override
                        public void onError(String message) {
                            userSearchAdapter.submitList(java.util.Collections.emptyList());
                            textEmptyUserSearch.setVisibility(android.view.View.VISIBLE);
                        }
                    }
            );
            return;
        }

        List<User> users = UserRepository.searchActiveUsers(this, query, currentUserId);
        userSearchAdapter.submitList(users);
        boolean shouldShowEmpty = query != null && !query.trim().isEmpty() && users.isEmpty();
        textEmptyUserSearch.setVisibility(shouldShowEmpty ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    @Override
    public void onConversationClicked(ConversationSummary conversation) {
        Intent intent = new Intent(this, ChatActivity.class);
        if (conversation.getConversationId() != null) {
            intent.putExtra(IntentKeys.EXTRA_CONVERSATION_ID, conversation.getConversationId());
        }
        if (conversation.getPartnerUserId() != null) {
            intent.putExtra(IntentKeys.EXTRA_TARGET_USER_ID, conversation.getPartnerUserId());
        }
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_NAME, conversation.getPartnerName());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_TYPE, conversation.getPartnerType());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL, conversation.getPartnerEmail());
        startActivity(intent);
    }

    @Override
    public void onUserClicked(User user) {
        if (currentUserId == null) {
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        if (!FeatureFlags.useFirebaseChat(this) || !FirebaseChatConfig.isConfigured(this)) {
            if (user.getId() == null) {
                return;
            }
            long conversationId = ChatRepository.findOrCreateConversation(this, currentUserId, user.getId());
            intent.putExtra(IntentKeys.EXTRA_CONVERSATION_ID, conversationId);
            intent.putExtra(IntentKeys.EXTRA_TARGET_USER_ID, user.getId());
        }
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_NAME, user.getName());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_TYPE, user.getUserType());
        intent.putExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL, user.getEmail());
        startActivity(intent);
    }

    private void toggleRecentConversations() {
        recentConversationsExpanded = !recentConversationsExpanded;
        updateRecentConversationsVisibility();
    }

    private void updateRecentConversationsVisibility() {
        if (textRecentConversationsToggle == null || recyclerConversations == null || textEmptyConversations == null) {
            return;
        }

        textRecentConversationsToggle.setText(
                recentConversationsExpanded
                        ? getString(R.string.action_collapse_recent_conversations)
                        : getString(R.string.action_expand_recent_conversations)
        );

        if (!recentConversationsExpanded) {
            recyclerConversations.setVisibility(android.view.View.GONE);
            textEmptyConversations.setVisibility(android.view.View.GONE);
            return;
        }

        boolean hasConversations = conversationAdapter != null && conversationAdapter.getItemCount() > 0;
        recyclerConversations.setVisibility(hasConversations ? android.view.View.VISIBLE : android.view.View.GONE);
        textEmptyConversations.setVisibility(hasConversations ? android.view.View.GONE : android.view.View.VISIBLE);
    }
}

