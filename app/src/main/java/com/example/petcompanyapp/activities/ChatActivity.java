package com.petbook.app.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.ChatMessageAdapter;
import com.petbook.app.models.ConversationSummary;
import com.petbook.app.models.User;
import com.petbook.app.repositories.ChatRepository;
import com.petbook.app.repositories.FirebaseChatRepository;
import com.petbook.app.repositories.FirebaseUserRepository;
import com.petbook.app.repositories.UserRepository;
import com.petbook.app.utils.FirebaseChatConfig;
import com.petbook.app.utils.FeatureFlags;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.UserProfileStorage;
import com.petbook.app.utils.UserType;
import com.google.firebase.firestore.ListenerRegistration;

public class ChatActivity extends AppCompatActivity {

    private Long currentUserId;
    private Long targetUserId;
    private Long conversationId;
    private String currentUserName;
    private String currentUserEmail;
    private String currentUserType;
    private String targetUserName;
    private String targetUserEmail;
    private String targetUserType;
    private ChatMessageAdapter adapter;
    private RecyclerView recyclerChatMessages;
    private EditText editChatMessage;
    private TextView textChatTitle;
    private TextView textChatSubtitle;
    private ListenerRegistration messagesRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = UserProfileStorage.getUserId(this);
        currentUserName = UserProfileStorage.getName(this, "");
        currentUserEmail = UserProfileStorage.getEmail(this, "");
        currentUserType = UserProfileStorage.getUserType(this, UserType.PERSON);
        conversationId = getIntent().hasExtra(IntentKeys.EXTRA_CONVERSATION_ID)
                ? getIntent().getLongExtra(IntentKeys.EXTRA_CONVERSATION_ID, -1L)
                : null;
        targetUserId = getIntent().hasExtra(IntentKeys.EXTRA_TARGET_USER_ID)
                ? getIntent().getLongExtra(IntentKeys.EXTRA_TARGET_USER_ID, -1L)
                : null;

