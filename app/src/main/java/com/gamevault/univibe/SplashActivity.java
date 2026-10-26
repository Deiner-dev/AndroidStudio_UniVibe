package com.gamevault.univibe;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvStatus;
    private TextView tvPercent;
    private int progressStatus = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        progressBar = findViewById(R.id.progress_bar_loading);
        tvStatus = findViewById(R.id.tv_splash_status);
        tvPercent = findViewById(R.id.tv_progress_percent);

        startLoadingSimulation();
    }

    private void startLoadingSimulation() {
        new Thread(() -> {
            while (progressStatus < 100) {
                progressStatus += 2; // Increments by 2%

                handler.post(() -> {
                    progressBar.setProgress(progressStatus);
                    String percentStr = progressStatus + "%";
                    tvPercent.setText(percentStr);

                    if (progressStatus < 40) {
                        tvStatus.setText(R.string.splash_loading_resources);
                    } else if (progressStatus < 80) {
                        tvStatus.setText(R.string.splash_checking_session);
                    } else {
                        tvStatus.setText(R.string.splash_loaded_success);
                    }
                });

                try {
                    Thread.sleep(35); // Total duration ~ 1.75 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Loading finished, navigate to next screen
            handler.postDelayed(this::navigateToNextScreen, 300);
        }).start();
    }

    private void navigateToNextScreen() {
        if (isFinishing() || isDestroyed()) return;

        Intent intent;
        if (mAuth.getCurrentUser() != null) {
            // User is already logged in, go to main panel
            intent = new Intent(SplashActivity.this, panelPrincipal.class);
        } else {
            // User needs to log in, go to MainActivity (Login)
            intent = new Intent(SplashActivity.this, MainActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
