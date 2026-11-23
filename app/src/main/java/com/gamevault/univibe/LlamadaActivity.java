package com.gamevault.univibe;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class LlamadaActivity extends AppCompatActivity {

    private TextView tvPeerName, tvStatus, tvTimer;
    private ImageButton btnToggleMic, btnToggleCam, btnSwitchCam, btnEndCall;
    private String targetUid, callType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_llamada);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        targetUid = getIntent().getStringExtra("targetUid");
        callType = getIntent().getStringExtra("callType"); // "VOICE" or "VIDEO"

        tvPeerName = findViewById(R.id.tv_call_peer_name);
        tvStatus = findViewById(R.id.tv_call_status);
        tvTimer = findViewById(R.id.tv_call_timer);

        btnToggleMic = findViewById(R.id.btn_toggle_mic);
        btnToggleCam = findViewById(R.id.btn_toggle_cam);
        btnSwitchCam = findViewById(R.id.btn_switch_cam);
        btnEndCall = findViewById(R.id.btn_end_call);

        if (btnEndCall != null) {
            btnEndCall.setOnClickListener(v -> finish());
        }

        if (btnToggleMic != null) {
            btnToggleMic.setOnClickListener(v -> Toast.makeText(this, "Micrófono alternado", Toast.LENGTH_SHORT).show());
        }

        if (btnToggleCam != null) {
            btnToggleCam.setOnClickListener(v -> Toast.makeText(this, "Cámara alternada", Toast.LENGTH_SHORT).show());
        }

        if (btnSwitchCam != null) {
            btnSwitchCam.setOnClickListener(v -> Toast.makeText(this, "Cambiar cámara", Toast.LENGTH_SHORT).show());
        }
    }
}
