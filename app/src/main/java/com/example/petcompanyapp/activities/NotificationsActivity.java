package com.petbook.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.petbook.app.R;
import com.petbook.app.adapters.NotificationAdapter;
import com.petbook.app.models.AppNotification;
import com.petbook.app.repositories.FirebaseNotificationRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.repositories.NotificationRepository;
import com.petbook.app.utils.BottomNavigationHelper;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.PostType;
import com.petbook.app.utils.SwipeNavigationHelper;
import com.petbook.app.utils.UserProfileStorage;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationActionListener {

    private Long currentUserId;
    private NotificationAdapter adapter;
    private TextView textEmptyState;
    private SwipeNavigationHelper swipeNavigationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        currentUserId = UserProfileStorage.getUserId(this);

        ImageButton buttonBack = findViewById(R.id.buttonBackNotifications);
        TextView textMarkAllRead = findViewById(R.id.textMarkAllRead);
        TextView textClearAllNotifications = findViewById(R.id.textClearAllNotifications);
        RecyclerView recyclerNotifications = findViewById(R.id.recyclerNotifications);
        textEmptyState = findViewById(R.id.textEmptyNotifications);

        buttonBack.setOnClickListener(v -> finish());
        BottomNavigationHelper.bind(this, BottomNavigationHelper.DESTINATION_NOTIFICATIONS);
        swipeNavigationHelper = new SwipeNavigationHelper(this, BottomNavigationHelper.DESTINATION_NOTIFICATIONS);
        textMarkAllRead.setOnClickListener(v -> {
            if (currentUserId != null) {
                if (FirebasePostRepository.isEnabled(this)) {
                    FirebaseNotificationRepository.markAllAsRead(this, UserProfileStorage.getEmail(this, ""));
                    loadNotifications();
                    BottomNavigationHelper.refreshNotificationBadge(this);
                    return;
                }
                NotificationRepository.markAllAsRead(this, currentUserId);
                loadNotifications();
                BottomNavigationHelper.refreshNotificationBadge(this);
            }
        });
        textClearAllNotifications.setOnClickListener(v -> confirmClearAllNotifications());

        adapter = new NotificationAdapter(this);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(adapter);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (swipeNavigationHelper != null) {
            swipeNavigationHelper.onTouchEvent(event);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationHelper.refreshNotificationBadge(this);
        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUserId == null) {
            adapter.submitList(java.util.Collections.emptyList());
            textEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (FirebasePostRepository.isEnabled(this)) {
            FirebaseNotificationRepository.getNotifications(
                    this,
                    UserProfileStorage.getEmail(this, ""),
                    new FirebaseNotificationRepository.NotificationsCallback() {
                        @Override
                        public void onSuccess(List<AppNotification> notifications) {
                            adapter.submitList(notifications);
                            textEmptyState.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                        }

                        @Override
                        public void onError(String message) {
                            textEmptyState.setVisibility(View.VISIBLE);
                        }
                    }
            );
            return;
        }

        List<AppNotification> notifications = NotificationRepository.getNotificationsForUser(this, currentUserId);
        adapter.submitList(notifications);
        textEmptyState.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNotificationClicked(AppNotification notification) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebaseNotificationRepository.markAsRead(this, notification.getId());
        } else {
            NotificationRepository.markAsRead(this, notification.getId());
        }
        BottomNavigationHelper.refreshNotificationBadge(this);

        if (notification.getRelatedConversationId() != null
                || notification.getRelatedUserId() != null
                || (notification.getRelatedUserEmail() != null && !notification.getRelatedUserEmail().trim().isEmpty())) {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            if (notification.getRelatedConversationId() != null) {
                chatIntent.putExtra(IntentKeys.EXTRA_CONVERSATION_ID, notification.getRelatedConversationId());
            }
            if (notification.getRelatedUserId() != null) {
                chatIntent.putExtra(IntentKeys.EXTRA_TARGET_USER_ID, notification.getRelatedUserId());
            }
            chatIntent.putExtra(IntentKeys.EXTRA_TARGET_USER_NAME, notification.getRelatedUserName());
            chatIntent.putExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL, notification.getRelatedUserEmail());
            startActivity(chatIntent);
            return;
        }

        if (notification.getRelatedPostId() != null
                && NotificationType.COMMENT.equals(notification.getType())) {
            Intent commentsIntent = new Intent(this, PostCommentsActivity.class);
            commentsIntent.putExtra(IntentKeys.EXTRA_POST_ID, notification.getRelatedPostId());
            commentsIntent.putExtra(IntentKeys.EXTRA_POST_TYPE, notification.getRelatedPostType());
            startActivity(commentsIntent);
            return;
        }

        if (notification.getRelatedPostId() != null && PostType.isFair(notification.getRelatedPostType())) {
            Intent fairIntent = new Intent(this, FairPostDetailActivity.class);
            fairIntent.putExtra(IntentKeys.EXTRA_POST_ID, notification.getRelatedPostId());
            startActivity(fairIntent);
            return;
        }

        if (notification.getRelatedPostId() != null) {
            Intent commentsIntent = new Intent(this, PostCommentsActivity.class);
            commentsIntent.putExtra(IntentKeys.EXTRA_POST_ID, notification.getRelatedPostId());
            commentsIntent.putExtra(IntentKeys.EXTRA_POST_TYPE, notification.getRelatedPostType());
            startActivity(commentsIntent);
            return;
        }

        startActivity(new Intent(this, FeedActivity.class));
    }

    @Override
    public void onMarkAsReadClicked(AppNotification notification) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebaseNotificationRepository.markAsRead(this, notification.getId());
        } else {
            NotificationRepository.markAsRead(this, notification.getId());
        }
        loadNotifications();
        BottomNavigationHelper.refreshNotificationBadge(this);
    }

    @Override
    public void onDeleteClicked(AppNotification notification) {
        if (FirebasePostRepository.isEnabled(this)) {
            FirebaseNotificationRepository.deleteNotification(this, notification.getId(), new FirebaseNotificationRepository.OperationCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        loadNotifications();
                        BottomNavigationHelper.refreshNotificationBadge(NotificationsActivity.this);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(
                            NotificationsActivity.this,
                            message == null ? getString(R.string.error_delete_notification_failed) : message,
                            Toast.LENGTH_LONG
                    ).show());
                }
            });
            return;
        }

        NotificationRepository.deleteNotification(this, notification.getId());
        loadNotifications();
        BottomNavigationHelper.refreshNotificationBadge(this);
    }

    private void confirmClearAllNotifications() {
        if (currentUserId == null) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.notifications_clear_all_title)
                .setMessage(R.string.notifications_clear_all_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_remove_all_notifications, (dialog, which) -> clearAllNotifications())
                .show();
    }

    private void clearAllNotifications() {
        if (currentUserId == null) {
            return;
        }

        if (FirebasePostRepository.isEnabled(this)) {
            FirebaseNotificationRepository.deleteAllNotifications(
                    this,
                    UserProfileStorage.getEmail(this, ""),
                    new FirebaseNotificationRepository.OperationCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() -> {
                                loadNotifications();
                                BottomNavigationHelper.refreshNotificationBadge(NotificationsActivity.this);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> Toast.makeText(
                                    NotificationsActivity.this,
                                    message == null ? getString(R.string.error_clear_notifications_failed) : message,
                                    Toast.LENGTH_LONG
                            ).show());
                        }
                    }
            );
            return;
        }

        NotificationRepository.deleteAllForUser(this, currentUserId);
        loadNotifications();
        BottomNavigationHelper.refreshNotificationBadge(this);
    }
}
