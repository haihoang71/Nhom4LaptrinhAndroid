package com.example.modified_expensify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ImageHelper {
    private static final String TAG = "ImageHelper";
    private final Context context;
    private final int MAX_IMAGE_SIZE = 500; // Kích thước tối đa cho avatar (pixel)

    public ImageHelper(Context context) {
        this.context = context;
    }

    /**
     * Chuyển đổi Bitmap thành chuỗi Base64
     */
    public String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * Chuyển đổi chuỗi Base64 thành Bitmap
     */
    public Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting Base64 to Bitmap", e);
            return null;
        }
    }

    /**
     * Resize bitmap để giới hạn kích thước
     */
    public Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / height;

        if (width > height) {
            if (width > MAX_IMAGE_SIZE) {
                width = MAX_IMAGE_SIZE;
                height = (int) (width / ratio);
            }
        } else {
            if (height > MAX_IMAGE_SIZE) {
                height = MAX_IMAGE_SIZE;
                width = (int) (height * ratio);
            }
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    /**
     * Xử lý Uri ảnh từ thư viện và chuyển thành Bitmap
     */
    public Bitmap handleImageFromGallery(Uri imageUri) {
        try {
            InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap selectedBitmap = BitmapFactory.decodeStream(imageStream);
            return resizeBitmap(selectedBitmap);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found", e);
            return null;
        }
    }

    /**
     * Lấy Bitmap từ tài nguyên drawable
     */
    public Bitmap getDefaultAvatar() {
        return BitmapFactory.decodeResource(context.getResources(), R.drawable.default_avatar);
    }
}