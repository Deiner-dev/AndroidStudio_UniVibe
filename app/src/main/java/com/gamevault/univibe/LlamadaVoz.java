package com.gamevault.univibe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LlamadaVoz extends AppCompatActivity {

    private static final String LIVEKIT_URL = "wss://univibe-oy1p5z3k.livekit.cloud";
    private static final int REQ_MIC = 100;

    private ImageButton ibBack, btnMute, btnSpeaker, btnEndCall, btnSwitchVideo;
    private ShapeableImageView ivCallerAvatar;
    private TextView tvCallerName, tvCallTimer;

    private boolean isMuted = false;
    private boolean isSpeakerOn = true;
    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private MediaPlayer ringingMediaPlayer;
    private boolean isConnected = false;

    private String targetUid, targetUserName, targetPhotoUrl;
    private boolean isIncoming = false;

    private llamadas llamada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_llamada_voz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        targetUid = getIntent().getStringExtra("targetUid");
        targetUserName = getIntent().getStringExtra("targetUserName");
        targetPhotoUrl = getIntent().getStringExtra("targetPhotoUrl");
        isIncoming = getIntent().getBooleanExtra("isIncoming", false);

        initViews();
        setupListeners();
        playRingingSound();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarLlamada();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarLlamada();
            } else {
                Toast.makeText(this, "Se necesita el permiso del micrófono", Toast.LENGTH_LONG).show();
                terminarLlamada();
            }
        }
    }

    private void iniciarLlamada() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(targetUid)) {
            Toast.makeText(this, "No se pudo iniciar la llamada", Toast.LENGTH_LONG).show();
            terminarLlamada();
            return;
        }

        if (!isIncoming) {
            enviarInvitacionLlamada();
        }

        String miUid = user.getUid();
        // Misma sala para ambos usuarios: se ordenan los UIDs
        String sala = miUid.compareTo(targetUid) < 0
                ? miUid + "_" + targetUid
                : targetUid + "_" + miUid;

        llamada = new llamadas(this);
        llamada.iniciar(LIVEKIT_URL, sala, miUid, new llamadas.Listener() {
            @Override
            public void onConectado() {
                // Conectado a la sala; sigue sonando hasta que entre el otro usuario
            }

            @Override
            public void onRemotoEntro() {
                conectarLlamada();
            }

            @Override
            public void onRemotoSalio() {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(LlamadaVoz.this, "El otro usuario colgó", Toast.LENGTH_SHORT).show();
                    terminarLlamada();
                }
            }

            @Override
            public void onError(String mensaje) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(LlamadaVoz.this, mensaje, Toast.LENGTH_LONG).show();
                    terminarLlamada();
                }
            }
        });
    }

    private void enviarInvitacionLlamada() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(targetUid)) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String myName = snapshot.child("fullName").getValue(String.class);
                String myPhoto = snapshot.child("imgPerfil").getValue(String.class);

                Map<String, Object> callData = new HashMap<>();
                callData.put("callerUid", currentUser.getUid());
                callData.put("callerName", myName != null ? myName : "Usuario");
                callData.put("callerPhoto", myPhoto != null ? myPhoto : "");
                callData.put("targetUid", targetUid);
                callData.put("callType", "VOICE");
                callData.put("status", "RINGING");
                callData.put("timestamp", System.currentTimeMillis());

                DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("llamadas").child(targetUid);
                callRef.setValue(callData);

                callRef.child("status").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot statusSnap) {
                        String status = statusSnap.getValue(String.class);
                        if ("REJECTED".equals(status)) {
                            stopRingingSound();
                            Toast.makeText(LlamadaVoz.this, "Llamada rechazada", Toast.LENGTH_SHORT).show();
                            callRef.removeValue();
                            terminarLlamada();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void terminarLlamada() {
        stopRingingSound();
        liberarLlamada();
        finish();
    }

    private void liberarLlamada() {
        if (llamada != null) {
            llamada.finalizar();
            llamada = null;
        }
    }

    private void playRingingSound() {
        try {
            int audioRes = isIncoming ? R.raw.llamada_entrante : R.raw.sonido_de_llamada_saliente;
            ringingMediaPlayer = MediaPlayer.create(this, audioRes);
            if (ringingMediaPlayer != null) {
                ringingMediaPlayer.setLooping(true);
                ringingMediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopRingingSound() {
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

    private void conectarLlamada() {
        if (isConnected || isFinishing() || isDestroyed()) return;
        isConnected = true;
        stopRingingSound();

        if (tvCallTimer != null) {
            tvCallTimer.setText("00:00");
        }
        startTimer();
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_call);
        ivCallerAvatar = findViewById(R.id.iv_caller_avatar);
        tvCallerName = findViewById(R.id.tv_caller_name);
        tvCallTimer = findViewById(R.id.tv_call_timer_voice);

        btnMute = findViewById(R.id.btn_voice_mute);
        btnSpeaker = findViewById(R.id.btn_voice_speaker);
        btnEndCall = findViewById(R.id.btn_voice_end);
        btnSwitchVideo = findViewById(R.id.btn_voice_switch_video);

        if (!TextUtils.isEmpty(targetUserName) && tvCallerName != null) {
            tvCallerName.setText(targetUserName);
        }

        if (tvCallTimer != null) {
            tvCallTimer.setText(isIncoming ? "Llamada entrante..." : "Llamando...");
        }

        if (!TextUtils.isEmpty(targetPhotoUrl) && ivCallerAvatar != null) {
            Glide.with(this)
                    .load(targetPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(ivCallerAvatar);
        }
    }

    private void setupListeners() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> terminarLlamada());
        }

        if (btnEndCall != null) {
            btnEndCall.setOnClickListener(v -> {
                Toast.makeText(this, "Llamada finalizada", Toast.LENGTH_SHORT).show();
                terminarLlamada();
            });
        }

        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (llamada != null) llamada.silenciar(isMuted);
                Toast.makeText(this, isMuted ? "Micrófono silenciado" : "Micrófono activado",
                        Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSpeaker != null) {
            btnSpeaker.setOnClickListener(v -> {
                isSpeakerOn = !isSpeakerOn;
                if (llamada != null) llamada.altavoz(isSpeakerOn);
                Toast.makeText(this, isSpeakerOn ? "Altavoz activado" : "Altavoz desactivado",
                        Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSwitchVideo != null) {
            btnSwitchVideo.setOnClickListener(v -> {
                stopRingingSound();
                liberarLlamada();
                Intent intent = new Intent(this, LlamadaActivity.class);
                intent.putExtra("targetUid", targetUid);
                intent.putExtra("callType", "VIDEO");
                startActivity(intent);
                finish();
            });
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int mins = secondsElapsed / 60;
                int secs = secondsElapsed % 60;
                if (tvCallTimer != null) {
                    tvCallTimer.setText(String.format(Locale.US, "%02d:%02d", mins, secs));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingingSound();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        liberarLlamada();
    }
}
