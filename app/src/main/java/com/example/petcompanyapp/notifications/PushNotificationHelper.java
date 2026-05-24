package com.petbook.app.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.petbook.app.R;
import com.petbook.app.activities.ChatActivity;
import com.petbook.app.activities.FairPostDetailActivity;
import com.petbook.app.activities.FeedActivity;
import com.petbook.app.activities.LoginActivity;
import com.petbook.app.activities.PostCommentsActivity;
import com.petbook.app.utils.IntentKeys;
import com.petbook.app.utils.NotificationType;
import com.petbook.app.utils.PostType;
import com.petbook.app.utils.UserProfileStorage;

import java.util.Map;

public final class PushNotificationHelper {

    public static final String CHANNEL_ID = "petbook_general_notifications";

    private PushNotificationHelper() {
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        manager.createNotificationChannel(channel);
    }

    public static void showNotification(Context context, String title, String message, Map<String, String> data) {
        ensureNotificationChannel(context);

        Intent targetIntent = buildTargetIntent(context, data);
        targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                targetIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title == null || title.trim().isEmpty()
                        ? context.getString(R.string.app_name)
                        : title)
                .setContentText(message == null ? "" : message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message == null ? "" : message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat.from(context)
                .notify((int) (System.currentTimeMillis() & 0x0FFFFFFF), builder.build());
    }

    private static Intent buildTargetIntent(Context context, Map<String, String> data) {
        Long loggedUserId = UserProfileStorage.getUserId(context);
        Intent fallbackIntent = new Intent(context, loggedUserId == null ? LoginActivity.class : FeedActivity.class);

        if (data == null || data.isEmpty()) {
            return fallbackIntent;
        }

        String relatedUserEmail = safe(data.get("relatedUserEmail"));
        String relatedUserName = safe(data.get("relatedUserName"));
        String notificationType = safe(data.get("notificationType"));
        String relatedPostType = safe(data.get("relatedPostType"));
        long relatedPostId = parseLong(data.get("relatedPostId"));

        if (NotificationType.CHAT_MESSAGE.equals(notificationType) || !relatedUserEmail.isEmpty()) {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra(IntentKeys.EXTRA_TARGET_USER_EMAIL, relatedUserEmail);
            intent.putExtra(IntentKeys.EXTRA_TARGET_USER_NAME, relatedUserName);
            return intent;
        }

        if (relatedPostId > 0 && PostType.isFair(relatedPostType)) {
            Intent intent = new Intent(context, FairPostDetailActivity.class);
            intent.putExtra(IntentKeys.EXTRA_POST_ID, relatedPostId);
            return intent;
        }

        if (relatedPostId > 0) {
            Intent intent = new Intent(context, PostCommentsActivity.class);
            intent.putExtra(IntentKeys.EXTRA_POST_ID, relatedPostId);
            intent.putExtra(IntentKeys.EXTRA_POST_TYPE, relatedPostType);
            return intent;
        }

        return fallbackIntent;
    }

    private static long parseLong(String value) {
        try {
            return value == null || value.trim().isEmpty() ? -1L : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
