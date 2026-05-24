package com.petbook.app.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.petbook.app.repositories.FirebasePushRepository;

import java.util.Map;

public class PetBookFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        FirebasePushRepository.storeLatestToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = null;
        String body = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        Map<String, String> data = remoteMessage.getData();
        if ((title == null || title.trim().isEmpty()) && data != null) {
            title = data.get("title");
        }
        if ((body == null || body.trim().isEmpty()) && data != null) {
            body = data.get("messageText");
        }

        PushNotificationHelper.showNotification(
                getApplicationContext(),
                title,
                body,
                data
        );
    }
}
