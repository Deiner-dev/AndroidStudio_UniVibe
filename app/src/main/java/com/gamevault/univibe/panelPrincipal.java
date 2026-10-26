package com.gamevault.univibe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
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

public class panelPrincipal extends AppCompatActivity {

    private LinearLayout llTabLocal, llTabColombia;
    private TextView tvGreeting, tvBadgeCount, tvChatBadgeCount;
    private ShapeableImageView ivAvatarDashboard;
    private ConstraintLayout clEmptyState;
    private RecyclerView rvPosts;
    private PopupWindow notificacionesPopupWindow;

    private String currentUserId;
    private DatabaseReference userRef, postsRef, userNotifsRef, userChatsRef;
    private ValueEventListener userListener, postsValueEventListener, notifsListener, chatsListener;

    private List<Publicacion> publicacionesList;
    private PublicacionesAdapter publicacionesAdapter;

    private Uri cameraImageUri = null;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    NotificacionesHelper.iniciarServicioNotificaciones(this);
                }
            });

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
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_panel_principal);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            irALogin();
            return;
        }

        initViews();
        setupTabs();
        setupNavigation();
        initFirebase();
        loadUserInfo();
        loadPublicaciones();
        solicitarPermisoNotificaciones();
    }

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                NotificacionesHelper.iniciarServicioNotificaciones(this);
            }
        } else {
            NotificacionesHelper.iniciarServicioNotificaciones(this);
        }
    }

    private void initViews() {
        llTabLocal = findViewById(R.id.ll_tab_local);
        llTabColombia = findViewById(R.id.ll_tab_colombia);
        tvGreeting = findViewById(R.id.tv_greeting);
        tvBadgeCount = findViewById(R.id.tv_badge_count);
        tvChatBadgeCount = findViewById(R.id.tv_chat_badge_count);
        ivAvatarDashboard = findViewById(R.id.iv_avatar_dashboard);
        clEmptyState = findViewById(R.id.cl_empty_state);
        rvPosts = findViewById(R.id.rv_posts);

        publicacionesList = new ArrayList<>();
        publicacionesAdapter = new PublicacionesAdapter(this, publicacionesList);

        if (rvPosts != null) {
            rvPosts.setLayoutManager(new LinearLayoutManager(this));
            rvPosts.setAdapter(publicacionesAdapter);
            rvPosts.setNestedScrollingEnabled(false);
        }
    }

    private void initFirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUserId);
        postsRef = FirebaseDatabase.getInstance().getReference("publicaciones");
        userNotifsRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(currentUserId);
        userChatsRef = FirebaseDatabase.getInstance().getReference("conversaciones").child(currentUserId);

        escucharNotificacionesNoLeidas();
        escucharConversacionesNoLeidas();
    }

    private void escucharNotificacionesNoLeidas() {
        if (userNotifsRef == null) return;

        notifsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                int unreadCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Notificacion notif = child.getValue(Notificacion.class);
                            if (notif != null && !notif.isRead()) {
                                unreadCount++;
                            }
                        } catch (Exception e) {}
                    }
                }

                if (tvBadgeCount != null) {
                    if (unreadCount > 0) {
                        tvBadgeCount.setVisibility(View.VISIBLE);
                        tvBadgeCount.setText(String.valueOf(unreadCount));
                    } else {
                        tvBadgeCount.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userNotifsRef.addValueEventListener(notifsListener);
    }

    private void escucharConversacionesNoLeidas() {
        if (userChatsRef == null) return;

        chatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                int unreadChatsCount = 0;
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Integer unread = child.child("unreadCount").getValue(Integer.class);
                            if (unread != null && unread > 0) {
                                unreadChatsCount++;
                            }
                        } catch (Exception e) {}
                    }
                }

                if (tvChatBadgeCount != null) {
                    if (unreadChatsCount > 0) {
                        tvChatBadgeCount.setVisibility(View.VISIBLE);
                        tvChatBadgeCount.setText(String.valueOf(unreadChatsCount));
                    } else {
                        tvChatBadgeCount.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userChatsRef.addValueEventListener(chatsListener);
    }

    private void loadUserInfo() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    String name = snapshot.child("fullName").getValue(String.class);
                    if (name != null && tvGreeting != null) {
                        tvGreeting.setText(getString(R.string.greeting_format, name));
                    }

                    String photoUrl = snapshot.child("imgPerfil").getValue(String.class);
                    if (photoUrl != null && !photoUrl.isEmpty() && ivAvatarDashboard != null) {
                        Glide.with(panelPrincipal.this)
                                .load(photoUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .circleCrop()
                                .into(ivAvatarDashboard);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        userRef.addValueEventListener(userListener);
    }

    private void loadPublicaciones() {
        postsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                publicacionesList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot postSnap : snapshot.getChildren()) {
                        try {
                            Publicacion post = postSnap.getValue(Publicacion.class);
                            if (post != null) {
                                publicacionesList.add(post);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Order by timestamp descending (newest first)
                Collections.sort(publicacionesList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                if (publicacionesList.isEmpty()) {
                    if (rvPosts != null) rvPosts.setVisibility(View.GONE);
                    if (clEmptyState != null) clEmptyState.setVisibility(View.VISIBLE);
                } else {
                    if (rvPosts != null) rvPosts.setVisibility(View.VISIBLE);
                    if (clEmptyState != null) clEmptyState.setVisibility(View.GONE);
                    publicacionesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(panelPrincipal.this, "Error al cargar publicaciones", Toast.LENGTH_SHORT).show();
            }
        };

        postsRef.addValueEventListener(postsValueEventListener);
    }

    private void irALogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupTabs() {
        if (llTabLocal != null) {
            llTabLocal.setOnClickListener(v -> {
                llTabLocal.setBackgroundResource(R.drawable.bg_tab_selected);
                llTabColombia.setBackgroundResource(0);
                Toast.makeText(this, R.string.label_local, Toast.LENGTH_SHORT).show();
            });
        }
        
        if (llTabColombia != null) {
            llTabColombia.setOnClickListener(v -> {
                llTabColombia.setBackgroundResource(R.drawable.bg_tab_selected);
                llTabLocal.setBackgroundResource(0);
                Toast.makeText(this, R.string.label_colombia, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupNavigation() {
        View navInicio = findViewById(R.id.ll_nav_inicio);
        if (navInicio != null) navInicio.setOnClickListener(v -> Toast.makeText(this, R.string.nav_home, Toast.LENGTH_SHORT).show());
        
        View navBuscar = findViewById(R.id.ll_nav_buscar);
        if (navBuscar != null) navBuscar.setOnClickListener(v -> Toast.makeText(this, R.string.nav_search, Toast.LENGTH_SHORT).show());
        
        View navAdd = findViewById(R.id.fl_nav_center);
        if (navAdd != null) {
            navAdd.setOnClickListener(v -> solicitarCamaraOCapturar());
        }

        View shareArea = findViewById(R.id.cl_share_area);
        if (shareArea != null) {
            shareArea.setOnClickListener(v -> {
                Intent intent = new Intent(this, PublicacionContenido.class);
                startActivity(intent);
            });
        }
        
        View navAlertas = findViewById(R.id.ll_nav_alertas);
        if (navAlertas != null) {
            navAlertas.setOnClickListener(v -> {
                Intent intent = new Intent(this, alertas.class);
                startActivity(intent);
            });
        }
        
        View navPerfil = findViewById(R.id.ll_nav_perfil);
        if (navPerfil != null) {
            navPerfil.setOnClickListener(v -> {
                Intent intent = new Intent(this, PefilUser.class);
                startActivity(intent);
            });
        }
        
        View btnNotifications = findViewById(R.id.ib_notifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(this::togglePopupNotificaciones);
        }
        
        View btnChat = findViewById(R.id.ib_chat);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, mensajeria.class);
                startActivity(intent);
            });
        }
    }

    private void togglePopupNotificaciones(View anchorView) {
        if (notificacionesPopupWindow != null && notificacionesPopupWindow.isShowing()) {
            notificacionesPopupWindow.dismiss();
            return;
        }

        View popupView = getLayoutInflater().inflate(R.layout.popup_notifications, null);

        notificacionesPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        notificacionesPopupWindow.setElevation(20f);
        notificacionesPopupWindow.setOutsideTouchable(true);

        // When closed, mark unread notifications as read
        notificacionesPopupWindow.setOnDismissListener(this::marcarNotificacionesComoLeidas);

        // Load dynamic real-time max 5 notifications
        cargarNotificacionesEnPopup(popupView);

        // Option "Ver más notificaciones" opens full Alertas screen
        View btnSeeMore = popupView.findViewById(R.id.btn_see_more_notifications);
        if (btnSeeMore != null) {
            btnSeeMore.setOnClickListener(v -> {
                if (notificacionesPopupWindow != null && notificacionesPopupWindow.isShowing()) {
                    notificacionesPopupWindow.dismiss();
                }
                Intent intent = new Intent(panelPrincipal.this, alertas.class);
                startActivity(intent);
            });
        }

        notificacionesPopupWindow.showAsDropDown(anchorView, -320, 10);
    }

    private void marcarNotificacionesComoLeidas() {
        if (userNotifsRef == null) return;

        userNotifsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        if (Boolean.FALSE.equals(child.child("read").getValue(Boolean.class))) {
                            child.getRef().child("read").setValue(true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void cargarNotificacionesEnPopup(View popupView) {
        RecyclerView rvNotifs = popupView.findViewById(R.id.rv_notifications_list);
        TextView tvEmpty = popupView.findViewById(R.id.tv_empty_notifications);

        if (rvNotifs == null || TextUtils.isEmpty(currentUserId)) return;

        List<Notificacion> allNotificacionesList = new ArrayList<>();
        List<Notificacion> max5List = new ArrayList<>();
        NotificacionesAdapter adapter = new NotificacionesAdapter(this, max5List);
        rvNotifs.setLayoutManager(new LinearLayoutManager(this));
        rvNotifs.setAdapter(adapter);

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(currentUserId);
        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allNotificacionesList.clear();
                max5List.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Notificacion notif = child.getValue(Notificacion.class);
                            if (notif != null) {
                                allNotificacionesList.add(notif);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Sort timestamp descending (newest first)
                Collections.sort(allNotificacionesList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));

                // Keep at most 5 items for popup display
                if (allNotificacionesList.size() > 5) {
                    max5List.addAll(allNotificacionesList.subList(0, 5));
                } else {
                    max5List.addAll(allNotificacionesList);
                }

                if (max5List.isEmpty()) {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    rvNotifs.setVisibility(View.GONE);
                } else {
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                    rvNotifs.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        if (postsRef != null && postsValueEventListener != null) {
            postsRef.removeEventListener(postsValueEventListener);
        }
        if (userNotifsRef != null && notifsListener != null) {
            userNotifsRef.removeEventListener(notifsListener);
        }
        if (userChatsRef != null && chatsListener != null) {
            userChatsRef.removeEventListener(chatsListener);
        }
        if (notificacionesPopupWindow != null && notificacionesPopupWindow.isShowing()) {
            notificacionesPopupWindow.dismiss();
        }
    }
}
