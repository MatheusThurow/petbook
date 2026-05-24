package com.petbook.app.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageUtils {

    private static final int MAX_IMAGE_DIMENSION = 1280;
    private static final int MAX_IMAGE_BYTES = 220 * 1024;

    private ImageUtils() {
    }

    public static String encodeImageAsDataUrl(Context context, Uri imageUri) throws IOException {
        Bitmap bitmap = decodeBitmapRespectingOrientation(context, imageUri);
        if (bitmap == null) {
            throw new IOException("Nao foi possivel ler a imagem selecionada.");
        }

        bitmap = scaleDown(bitmap, MAX_IMAGE_DIMENSION);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int quality = 80;
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        while (outputStream.size() > MAX_IMAGE_BYTES && quality > 35) {
            outputStream.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        }

        if (outputStream.size() > MAX_IMAGE_BYTES) {
            Bitmap smallerBitmap = scaleDown(bitmap, 900);
            outputStream.reset();
            smallerBitmap.compress(Bitmap.CompressFormat.JPEG, 35, outputStream);
            bitmap = smallerBitmap;
        }

        String base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
        return "data:image/jpeg;base64," + base64;
    }

    public static void loadInto(ImageView imageView, String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }

        if (imageValue.startsWith("data:image")) {
            int separatorIndex = imageValue.indexOf(',');
            if (separatorIndex > 0 && separatorIndex < imageValue.length() - 1) {
                String base64 = imageValue.substring(separatorIndex + 1);
                byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            }
        }

        imageView.setImageURI(Uri.parse(imageValue));
    }

    public static void loadInto(Context context, ImageView imageView, Uri imageUri) {
        if (imageUri == null) {
            imageView.setImageDrawable(null);
            return;
        }

        try {
            Bitmap bitmap = decodeBitmapRespectingOrientation(context, imageUri);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                return;
            }
        } catch (IOException ignored) {
            // Fallback abaixo.
        }

        imageView.setImageURI(imageUri);
    }

    private static Bitmap scaleDown(Bitmap bitmap, int maxDimension) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap;
        }

        float scale = Math.min(
                (float) maxDimension / Math.max(width, 1),
                (float) maxDimension / Math.max(height, 1)
        );
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);
    }

    private static Bitmap decodeBitmapRespectingOrientation(Context context, Uri imageUri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        Bitmap bitmap;
        try (InputStream inputStream = contentResolver.openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Nao foi possivel abrir a imagem selecionada.");
            }
            bitmap = BitmapFactory.decodeStream(inputStream);
        }

        if (bitmap == null) {
            return null;
        }

        int rotation = 0;
        try (InputStream exifStream = contentResolver.openInputStream(imageUri)) {
            if (exifStream != null) {
                ExifInterface exifInterface = new ExifInterface(exifStream);
                int orientation = exifInterface.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                );
                rotation = orientationToDegrees(orientation);
            }
        }

        if (rotation == 0) {
            return bitmap;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static int orientationToDegrees(int orientation) {
        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        }
        if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        }
        if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }
}

