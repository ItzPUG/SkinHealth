package com.example.skincancerai;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;

public final class UiFeedback {

    private UiFeedback() {
    }

    public static void showActionDialog(
            Activity activity,
            @DrawableRes int iconRes,
            String title,
            String message,
            String primaryText,
            Runnable primaryAction,
            @Nullable String secondaryText,
            @Nullable Runnable secondaryAction
    ) {
        if (activity == null || activity.isFinishing()) return;

        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_action_feedback, null, false);

        ImageView imgIcon = dialogView.findViewById(R.id.imgDialogIcon);
        TextView txtTitle = dialogView.findViewById(R.id.txtDialogTitle);
        TextView txtMessage = dialogView.findViewById(R.id.txtDialogMessage);
        MaterialButton btnPrimary = dialogView.findViewById(R.id.btnPrimary);
        MaterialButton btnSecondary = dialogView.findViewById(R.id.btnSecondary);

        imgIcon.setImageResource(iconRes);
        txtTitle.setText(title);
        txtMessage.setText(message);
        btnPrimary.setText(primaryText);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        btnPrimary.setOnClickListener(v -> {
            dialog.dismiss();
            if (primaryAction != null) primaryAction.run();
        });

        if (secondaryText == null || secondaryText.trim().isEmpty()) {
            btnSecondary.setVisibility(View.GONE);
        } else {
            btnSecondary.setVisibility(View.VISIBLE);
            btnSecondary.setText(secondaryText);
            btnSecondary.setOnClickListener(v -> {
                dialog.dismiss();
                if (secondaryAction != null) secondaryAction.run();
            });
        }

        dialog.show();
    }
}
