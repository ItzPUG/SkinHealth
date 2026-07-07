package com.example.skincancerai;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SkinCheckRepository {

    public static void toggleReminder(
            Context ctx,
            String profileId,
            SkinCheck check,
            boolean enable,
            int days
    ) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || profileId == null || check == null || check.id == null) {
            return;
        }

        DatabaseReference ref =
                FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(uid)
                        .child("medical_profiles")
                        .child(profileId)
                        .child("skin_checks")
                        .child(check.id);

        ref.child("reminderEnabled").setValue(enable);
        ref.child("reminderDays").setValue(days);
        long reminderAt = enable
                ? (System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L))
                : 0L;
        ref.child("reminderAt").setValue(reminderAt);

        if (enable) {
            ReminderService.scheduleAt(
                    ctx,
                    profileId,
                    check.id,
                    check.resultLabel,
                    reminderAt
            );
        } else {
            ReminderService.cancel(ctx, profileId, check.id);
        }
    }
}
