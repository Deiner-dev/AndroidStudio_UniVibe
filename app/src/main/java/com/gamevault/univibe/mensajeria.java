package com.gamevault.univibe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class mensajeria extends AppCompatActivity {

    private ImageButton ibBack;
    private EditText etSearchChats;
    private RecyclerView rvConversationsList;
    private TextView tvEmptyConversations;

    private FirebaseAuth mAuth;
    private DatabaseReference conversationsRef;
    private ValueEventListener conversationsListener;
    private String currentUserId = "";

    private List<Conversacion> masterConversacionesList;
    private List<Conversacion> displayConversacionesList;
    private ConversacionesAdapter adapter;

    private Uri cameraImageUri = null;

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    abrirCamaraCaptura();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), isSuccess -> {
                if (isSuccess && cameraImageUri != null) {
                    Intent intent = new Intent(this, RecortarImagenActivity.class);
                    intent.putExtra("imageUri", cameraImageUri.toString());
                    startActivity(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_mensajeria);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        initViews();
        initFirebase();
        setupListeners();
        cargarConversaciones();
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_messages);
        etSearchChats = findViewById(R.id.et_search_chats);
        rvConversationsList = findViewById(R.id.rv_conversations_list);
        tvEmptyConversations = findViewById(R.id.tv_empty_conversations);

        masterConversacionesList = new ArrayList<>();
        displayConversacionesList = new ArrayList<>();
        adapter = new ConversacionesAdapter(this, displayConversacionesList);

        if (rvConversationsList != null) {
            rvConversationsList.setLayoutManager(new LinearLayoutManager(this));
            rvConversationsList.setAdapter(adapter);
            rvConversationsList.setNestedScrollingEnabled(false);
        }
    }

    private void initFirebase() {
        conversationsRef = FirebaseDatabase.getInstance().getReference("conversaciones").child(currentUserId);
    }

    private void setupListeners() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (etSearchChats != null) {
            etSearchChats.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarConversaciones(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.ll_nav_inicio);
        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                Intent intent = new Intent(this, panelPrincipal.class);
                startActivity(intent);
                finish();
            });
        }

        View navSearch = findViewById(R.id.ll_nav_buscar);
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> Toast.makeText(this, R.string.nav_search, Toast.LENGTH_SHORT).show());
        }

        View navAdd = findViewById(R.id.fl_nav_center);
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> solicitarCamaraOCapturar());
        }

        View navAlerts = findViewById(R.id.ll_nav_alertas);
        if (navAlerts != null) {
            navAlerts.setOnClickListener(v -> {
                Intent intent = new Intent(this, alertas.class);
                startActivity(intent);
                finish();
            });
        }

        View navPerfil = findViewById(R.id.ll_nav_perfil);
        if (navPerfil != null) {
            navPerfil.setOnClickListener(v -> {
                Intent intent = new Intent(this, PefilUser.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void cargarConversaciones() {
        if (conversationsRef == null) return;

        conversationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                masterConversacionesList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Conversacion conv = child.getValue(Conversacion.class);
                            if (conv != null) {
                                masterConversacionesList.add(conv);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Sort timestamp descending
                Collections.sort(masterConversacionesList, (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));

                filtrarConversaciones(etSearchChats != null ? etSearchChats.getText().toString() : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                masterConversacionesList.clear();
                filtrarConversaciones("");
            }
        };

        conversationsRef.addValueEventListener(conversationsListener);
    }

    private void filtrarConversaciones(String query) {
        displayConversacionesList.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (Conversacion conv : masterConversacionesList) {
            String name = conv.getChatUserName() != null ? conv.getChatUserName().toLowerCase() : "";
            String msg = conv.getLastMessage() != null ? conv.getLastMessage().toLowerCase() : "";

            if (TextUtils.isEmpty(lowerQuery) || name.contains(lowerQuery) || msg.contains(lowerQuery)) {
                displayConversacionesList.add(conv);
            }
        }

        if (displayConversacionesList.isEmpty()) {
            if (tvEmptyConversations != null) tvEmptyConversations.setVisibility(View.VISIBLE);
            if (rvConversationsList != null) rvConversationsList.setVisibility(View.GONE);
        } else {
            if (tvEmptyConversations != null) tvEmptyConversations.setVisibility(View.GONE);
            if (rvConversationsList != null) rvConversationsList.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    private void solicitarCamaraOCapturar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamaraCaptura();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamaraCaptura() {
        try {
            File photoFile = File.createTempFile("cam_photo_", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EstadoUsuarioHelper.setOnline(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EstadoUsuarioHelper.setOnline(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsRef != null && conversationsListener != null) {
            conversationsRef.removeEventListener(conversationsListener);
        }
    }
}
