package com.example.skincancerai;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String profileId = getInputData().getString("profileId");
        String checkId = getInputData().getString("checkId");
        String resultLabel = getInputData().getString("resultLabel");

        Log.d(TAG, "Backup worker fired, profileId=" + profileId + ", checkId=" + checkId);

        Intent intent = new Intent(getApplicationContext(), ReminderReceiver.class);
        intent.putExtra("profileId", profileId);
        intent.putExtra("checkId", checkId);
        intent.putExtra("resultLabel", resultLabel);

        new ReminderReceiver().onReceive(getApplicationContext(), intent);
        return Result.success();
    }
}