        if (conversationId != null && conversationId < 0) {
            conversationId = null;
        }
        if (targetUserId != null && targetUserId < 0) {
            targetUserId = null;
        }
        targetUserName = getIntent().getStringExtra(IntentKeys.EXTRA_TARGET_USER_NAME);
        targetUserType = getIntent().getStringExtra(IntentKeys.EXTRA_TARGET_USER_TYPE);
        targetUserEmail = getIntent().getStringExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL);

        ImageButton buttonBack = findViewById(R.id.buttonBackChat);
        ImageButton buttonSend = findViewById(R.id.buttonSendMessage);
        recyclerChatMessages = findViewById(R.id.recyclerChatMessages);
        editChatMessage = findViewById(R.id.editChatMessage);
        textChatTitle = findViewById(R.id.textChatTitle);
        textChatSubtitle = findViewById(R.id.textChatSubtitle);

        buttonBack.setOnClickListener(v -> finish());

        if (currentUserId == null) {
            Toast.makeText(this, R.string.error_chat_user_invalid, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        adapter = new ChatMessageAdapter(currentUserId, currentUserEmail);
        recyclerChatMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerChatMessages.setAdapter(adapter);

        bindHeader();
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (messagesRegistration != null) {
            messagesRegistration.remove();
            messagesRegistration = null;
        }
    }

    private void bindHeader() {
        String fallbackName = targetUserName;
        String fallbackType = targetUserType;

        if (!FeatureFlags.useFirebaseChat(this) && targetUserId != null) {
            User targetUser = UserRepository.findById(this, targetUserId);
            if (targetUser != null) {
                fallbackName = targetUser.getName();
                fallbackType = targetUser.getUserType();
                targetUserEmail = targetUser.getEmail();
            }
        } else if (FeatureFlags.useFirebaseChat(this)
                && (fallbackName == null || fallbackName.trim().isEmpty())
                && targetUserId != null) {
            FirebaseUserRepository.findById(this, targetUserId, new FirebaseUserRepository.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    runOnUiThread(() -> {
                        targetUserName = user.getName();
                        targetUserType = user.getUserType();
                        targetUserEmail = user.getEmail();
                        textChatTitle.setText(targetUserName);
                        textChatSubtitle.setText(getString(
                                R.string.chat_partner_type,
                                getUserTypeLabel(targetUserType)
                        ));
                    });
                }

                @Override
                public void onError(String message) {
                }
            });
        }

        if (fallbackName == null || fallbackName.trim().isEmpty()) {
            fallbackName = getString(R.string.chat_default_title);
        }

        targetUserName = fallbackName;
        targetUserType = fallbackType;

        textChatTitle.setText(fallbackName);
        textChatSubtitle.setText(getString(
                R.string.chat_partner_type,
                getUserTypeLabel(fallbackType)
        ));
    }

    private void loadMessages() {
        if (currentUserId == null) {
            return;
        }

        if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
            if (targetUserEmail == null || targetUserEmail.trim().isEmpty()) {
                Toast.makeText(this, R.string.error_chat_user_invalid, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (messagesRegistration != null) {
                messagesRegistration.remove();
            }
            FirebaseChatRepository.openConversation(
                    this,
                    currentUserName,
                    currentUserEmail,
                    currentUserType,
                    targetUserName,
                    targetUserEmail,
                    targetUserType
            );
            messagesRegistration = FirebaseChatRepository.listenMessages(
                    this,
                    currentUserEmail,
                    targetUserEmail,
                    new FirebaseChatRepository.MessagesCallback() {
                        @Override
                        public void onSuccess(java.util.List<com.petbook.app.models.ChatMessage> messages) {
                            adapter.submitList(messages);
                            if (!messages.isEmpty()) {
                                recyclerChatMessages.scrollToPosition(messages.size() - 1);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(
                                    ChatActivity.this,
                                    message == null ? getString(R.string.error_firebase_chat_not_configured) : message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
            return;
        }

        if (conversationId == null && targetUserId != null) {
            conversationId = ChatRepository.findOrCreateConversation(this, currentUserId, targetUserId);
        }

        if (conversationId == null) {
            adapter.submitList(java.util.Collections.emptyList());
            return;
        }

        if (targetUserId == null) {
            ConversationSummary summary = ChatRepository.getConversationSummary(this, currentUserId, conversationId);
            if (summary != null) {
                targetUserId = summary.getPartnerUserId();
                textChatTitle.setText(summary.getPartnerName());
                textChatSubtitle.setText(getString(R.string.chat_partner_type, getUserTypeLabel(summary.getPartnerType())));
                targetUserEmail = summary.getPartnerEmail();
            }
        }

        java.util.List<com.petbook.app.models.ChatMessage> messages =
                ChatRepository.getMessages(this, conversationId, currentUserId);
        adapter.submitList(messages);
        if (!messages.isEmpty()) {
            recyclerChatMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage() {
        if (currentUserId == null || targetUserId == null) {
            if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
                if (targetUserEmail == null || targetUserEmail.trim().isEmpty()) {
                    Toast.makeText(this, R.string.error_chat_user_invalid, Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                Toast.makeText(this, R.string.error_chat_user_invalid, Toast.LENGTH_LONG).show();
                return;
            }
        }

        String message = editChatMessage.getText().toString().trim();
        if (message.isEmpty()) {
            editChatMessage.setError(getString(R.string.error_chat_message_required));
            return;
        }

        if (FeatureFlags.useFirebaseChat(this) && FirebaseChatConfig.isConfigured(this)) {
            FirebaseChatRepository.sendMessage(
                    this,
                    currentUserName,
                    currentUserEmail,
                    currentUserType,
                    targetUserName,
                    targetUserEmail,
                    targetUserType,
                    message
            );
            editChatMessage.setText("");
            return;
        }

        if (currentUserId == null || targetUserId == null) {
            Toast.makeText(this, R.string.error_chat_user_invalid, Toast.LENGTH_LONG).show();
            return;
        }

        ChatRepository.sendMessage(this, currentUserId, targetUserId, message);
        editChatMessage.setText("");
        loadMessages();
    }

    private String getUserTypeLabel(String userType) {
        if (userType == null || userType.trim().isEmpty()) {
            return getString(R.string.chat_unknown_type);
        }
        return UserType.isCompany(userType)
                ? getString(R.string.user_type_company)
                : getString(R.string.user_type_person);
    }
}

