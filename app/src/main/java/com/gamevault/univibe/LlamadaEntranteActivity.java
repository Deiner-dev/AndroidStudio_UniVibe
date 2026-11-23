package com.gamevault.univibe;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LlamadaEntranteActivity extends AppCompatActivity {

    private ShapeableImageView ivAvatar;
    private TextView tvName, tvCallType;
    private ImageButton btnAccept, btnReject;

    private MediaPlayer ringingMediaPlayer;
    private String callerUid, callerName, callerPhoto, callType;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_llamada_entrante);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        callerUid = getIntent().getStringExtra("callerUid");
        callerName = getIntent().getStringExtra("callerName");
        callerPhoto = getIntent().getStringExtra("callerPhoto");
        callType = getIntent().getStringExtra("callType");

        initViews();
        setupListeners();
        playRingtone();
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_incoming_caller_avatar);
        tvName = findViewById(R.id.tv_incoming_caller_name);
        tvCallType = findViewById(R.id.tv_incoming_call_type);
        btnAccept = findViewById(R.id.btn_accept_call);
        btnReject = findViewById(R.id.btn_reject_call);

        if (!TextUtils.isEmpty(callerName) && tvName != null) {
            tvName.setText(callerName);
        }

        if (!TextUtils.isEmpty(callerPhoto) && ivAvatar != null) {
            Glide.with(this)
                    .load(callerPhoto)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(ivAvatar);
        }

        if (tvCallType != null) {
            tvCallType.setText("VIDEO".equals(callType) ? "Videollamada entrante..." : "Llamada de voz entrante...");
        }
    }

    private void setupListeners() {
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> aceptarLlamada());
        }

        if (btnReject != null) {
            btnReject.setOnClickListener(v -> rechazarLlamada());
        }
    }

    private void playRingtone() {
        try {
            ringingMediaPlayer = MediaPlayer.create(this, R.raw.llamada_entrante);
            if (ringingMediaPlayer != null) {
                ringingMediaPlayer.setLooping(true);
                ringingMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        try {
            if (ringingMediaPlayer != null) {
                if (ringingMediaPlayer.isPlaying()) {
                    ringingMediaPlayer.stop();
                }
                ringingMediaPlayer.release();
                ringingMediaPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void aceptarLlamada() {
        stopRingtone();
        if (!TextUtils.isEmpty(currentUserId)) {
            DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("llamadas").child(currentUserId);
            callRef.child("status").setValue("ACCEPTED");
        }

        Intent intent;
        if ("VIDEO".equals(callType)) {
            intent = new Intent(this, LlamadaActivity.class);
        } else {
            intent = new Intent(this, LlamadaVoz.class);
        }
        intent.putExtra("targetUid", callerUid);
        intent.putExtra("targetUserName", callerName);
        intent.putExtra("targetPhotoUrl", callerPhoto);
        startActivity(intent);
        finish();
    }

    private void rechazarLlamada() {
        stopRingtone();
        if (!TextUtils.isEmpty(currentUserId)) {
            DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("llamadas").child(currentUserId);
            callRef.child("status").setValue("REJECTED");
            callRef.removeValue();
        }
        Toast.makeText(this, "Llamada rechazada", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }
}
