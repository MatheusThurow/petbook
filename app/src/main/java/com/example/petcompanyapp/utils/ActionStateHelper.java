package com.petbook.app.utils;

import android.view.View;
import android.widget.TextView;

public final class ActionStateHelper {

    private ActionStateHelper() {
    }

    public static void setLoading(
            TextView actionView,
            boolean isLoading,
            CharSequence idleText,
            CharSequence loadingText
    ) {
        if (actionView == null) {
            return;
        }

        actionView.setEnabled(!isLoading);
        actionView.setClickable(!isLoading);
        actionView.setAlpha(isLoading ? 0.7f : 1f);
        actionView.setText(isLoading ? loadingText : idleText);
    }

    public static void setLoading(View actionView, boolean isLoading) {
        if (actionView == null) {
            return;
        }

        actionView.setEnabled(!isLoading);
        actionView.setClickable(!isLoading);
        actionView.setAlpha(isLoading ? 0.7f : 1f);
    }

    public static void setEnabled(boolean isEnabled, View... views) {
        if (views == null) {
            return;
        }

        for (View view : views) {
            if (view == null) {
                continue;
            }
            view.setEnabled(isEnabled);
            view.setClickable(isEnabled);
            view.setAlpha(isEnabled ? 1f : 0.7f);
        }
    }
}
