package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingAuthActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnLogin;
    private MaterialButton btnRegister;

    private final List<OnboardingAuthPage> pages = new ArrayList<>();
    private OnboardingAuthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_auth);

        viewPager = findViewById(R.id.viewPager);
        layoutDots = findViewById(R.id.layoutDots);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        setupPages();
        setupDots();
        setupActions();
    }

    private void setupPages() {
        pages.add(new OnboardingAuthPage(
                R.drawable.onboard_health_1,
                "Xem hồ sơ sức khỏe cá nhân",
                "Quản lý hồ sơ sức khỏe, kết quả khám chữa bệnh và theo dõi lâu dài ngay trên ứng dụng."
        ));

        pages.add(new OnboardingAuthPage(
                R.drawable.onboard_health_2,
                "Theo dõi và sàng lọc thuận tiện",
                "Lưu lịch sử quét, tái kiểm tra đúng hẹn và xem lại kết quả một cách trực quan hơn."
        ));

        adapter = new OnboardingAuthAdapter(pages);
        viewPager.setAdapter(adapter);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);
            }
        });
    }

    private void setupDots() {
        layoutDots.removeAllViews();

        for (int i = 0; i < pages.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    dp(i == 0 ? 24 : 12),
                    dp(8)
            );
            lp.setMargins(dp(4), 0, dp(4), 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == 0
                    ? R.drawable.dot_indicator_active
                    : R.drawable.dot_indicator_inactive);
            layoutDots.addView(dot);
        }
    }

    private void updateDots(int activePosition) {
        for (int i = 0; i < layoutDots.getChildCount(); i++) {
            View dot = layoutDots.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = dp(i == activePosition ? 24 : 12);
            lp.height = dp(8);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i == activePosition
                    ? R.drawable.dot_indicator_active
                    : R.drawable.dot_indicator_inactive);
        }
    }

    private void setupActions() {
        btnLogin.setOnClickListener(v -> {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, LoginActivity.class)
            );
        });

        btnRegister.setOnClickListener(v -> {
            PageTransitionHelper.navigateWithLoading(
                    this,
                    new Intent(this, RegisterActivity.class)
            );
        });
    }

    private void markOnboardingShown() {
        getSharedPreferences("onboarding_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_shown", true)
                .apply();
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}
