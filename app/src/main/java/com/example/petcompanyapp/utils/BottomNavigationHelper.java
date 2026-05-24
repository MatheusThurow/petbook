package com.petbook.app.utils;

import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.R;
import com.petbook.app.activities.ConversationListActivity;
import com.petbook.app.activities.FeedActivity;
import com.petbook.app.activities.NotificationsActivity;
import com.petbook.app.activities.PostCreateActivity;
import com.petbook.app.activities.ProfileActivity;
import com.petbook.app.repositories.FirebaseNotificationRepository;
import com.petbook.app.repositories.FirebasePostRepository;
import com.petbook.app.repositories.NotificationRepository;

public final class BottomNavigationHelper {

    public static final String DESTINATION_FEED = "feed";
    public static final String DESTINATION_CONVERSATIONS = "conversations";
    public static final String DESTINATION_NOTIFICATIONS = "notifications";
    public static final String DESTINATION_PROFILE = "profile";
    private static final String[] DESTINATION_ORDER = new String[]{
            DESTINATION_FEED,
            DESTINATION_CONVERSATIONS,
            DESTINATION_NOTIFICATIONS,
            DESTINATION_PROFILE
    };

    private BottomNavigationHelper() {
        // Helper estatico para a barra inferior principal.
    }

    public static void bind(AppCompatActivity activity, String currentDestination) {
        ImageButton buttonFeed = activity.findViewById(R.id.buttonNavFeed);
        ImageButton buttonConversations = activity.findViewById(R.id.buttonNavConversations);
        ImageButton buttonNotifications = activity.findViewById(R.id.buttonNavNotifications);
        ImageButton buttonProfile = activity.findViewById(R.id.buttonNavProfile);
        View buttonCreatePost = activity.findViewById(R.id.buttonNavCreatePost);
        TextView textNotificationBadge = activity.findViewById(R.id.textBottomNavNotificationBadge);

        if (buttonFeed == null
                || buttonConversations == null
                || buttonNotifications == null
                || buttonProfile == null
                || buttonCreatePost == null) {
            return;
        }

        bindSelectedState(buttonFeed, DESTINATION_FEED.equals(currentDestination));
        bindSelectedState(buttonConversations, DESTINATION_CONVERSATIONS.equals(currentDestination));
        bindSelectedState(buttonNotifications, DESTINATION_NOTIFICATIONS.equals(currentDestination));
        bindSelectedState(buttonProfile, DESTINATION_PROFILE.equals(currentDestination));

        buttonFeed.setOnClickListener(v -> openSection(activity, currentDestination, DESTINATION_FEED, FeedActivity.class));
        buttonConversations.setOnClickListener(v -> openSection(activity, currentDestination, DESTINATION_CONVERSATIONS, ConversationListActivity.class));
        buttonNotifications.setOnClickListener(v -> openSection(activity, currentDestination, DESTINATION_NOTIFICATIONS, NotificationsActivity.class));
        buttonProfile.setOnClickListener(v -> openSection(activity, currentDestination, DESTINATION_PROFILE, ProfileActivity.class));
        buttonCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(activity, PostCreateActivity.class);
            Long userId = UserProfileStorage.getUserId(activity);
            if (userId != null) {
                intent.putExtra(IntentKeys.EXTRA_USER_ID, userId);
            }
            intent.putExtra(IntentKeys.EXTRA_USER_TYPE, UserProfileStorage.getUserType(activity, UserType.PERSON));
            intent.putExtra(IntentKeys.EXTRA_USER_NAME, UserProfileStorage.getName(activity, activity.getString(R.string.default_user_name)));
            intent.putExtra(IntentKeys.EXTRA_USER_EMAIL, UserProfileStorage.getEmail(activity, ""));
            activity.startActivity(intent);
        });

        refreshNotificationBadge(activity, textNotificationBadge);
    }

    public static void refreshNotificationBadge(AppCompatActivity activity) {
        TextView textNotificationBadge = activity.findViewById(R.id.textBottomNavNotificationBadge);
        if (textNotificationBadge == null) {
            return;
        }
        refreshNotificationBadge(activity, textNotificationBadge);
    }

    private static void refreshNotificationBadge(AppCompatActivity activity, TextView textNotificationBadge) {
        Long userId = UserProfileStorage.getUserId(activity);
        if (userId == null) {
            textNotificationBadge.setVisibility(View.GONE);
            return;
        }

        if (FirebasePostRepository.isEnabled(activity)) {
            FirebaseNotificationRepository.getUnreadCount(
                    activity,
                    UserProfileStorage.getEmail(activity, ""),
                    new FirebaseNotificationRepository.CountCallback() {
                        @Override
                        public void onSuccess(int unreadCount) {
                            bindBadge(textNotificationBadge, unreadCount);
                        }

                        @Override
                        public void onError(String message) {
                            textNotificationBadge.setVisibility(View.GONE);
                        }
                    }
            );
            return;
        }

        bindBadge(textNotificationBadge, NotificationRepository.getUnreadCount(activity, userId));
    }

    private static void bindBadge(TextView badgeView, int unreadCount) {
        if (unreadCount <= 0) {
            badgeView.setVisibility(View.GONE);
            return;
        }

        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
    }

    private static void bindSelectedState(ImageButton button, boolean selected) {
        button.setSelected(selected);
        button.setActivated(selected);
    }

    private static void openSection(
            AppCompatActivity activity,
            String currentDestination,
            String targetDestination,
            Class<?> destinationClass
    ) {
        if (targetDestination.equals(currentDestination)) {
            return;
        }

        Intent intent = new Intent(activity, destinationClass);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        );
        activity.startActivity(intent);
        applyTransition(activity, currentDestination, targetDestination);
        activity.finish();
    }

    public static boolean openAdjacentSection(
            AppCompatActivity activity,
            String currentDestination,
            boolean forward
    ) {
        int currentIndex = findDestinationIndex(currentDestination);
        if (currentIndex < 0) {
            return false;
        }

        int targetIndex = forward ? currentIndex + 1 : currentIndex - 1;
        if (targetIndex < 0 || targetIndex >= DESTINATION_ORDER.length) {
            return false;
        }

        String targetDestination = DESTINATION_ORDER[targetIndex];
        Class<?> destinationClass = resolveDestinationClass(targetDestination);
        if (destinationClass == null) {
            return false;
        }

        openSection(activity, currentDestination, targetDestination, destinationClass);
        return true;
    }

    private static int findDestinationIndex(String destination) {
        for (int index = 0; index < DESTINATION_ORDER.length; index++) {
            if (DESTINATION_ORDER[index].equals(destination)) {
                return index;
            }
        }
        return -1;
    }

    private static Class<?> resolveDestinationClass(String destination) {
        if (DESTINATION_FEED.equals(destination)) {
            return FeedActivity.class;
        }
        if (DESTINATION_CONVERSATIONS.equals(destination)) {
            return ConversationListActivity.class;
        }
        if (DESTINATION_NOTIFICATIONS.equals(destination)) {
            return NotificationsActivity.class;
        }
        if (DESTINATION_PROFILE.equals(destination)) {
            return ProfileActivity.class;
        }
        return null;
    }

    private static void applyTransition(
            AppCompatActivity activity,
            String currentDestination,
            String targetDestination
    ) {
        int currentIndex = findDestinationIndex(currentDestination);
        int targetIndex = findDestinationIndex(targetDestination);
        if (currentIndex < 0 || targetIndex < 0 || currentIndex == targetIndex) {
            return;
        }

        if (targetIndex > currentIndex) {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return;
        }

        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
