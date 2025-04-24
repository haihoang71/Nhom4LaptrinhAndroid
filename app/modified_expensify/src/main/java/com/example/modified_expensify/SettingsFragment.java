package com.example.modified_expensify;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class SettingsFragment extends Fragment {

    private ProfileDAO profileDAO;
    private ImageHelper imageHelper;
    private ImageView imgUserAvatar;
    private TextView tvUserName;
    private Button bntEditProfile;
    private LinearLayout itemLogout, itemChangeLanguage, itemChangeColor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        profileDAO = new ProfileDAO(requireContext());
        imageHelper = new ImageHelper(requireContext());

        tvUserName = view.findViewById(R.id.user_details);
        imgUserAvatar = view.findViewById(R.id.imgUserAvatar);
        bntEditProfile = view.findViewById(R.id.bntEditProfile);
        itemLogout = view.findViewById(R.id.itemLogout);
        itemChangeLanguage = view.findViewById(R.id.itemChangeLanguage);
        itemChangeColor = view.findViewById(R.id.itemChangeColor);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(requireContext(), Login.class));
            requireActivity().finish();
        } else {
            tvUserName.setText(getString(R.string.hello_user, user.getEmail()));
        }

        bntEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), ProfileEditActivity.class));
        });

        itemChangeColor.setOnClickListener(v -> showThemeDialog());

        itemChangeLanguage.setOnClickListener(v -> showLanguageDialog());

        itemLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), Login.class));
            requireActivity().finish();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void loadUserProfile() {
        UserProfile localProfile = profileDAO.getLocalProfile();

        if (localProfile != null) {
            displayUserInfo(localProfile);
        }

        profileDAO.syncProfile(new ProfileDAO.OnProfileSyncListener() {
            @Override
            public void onSyncSuccess() {
                UserProfile updatedProfile = profileDAO.getLocalProfile();
                if (updatedProfile != null) {
                    requireActivity().runOnUiThread(() -> displayUserInfo(updatedProfile));
                }
            }

            @Override
            public void onSyncFailure(String errorMessage) {
                // Có thể log nếu cần
            }
        });
    }

    private void displayUserInfo(UserProfile profile) {
        tvUserName.setText("Hello " + profile.getFullName());

        if (profile.getAvatar() != null && !profile.getAvatar().equals("default_avatar")) {
            Bitmap avatarBitmap = imageHelper.base64ToBitmap(profile.getAvatar());
            if (avatarBitmap != null) {
                imgUserAvatar.setImageBitmap(avatarBitmap);
            } else {
                imgUserAvatar.setImageResource(R.drawable.default_avatar);
            }
        } else {
            imgUserAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    private void showLanguageDialog() {
        String[] langs = {"Tiếng Việt", "English", "日本語"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Chọn ngôn ngữ")
                .setItems(langs, (dialog, which) -> {
                    if (which == 0) setLocale("vi");
                    else if (which == 1) setLocale("en");
                    else setLocale("ja");
                }).show();
    }

    private void showThemeDialog() {
        String[] themes = getResources().getStringArray(R.array.theme_names);
        String[] THEME_KEYS = {"DynamicTheme4", "DynamicTheme2", "DynamicTheme3", "DynamicTheme1"};

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_theme))
                .setItems(themes, (dialog, which) -> {
                    String selectedTheme = THEME_KEYS[which];
                    SharedPreferences prefs = requireContext().getSharedPreferences("AppThemePrefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("selected_theme", selectedTheme).apply();

                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void setLocale(String langCode) {
        SharedPreferences prefs = requireContext().getSharedPreferences("Settings", Context.MODE_PRIVATE);
        prefs.edit().putString("My_Lang", langCode).apply();

        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
