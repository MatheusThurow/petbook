package com.petbook.app.utils;

import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

public final class SwipeNavigationHelper extends android.view.GestureDetector.SimpleOnGestureListener {

    private static final int MIN_DISTANCE_PX = 96;
    private static final int MIN_VELOCITY_PX = 110;

    private final AppCompatActivity activity;
    private final String currentDestination;
    private final GestureDetectorCompat gestureDetector;

    public SwipeNavigationHelper(AppCompatActivity activity, String currentDestination) {
        this.activity = activity;
        this.currentDestination = currentDestination;
        this.gestureDetector = new GestureDetectorCompat(activity, this);
    }

    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) {
            return false;
        }

        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getY() - e1.getY();

        if (Math.abs(deltaX) < MIN_DISTANCE_PX
                || Math.abs(velocityX) < MIN_VELOCITY_PX
                || Math.abs(deltaX) <= Math.abs(deltaY) * 1.35f) {
            return false;
        }

        boolean forward = deltaX < 0;
        return BottomNavigationHelper.openAdjacentSection(activity, currentDestination, forward);
    }
}
