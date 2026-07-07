package com.example.skincancerai;

import android.app.Activity;
import android.content.Intent;

public final class PageTransitionHelper {

    private PageTransitionHelper() {
    }

    public static void navigateWithLoading(Activity activity, Intent intent) {
        navigateWithLoading(activity, intent, false);
    }

    public static void navigateWithLoading(Activity activity, Intent intent, boolean finishCurrent) {
        if (activity == null || intent == null || activity.isFinishing()) {
            return;
        }

        activity.startActivity(intent);

        // Dùng bộ anim mềm hơn thay vì slide mạnh
        activity.overridePendingTransition(R.anim.fade_in_quick, R.anim.fade_out_quick);

        if (finishCurrent) {
            activity.finish();
        }
    }

    public static void finishWithAnimation(Activity activity) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        activity.finish();
        activity.overridePendingTransition(R.anim.fade_in_back, R.anim.fade_out_back);
    }
}
