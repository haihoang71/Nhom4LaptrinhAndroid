package com.example.modified_expensify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileDAO {
    private static final String TAG = "ProfileDAO";
    private DBHelper dbHelper;
    private Context context;
    private DatabaseReference userProfileRef;
    private FirebaseUser currentUser;

    public ProfileDAO(Context context) {
        this.context = context;
        this.dbHelper = new DBHelper(context);
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            this.userProfileRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(userId).child("Profile");
        }
    }

    /**
     * Lấy thông tin profile từ SQLite
     */
    public UserProfile getLocalProfile() {
        UserProfile profile = null;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (currentUser == null) return null;

        String userId = currentUser.getUid();

        String[] projection = {
                DBHelper.COLUMN_USER_ID,
                DBHelper.COLUMN_USER_NAME,
                DBHelper.COLUMN_BIRDTH_DATE,
                DBHelper.COLUMN_AVATAR
        };

        String selection = DBHelper.COLUMN_USER_ID + " = ?";
        String[] selectionArgs = {userId};

        try {
            Cursor cursor = db.query(
                    DBHelper.TABLE_PROFILE,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                String fullName = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_USER_NAME));
                String birthDate = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_BIRDTH_DATE));
                String avatar = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COLUMN_AVATAR));

                profile = new UserProfile(userId, fullName, birthDate, avatar);
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local profile", e);
        } finally {
            db.close();
        }

        return profile;
    }

    /**
     * Lưu thông tin profile vào SQLite
     */
    public boolean saveLocalProfile(UserProfile profile) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        boolean success = false;

        try {
            ContentValues values = new ContentValues();
            values.put(DBHelper.COLUMN_USER_ID, currentUser.getUid());
            values.put(DBHelper.COLUMN_USER_NAME, profile.getFullName());
            values.put(DBHelper.COLUMN_BIRDTH_DATE, profile.getBirthDate());
            values.put(DBHelper.COLUMN_AVATAR, profile.getAvatar());

            // Kiểm tra xem profile đã tồn tại chưa
            Cursor cursor = db.query(
                    DBHelper.TABLE_PROFILE,
                    new String[]{DBHelper.COLUMN_USER_ID},
                    DBHelper.COLUMN_USER_ID + " = ?",
                    new String[]{currentUser.getUid()},
                    null, null, null
            );

            if (cursor != null && cursor.getCount() > 0) {
                // Cập nhật profile nếu đã tồn tại
                db.update(
                        DBHelper.TABLE_PROFILE,
                        values,
                        DBHelper.COLUMN_USER_ID + " = ?",
                        new String[]{currentUser.getUid()}
                );
            } else {
                // Thêm mới profile nếu chưa tồn tại
                db.insert(DBHelper.TABLE_PROFILE, null, values);
            }

            if (cursor != null) {
                cursor.close();
            }

            success = true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving local profile", e);
        } finally {
            db.close();
        }

        return success;
    }

    /**
     * Lưu thông tin profile lên Firebase
     */
    public void saveFirebaseProfile(UserProfile profile, final OnProfileSaveListener listener) {
        if (userProfileRef == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }

        userProfileRef.setValue(profile)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e.getMessage());
                    }
                });
    }

    /**
     * Lấy thông tin profile từ Firebase
     */
    public void getFirebaseProfile(final OnProfileLoadListener listener) {
        if (userProfileRef == null) {
            if (listener != null) {
                listener.onFailure("User not authenticated");
            }
            return;
        }

        userProfileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserProfile profile = dataSnapshot.getValue(UserProfile.class);
                if (profile == null) {
                    // Nếu không có profile, tạo profile mặc định
                    profile = createDefaultProfile();
                }

                if (listener != null) {
                    listener.onProfileLoaded(profile);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (listener != null) {
                    listener.onFailure(databaseError.getMessage());
                }
            }
        });
    }

    /**
     * Đồng bộ hóa profile giữa Firebase và SQLite
     */
    public void syncProfile(final OnProfileSyncListener listener) {
        if (currentUser == null) {
            if (listener != null) {
                listener.onSyncFailure("User not authenticated");
            }
            return;
        }

        // Lấy profile từ Firebase
        getFirebaseProfile(new OnProfileLoadListener() {
            @Override
            public void onProfileLoaded(UserProfile firebaseProfile) {
                // Lấy profile từ SQLite
                UserProfile localProfile = getLocalProfile();

                if (localProfile == null) {
                    // Nếu không có local profile, lưu Firebase profile xuống SQLite
                    saveLocalProfile(firebaseProfile);
                    if (listener != null) {
                        listener.onSyncSuccess();
                    }
                } else {
                    // So sánh timestamp hoặc logic khác để quyết định đồng bộ
                    // Trong trường hợp đơn giản, có thể ưu tiên dữ liệu từ Firebase
                    saveLocalProfile(firebaseProfile);
                    if (listener != null) {
                        listener.onSyncSuccess();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                // Nếu không lấy được từ Firebase, sử dụng dữ liệu cục bộ nếu có
                UserProfile localProfile = getLocalProfile();
                if (localProfile != null) {
                    // Thử đẩy lên Firebase
                    saveFirebaseProfile(localProfile, new OnProfileSaveListener() {
                        @Override
                        public void onSuccess() {
                            if (listener != null) {
                                listener.onSyncSuccess();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (listener != null) {
                                listener.onSyncFailure(errorMessage);
                            }
                        }
                    });
                } else {
                    // Không có dữ liệu ở cả hai nơi
                    if (listener != null) {
                        listener.onSyncFailure(errorMessage);
                    }
                }
            }
        });
    }

    /**
     * Tạo profile mặc định khi người dùng chưa có thông tin
     */
    private UserProfile createDefaultProfile() {
        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        String defaultName = email != null ? email.split("@")[0] : "User";
        String defaultBirthDate = "";
        String defaultAvatar = "default_avatar";

        return new UserProfile(userId, defaultName, defaultBirthDate, defaultAvatar);
    }

    // Interface để xử lý callbacks
    public interface OnProfileLoadListener {
        void onProfileLoaded(UserProfile profile);
        void onFailure(String errorMessage);
    }

    public interface OnProfileSaveListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnProfileSyncListener {
        void onSyncSuccess();
        void onSyncFailure(String errorMessage);
    }
}