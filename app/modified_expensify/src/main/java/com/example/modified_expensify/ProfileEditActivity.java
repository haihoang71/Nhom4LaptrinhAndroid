package com.example.modified_expensify;

import static android.Manifest.permission.READ_MEDIA_IMAGES;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

public class ProfileEditActivity extends AppCompatActivity {

    private EditText editTextFullName, editTextBirthDate;
    private ImageView imageViewAvatar;
    private Button buttonSave, buttonChooseImage;
    private ProgressBar progressBar;

    private ProfileDAO profileDAO;
    private ImageHelper imageHelper;
    private UserProfile currentProfile;
    private String avatarBase64 = "";

    private static final int REQUEST_CODE_READ_MEDIA_IMAGES = 101;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Chỉnh sửa thông tin");
        }

        editTextFullName = findViewById(R.id.editTextFullName);
        editTextBirthDate = findViewById(R.id.editTextBirthDate);
        imageViewAvatar = findViewById(R.id.imageViewAvatar);
        buttonSave = findViewById(R.id.buttonSave);
        buttonChooseImage = findViewById(R.id.buttonChooseImage);
        progressBar = findViewById(R.id.progressBar);

        profileDAO = new ProfileDAO(this);
        imageHelper = new ImageHelper(this);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            try {
                                Bitmap selectedImageBitmap = imageHelper.handleImageFromGallery(imageUri);
                                if (selectedImageBitmap != null) {
                                    imageViewAvatar.setImageBitmap(selectedImageBitmap);
                                    avatarBase64 = imageHelper.bitmapToBase64(selectedImageBitmap);
                                }
                            } catch (Exception e) {
                                Toast.makeText(this, "Lỗi khi tải ảnh", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

        editTextBirthDate.setOnClickListener(v -> showDatePickerDialog());
        buttonChooseImage.setOnClickListener(v -> {
            if(checkAndRequestPermission()){
                openImagePicker();
            }else{
                Toast.makeText(ProfileEditActivity.this,
                        "Bạn cần cấp quyền truy cập", Toast.LENGTH_SHORT).show();
            }
        });
        buttonSave.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);

        currentProfile = profileDAO.getLocalProfile();

        if (currentProfile != null) {
            editTextFullName.setText(currentProfile.getFullName());
            editTextBirthDate.setText(currentProfile.getBirthDate());

            if (currentProfile.getAvatar() != null && !currentProfile.getAvatar().equals("default_avatar")) {
                Bitmap avatarBitmap = imageHelper.base64ToBitmap(currentProfile.getAvatar());
                if (avatarBitmap != null) {
                    imageViewAvatar.setImageBitmap(avatarBitmap);
                    avatarBase64 = currentProfile.getAvatar();
                } else {
                    imageViewAvatar.setImageResource(R.drawable.default_avatar);
                }
            } else {
                imageViewAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            createDefaultProfile();
        }

        progressBar.setVisibility(View.GONE);
    }

    private void createDefaultProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String defaultName = email != null ? email.split("@")[0] : "User";
            editTextFullName.setText(defaultName);

            imageViewAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void saveProfile() {
        String fullName = editTextFullName.getText().toString().trim();
        String birthDate = editTextBirthDate.getText().toString().trim();

        if (fullName.isEmpty()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getEmail() != null) {
                fullName = user.getEmail().split("@")[0];
            } else {
                fullName = "User";
            }
        }

        if (avatarBase64.isEmpty() && currentProfile != null && currentProfile.getAvatar() != null) {
            avatarBase64 = currentProfile.getAvatar();
        } else if (avatarBase64.isEmpty()) {
            avatarBase64 = "default_avatar";
        }

        progressBar.setVisibility(View.VISIBLE);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UserProfile updatedProfile = new UserProfile(userId, fullName, birthDate, avatarBase64);

        boolean localSaved = profileDAO.saveLocalProfile(updatedProfile);

        if (localSaved) {
            profileDAO.saveFirebaseProfile(updatedProfile, new ProfileDAO.OnProfileSaveListener() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileEditActivity.this,
                                "Đã lưu thông tin thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ProfileEditActivity.this,
                                "Đã lưu cục bộ nhưng chưa đồng bộ được: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(ProfileEditActivity.this,
                    "Không thể lưu thông tin", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        if (currentProfile != null && currentProfile.getBirthDate() != null && !currentProfile.getBirthDate().isEmpty()) {
            try {
                String[] dateParts = currentProfile.getBirthDate().split("/");
                if (dateParts.length == 3) {
                    day = Integer.parseInt(dateParts[0]);
                    month = Integer.parseInt(dateParts[1]) - 1;
                    year = Integer.parseInt(dateParts[2]);
                }
            } catch (Exception e) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String birthDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    editTextBirthDate.setText(birthDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền để chọn ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean checkAndRequestPermission() {
        if (checkSelfPermission(READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{READ_MEDIA_IMAGES}, REQUEST_CODE_READ_MEDIA_IMAGES);
            return false;
        } else {
            return true;
        }
    }

}