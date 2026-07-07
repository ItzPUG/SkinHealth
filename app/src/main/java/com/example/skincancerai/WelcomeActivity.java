package com.example.skincancerai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        TextView btnCreate = findViewById(R.id.btnCreate);

        btnLogin.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, LoginActivity.class)
                ));

        btnCreate.setOnClickListener(v ->
                PageTransitionHelper.navigateWithLoading(
                        this,
                        new Intent(this, RegisterActivity.class)
                ));
    }
}
