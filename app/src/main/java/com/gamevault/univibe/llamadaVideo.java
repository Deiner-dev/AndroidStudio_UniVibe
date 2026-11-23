package com.gamevault.univibe;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import androidx.annotation.NonNull;

import java.util.Locale;

import io.livekit.android.renderer.TextureViewRenderer;

public class llamadaVideo extends AppCompatActivity {

    private static final String LIVEKIT_URL = "wss://univibe-oy1p5z3k.livekit.cloud";
    private static final int REQ_PERMISOS = 200;

    private ImageButton btnMute, btnToggleCam, btnEndCall, btnParticipants, btnMore, ibSwitchCam, ibGrid;
    private ShapeableImageView ivPillAvatar;
    private TextView tvPillName, tvPillTimer;
    private TextureViewRenderer rendererRemoto, rendererLocal;

    private boolean isMuted = false;
    private boolean isCameraOn = true;
    private boolean isIncoming = false;
    private boolean isConnected = false;
    private int secondsElapsed = 0;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    private String targetUid, targetUserName, targetPhotoUrl;

    private videoLLamada videoCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_llamada_video);

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

        String[] permisos = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (tienePermisos(permisos)) {
            iniciarVideollamada();
        } else {
            ActivityCompat.requestPermissions(this, permisos, REQ_PERMISOS);
        }
    }

    private boolean tienePermisos(String[] permisos) {
        for (String p : permisos) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQ_PERMISOS) return;
        boolean todos = results.length > 0;
        for (int r : results) {
            if (r != PackageManager.PERMISSION_GRANTED) todos = false;
        }
        if (todos) {
            iniciarVideollamada();
        } else {
            Toast.makeText(this, "Se necesitan cámara y micrófono", Toast.LENGTH_LONG).show();
            terminarLlamada();
        }
    }

    private void iniciarVideollamada() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || TextUtils.isEmpty(targetUid) || rendererRemoto == null || rendererLocal == null) {
            Toast.makeText(this, "No se pudo iniciar la videollamada", Toast.LENGTH_LONG).show();
            terminarLlamada();
            return;
        }

        if (!isIncoming) {
            enviarInvitacionVideoLlamada();
        }

        String miUid = user.getUid();
        // Misma sala para los dos usuarios: se ordenan los UIDs
        String sala = miUid.compareTo(targetUid) < 0
                ? miUid + "_" + targetUid
                : targetUid + "_" + miUid;

        videoCall = new videoLLamada(this);
        videoCall.iniciar(LIVEKIT_URL, sala, miUid, rendererRemoto, rendererLocal,
                new videoLLamada.Listener() {
                    @Override
                    public void onConectado() {
                        // Conectado a la sala; espera a que entre el otro usuario
                    }

                    @Override
                    public void onRemotoEntro() {
                        conectarLlamada();
                    }

                    @Override
                    public void onRemotoSalio() {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(llamadaVideo.this, "El otro usuario colgó",
                                    Toast.LENGTH_SHORT).show();
                            terminarLlamada();
                        }
                    }

                    @Override
                    public void onError(String mensaje) {
                        if (!isFinishing() && !isDestroyed()) {
                            Toast.makeText(llamadaVideo.this, mensaje, Toast.LENGTH_LONG).show();
                            terminarLlamada();
                        }
                    }
                });
    }

    private void enviarInvitacionVideoLlamada() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || TextUtils.isEmpty(targetUid)) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                String myName = snapshot.child("fullName").getValue(String.class);
                String myPhoto = snapshot.child("imgPerfil").getValue(String.class);

                java.util.Map<String, Object> callData = new java.util.HashMap<>();
                callData.put("callerUid", currentUser.getUid());
                callData.put("callerName", myName != null ? myName : "Usuario");
                callData.put("callerPhoto", myPhoto != null ? myPhoto : "");
                callData.put("targetUid", targetUid);
                callData.put("callType", "VIDEO");
                callData.put("status", "RINGING");
                callData.put("timestamp", System.currentTimeMillis());

                DatabaseReference callRef = FirebaseDatabase.getInstance().getReference("llamadas").child(targetUid);
                callRef.setValue(callData);

                callRef.child("status").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot statusSnap) {
                        String status = statusSnap.getValue(String.class);
                        if ("REJECTED".equals(status)) {
                            Toast.makeText(llamadaVideo.this, "Videollamada rechazada", Toast.LENGTH_SHORT).show();
                            callRef.removeValue();
                            terminarLlamada();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void conectarLlamada() {
        if (isConnected || isFinishing() || isDestroyed()) return;
        isConnected = true;
        if (tvPillTimer != null) tvPillTimer.setText("00:00");
        startTimer();
    }

    private void terminarLlamada() {
        // Si yo llamé y el otro nunca contestó, cancelo el aviso
        if (!isIncoming && !isConnected && targetUid != null) {
            FirebaseDatabase.getInstance().getReference("llamadas").child(targetUid).removeValue();
        }
        liberarLlamada();
        finish();
    }

    private void liberarLlamada() {
        if (videoCall != null) {
            videoCall.finalizar();
            videoCall = null;
        }
    }

    private void initViews() {
        rendererRemoto = findViewById(R.id.renderer_remoto);
        rendererLocal = findViewById(R.id.renderer_local);

        ivPillAvatar = findViewById(R.id.iv_video_pill_avatar);
        tvPillName = findViewById(R.id.tv_video_pill_name);
        tvPillTimer = findViewById(R.id.tv_video_pill_timer);

        btnMute = findViewById(R.id.btn_video_mute);
        btnToggleCam = findViewById(R.id.btn_video_toggle_cam);
        btnEndCall = findViewById(R.id.btn_video_end);
        btnParticipants = findViewById(R.id.btn_video_participants);
        btnMore = findViewById(R.id.btn_video_more);
        ibSwitchCam = findViewById(R.id.ib_video_switch_camera);
        ibGrid = findViewById(R.id.ib_video_grid_options);

        if (!TextUtils.isEmpty(targetUserName) && tvPillName != null) {
            tvPillName.setText(targetUserName);
        } else if (tvPillName != null) {
            tvPillName.setText("UniVibe");
        }

        if (tvPillTimer != null) {
            tvPillTimer.setText(isIncoming ? "Conectando..." : "Llamando...");
        }

        if (!TextUtils.isEmpty(targetPhotoUrl) && ivPillAvatar != null) {
            Glide.with(this)
                    .load(targetPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo)
                    .into(ivPillAvatar);
        }
    }

    private void setupListeners() {
        if (btnEndCall != null) {
            btnEndCall.setOnClickListener(v -> {
                Toast.makeText(this, "Videollamada finalizada", Toast.LENGTH_SHORT).show();
                terminarLlamada();
            });
        }

        if (btnMute != null) {
            btnMute.setOnClickListener(v -> {
                isMuted = !isMuted;
                if (videoCall != null) videoCall.silenciar(isMuted);
                Toast.makeText(this, isMuted ? "Micrófono silenciado" : "Micrófono activado",
                        Toast.LENGTH_SHORT).show();
            });
        }

        if (btnToggleCam != null) {
            btnToggleCam.setOnClickListener(v -> {
                isCameraOn = !isCameraOn;
                if (videoCall != null) videoCall.camaraActiva(isCameraOn);
                if (rendererLocal != null) {
                    rendererLocal.setVisibility(isCameraOn
                            ? android.view.View.VISIBLE : android.view.View.INVISIBLE);
                }
                Toast.makeText(this, isCameraOn ? "Cámara encendida" : "Cámara apagada",
                        Toast.LENGTH_SHORT).show();
            });
        }

        if (ibSwitchCam != null) {
            ibSwitchCam.setOnClickListener(v -> {
                if (videoCall != null) videoCall.cambiarCamara();
            });
        }

        if (btnParticipants != null) {
            btnParticipants.setOnClickListener(v ->
                    Toast.makeText(this, "Participantes", Toast.LENGTH_SHORT).show());
        }

        if (btnMore != null) {
            btnMore.setOnClickListener(v ->
                    Toast.makeText(this, "Más opciones", Toast.LENGTH_SHORT).show());
        }

        if (ibGrid != null) {
            ibGrid.setOnClickListener(v ->
                    Toast.makeText(this, "Diseño de cuadrícula", Toast.LENGTH_SHORT).show());
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int mins = secondsElapsed / 60;
                int secs = secondsElapsed % 60;
                if (tvPillTimer != null) {
                    tvPillTimer.setText(String.format(Locale.US, "%02d:%02d", mins, secs));
                }
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        liberarLlamada();
    }
}