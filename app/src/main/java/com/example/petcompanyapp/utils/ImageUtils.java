package com.example.petcompanyapp.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageUtils {

    private ImageUtils() {
    }

    public static String encodeImageAsDataUrl(Context context, Uri imageUri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        try (InputStream inputStream = contentResolver.openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Nao foi possivel abrir a imagem selecionada.");
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new IOException("Nao foi possivel ler a imagem selecionada.");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            String base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
            return "data:image/jpeg;base64," + base64;
        }
    }

    public static void loadInto(ImageView imageView, String imageValue) {
        if (imageValue == null || imageValue.trim().isEmpty()) {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
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
}
