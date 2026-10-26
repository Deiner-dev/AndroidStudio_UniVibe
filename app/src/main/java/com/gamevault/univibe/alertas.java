package com.gamevault.univibe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.bumptech.glide.Glide;
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

public class alertas extends AppCompatActivity {

    private static final int FILTER_TODAS = 0;
    private static final int FILTER_SEGUIMIENTOS = 1;
    private static final int FILTER_LIKES = 2;
    private static final int FILTER_COMENTARIOS = 3;

    private int activeFilter = FILTER_TODAS;

    private ImageButton ibBack, ibChat;
    private TextView tvGreeting;
    private LinearLayout llFilterTodas, llFilterSeguimientos, llFilterLikes, llFilterComentarios;
    private ImageView ivIconTodas, ivIconSeguimientos, ivIconLikes, ivIconComentarios;
    private TextView tvTextTodas, tvTextSeguimientos, tvTextLikes, tvTextComentarios;
    private TextView tvCountTodas, tvCountSeguimientos, tvCountLikes, tvCountComentarios;
    private RecyclerView rvAllNotifications;
    private TextView tvEmptyNotifications;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef, notifsRef;
    private ValueEventListener userListener, notifsListener;
    private String currentUserId = "";

    private List<Notificacion> masterNotificationsList;
    private List<Notificacion> displayNotificationsList;
    private NotificacionesAdapter notificacionesAdapter;

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
        setContentView(R.layout.activity_alertas);

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
        setupNavigation();
        setupFilterListeners();
        cargarDatosUsuario();
        cargarTodasNotificaciones();
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_alertas);
        ibChat = findViewById(R.id.ib_chat_alertas);
        tvGreeting = findViewById(R.id.tv_greeting_alertas);

        llFilterTodas = findViewById(R.id.ll_filter_todas);
        llFilterSeguimientos = findViewById(R.id.ll_filter_seguimientos);
        llFilterLikes = findViewById(R.id.ll_filter_likes);
        llFilterComentarios = findViewById(R.id.ll_filter_comentarios);

        ivIconTodas = findViewById(R.id.iv_icon_todas);
        ivIconSeguimientos = findViewById(R.id.iv_icon_seguimientos);
        ivIconLikes = findViewById(R.id.iv_icon_likes);
        ivIconComentarios = findViewById(R.id.iv_icon_comentarios);

        tvTextTodas = findViewById(R.id.tv_text_todas);
        tvTextSeguimientos = findViewById(R.id.tv_text_seguimientos);
        tvTextLikes = findViewById(R.id.tv_text_likes);
        tvTextComentarios = findViewById(R.id.tv_text_comentarios);

        tvCountTodas = findViewById(R.id.tv_count_todas);
        tvCountSeguimientos = findViewById(R.id.tv_count_seguimientos);
        tvCountLikes = findViewById(R.id.tv_count_likes);
        tvCountComentarios = findViewById(R.id.tv_count_comentarios);

        rvAllNotifications = findViewById(R.id.rv_all_notifications);
        tvEmptyNotifications = findViewById(R.id.tv_empty_all_notifications);

        masterNotificationsList = new ArrayList<>();
        displayNotificationsList = new ArrayList<>();
        notificacionesAdapter = new NotificacionesAdapter(this, displayNotificationsList);

        if (rvAllNotifications != null) {
            rvAllNotifications.setLayoutManager(new LinearLayoutManager(this));
            rvAllNotifications.setAdapter(notificacionesAdapter);
            rvAllNotifications.setNestedScrollingEnabled(false);
        }
    }

    private void initFirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUserId);
        notifsRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(currentUserId);
    }

    private void cargarDatosUsuario() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null && tvGreeting != null) {
                        tvGreeting.setText(getString(R.string.greeting_format, name));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }

    private void cargarTodasNotificaciones() {
        notifsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                masterNotificationsList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Notificacion notif = child.getValue(Notificacion.class);
                            if (notif != null) {
                                masterNotificationsList.add(notif);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Sort timestamp descending
                Collections.sort(masterNotificationsList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                actualizarContadoresFiltros();
                aplicarFiltro(activeFilter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        notifsRef.addValueEventListener(notifsListener);
    }

    private void actualizarContadoresFiltros() {
        int countTodas = masterNotificationsList.size();
        int countSeguimientos = 0;
        int countLikes = 0;
        int countComentarios = 0;

        for (Notificacion notif : masterNotificationsList) {
            String type = notif.getType();
            if ("FOLLOW".equals(type)) {
                countSeguimientos++;
            } else if (type.startsWith("LIKE_")) {
                countLikes++;
            } else if ("COMMENT_POST".equals(type) || "REPLY_COMMENT".equals(type)) {
                countComentarios++;
            }
        }

        if (tvCountTodas != null) tvCountTodas.setText(String.valueOf(countTodas));
        if (tvCountSeguimientos != null) tvCountSeguimientos.setText(String.valueOf(countSeguimientos));
        if (tvCountLikes != null) tvCountLikes.setText(String.valueOf(countLikes));
        if (tvCountComentarios != null) tvCountComentarios.setText(String.valueOf(countComentarios));
    }

    private void setupFilterListeners() {
        if (llFilterTodas != null) {
            llFilterTodas.setOnClickListener(v -> aplicarFiltro(FILTER_TODAS));
        }
        if (llFilterSeguimientos != null) {
            llFilterSeguimientos.setOnClickListener(v -> aplicarFiltro(FILTER_SEGUIMIENTOS));
        }
        if (llFilterLikes != null) {
            llFilterLikes.setOnClickListener(v -> aplicarFiltro(FILTER_LIKES));
        }
        if (llFilterComentarios != null) {
            llFilterComentarios.setOnClickListener(v -> aplicarFiltro(FILTER_COMENTARIOS));
        }
    }

    private void aplicarFiltro(int filterType) {
        activeFilter = filterType;
        displayNotificationsList.clear();

        for (Notificacion notif : masterNotificationsList) {
            String type = notif.getType();
            if (filterType == FILTER_TODAS) {
                displayNotificationsList.add(notif);
            } else if (filterType == FILTER_SEGUIMIENTOS && "FOLLOW".equals(type)) {
                displayNotificationsList.add(notif);
            } else if (filterType == FILTER_LIKES && type.startsWith("LIKE_")) {
                displayNotificationsList.add(notif);
            } else if (filterType == FILTER_COMENTARIOS && ("COMMENT_POST".equals(type) || "REPLY_COMMENT".equals(type))) {
                displayNotificationsList.add(notif);
            }
        }

        actualizarEstilosChipsFiltros(filterType);

        if (displayNotificationsList.isEmpty()) {
            if (tvEmptyNotifications != null) tvEmptyNotifications.setVisibility(View.VISIBLE);
            if (rvAllNotifications != null) rvAllNotifications.setVisibility(View.GONE);
        } else {
            if (tvEmptyNotifications != null) tvEmptyNotifications.setVisibility(View.GONE);
            if (rvAllNotifications != null) rvAllNotifications.setVisibility(View.VISIBLE);
            notificacionesAdapter.notifyDataSetChanged();
        }
    }

    private void actualizarEstilosChipsFiltros(int selectedFilter) {
        resaltarChip(llFilterTodas, ivIconTodas, tvTextTodas, tvCountTodas, selectedFilter == FILTER_TODAS);
        resaltarChip(llFilterSeguimientos, ivIconSeguimientos, tvTextSeguimientos, tvCountSeguimientos, selectedFilter == FILTER_SEGUIMIENTOS);
        resaltarChip(llFilterLikes, ivIconLikes, tvTextLikes, tvCountLikes, selectedFilter == FILTER_LIKES);
        resaltarChip(llFilterComentarios, ivIconComentarios, tvTextComentarios, tvCountComentarios, selectedFilter == FILTER_COMENTARIOS);
    }

    private void resaltarChip(LinearLayout container, ImageView icon, TextView text, TextView count, boolean isSelected) {
        if (container == null) return;

        if (isSelected) {
            container.setBackgroundResource(R.drawable.bg_fab_green);
            if (icon != null) icon.setColorFilter(Color.WHITE);
            if (text != null) text.setTextColor(Color.WHITE);
            if (count != null) {
                count.setTextColor(Color.parseColor("#36A477"));
                count.setBackgroundResource(R.drawable.bg_status_dot);
            }
        } else {
            container.setBackgroundResource(R.drawable.bg_tab_selected);
            if (icon != null) icon.setColorFilter(Color.parseColor("#7A8C85"));
            if (text != null) text.setTextColor(Color.parseColor("#7A8C85"));
            if (count != null) {
                count.setTextColor(Color.parseColor("#7A8C85"));
                count.setBackgroundResource(R.drawable.bg_status_dot);
            }
        }
    }

    private void setupNavigation() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        if (ibChat != null) {
            ibChat.setOnClickListener(v -> Toast.makeText(this, R.string.desc_messages, Toast.LENGTH_SHORT).show());
        }

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

        View navPerfil = findViewById(R.id.ll_nav_perfil);
        if (navPerfil != null) {
            navPerfil.setOnClickListener(v -> {
                Intent intent = new Intent(this, PefilUser.class);
                startActivity(intent);
                finish();
            });
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
        if (userRef != null && userListener != null) {
            userRef.removeEventListener(userListener);
        }
        if (notifsRef != null && notifsListener != null) {
            notifsRef.removeEventListener(notifsListener);
        }
    }
}
