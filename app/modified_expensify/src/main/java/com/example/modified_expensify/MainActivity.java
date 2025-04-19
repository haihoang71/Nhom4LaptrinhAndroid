package com.example.modified_expensify;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applySavedTheme(this); // nếu có
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNav = findViewById(R.id.bottomNav);

        setupViewPager();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_expenses) {
                viewPager.setCurrentItem(1);
                return true;
            } else if (itemId == R.id.navigation_analysis) {
                viewPager.setCurrentItem(2);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                viewPager.setCurrentItem(3);
                return true;
            } else {
                return false;
            }
        });

        // Đồng bộ khi vuốt
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                bottomNav.getMenu().getItem(position).setChecked(true);
            }
        });
    }

    private void setupViewPager() {
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(new MainFragment());
        adapter.addFragment(new ShowExpendFragment());
        adapter.addFragment(new AnalysisFragment());
        adapter.addFragment(new SettingsFragment());
        viewPager.setAdapter(adapter);
    }
}
