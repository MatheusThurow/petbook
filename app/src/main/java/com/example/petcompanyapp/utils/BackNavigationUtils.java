package com.petbook.app.utils;

import android.content.Intent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.petbook.app.activities.FeedActivity;
import com.petbook.app.activities.LoginActivity;

public final class BackNavigationUtils {

    private BackNavigationUtils() {
    }

    public static void bind(AppCompatActivity activity) {
        bind(activity, null);
    }

    public static void bind(AppCompatActivity activity, View backView) {
        if (backView != null) {
            backView.setOnClickListener(v -> navigateBack(activity));
        }

        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBack(activity);
            }
        });
    }

    public static void navigateBack(AppCompatActivity activity) {
        if (SessionUtils.isAuthenticated(activity)) {
            if (activity instanceof FeedActivity) {
                return;
            }

            if (activity.isTaskRoot()) {
                openFeed(activity);
                return;
            }

            activity.finish();
            return;
        }

        if (activity.isTaskRoot()) {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
            return;
        }

        activity.finish();
    }

    public static void openFeed(AppCompatActivity activity) {
        Intent intent = new Intent(activity, FeedActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
