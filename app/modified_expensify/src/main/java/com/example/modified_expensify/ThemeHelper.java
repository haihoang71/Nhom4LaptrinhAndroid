package com.example.modified_expensify;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ThemeHelper {

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppThemePrefs", Context.MODE_PRIVATE);
        String themeName = prefs.getString("selected_theme", "DynamicTheme1");

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            switch (themeName) {
                case "DynamicTheme2":
                    activity.setTheme(R.style.DynamicTheme2);
                    break;
                case "DynamicTheme3":
                    activity.setTheme(R.style.DynamicTheme3);
                    break;
                case "DynamicTheme4":
                    activity.setTheme(R.style.DynamicTheme4);
                    break;
                default:
                    activity.setTheme(R.style.DynamicTheme1);
                    break;
            }
        }
    }
}

