package com.example.petcompanyapp.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class AsyncRunner {

    private static final ExecutorService IO_EXECUTOR = Executors.newCachedThreadPool();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AsyncRunner() {
    }

    public static <T> void run(
            Callable<T> task,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        IO_EXECUTOR.execute(() -> {
            try {
                T result = task.call();
                MAIN_HANDLER.post(() -> onSuccess.accept(result));
            } catch (Exception exception) {
                MAIN_HANDLER.post(() -> onError.accept(exception));
            }
        });
    }
}
