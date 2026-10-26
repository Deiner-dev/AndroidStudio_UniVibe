package com.gamevault.univibe;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
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
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PefilUser extends AppCompatActivity {

    private ShapeableImageView ivProfilePic;
    private TextView tvName, tvEmail, tvCareer, tvUniversity, tvAbout, tvPublicationsCount, tvSeguidosCount, tvSeguidoresCount;
    private MaterialButton btnFollowProfile;
    private RecyclerView rvProfilePosts;
    private LinearLayout llEmptyProfilePosts;
    private ImageButton ibSettings;
    private PopupWindow settingsPopupWindow;

    private DatabaseReference userRef, userPostsRef, seguidosRef, seguidoresRef, isFollowingRef;
    private StorageReference storageRef;
    private FirebaseAuth mAuth;
    private String currentUserId = ""; 
    private String targetUserId = "";
    private ValueEventListener userListener, userPostsValueEventListener, seguidosListener, seguidoresListener, isFollowingListener;

    private List<Publicacion> userPublicacionesList;
    private PublicacionesAdapter userPublicacionesAdapter;

    private Uri cameraImageUri = null;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    pickImage();
                } else {
                    Toast.makeText(this, "Permiso denegado para acceder a la galería", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadImage(uri);
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
        setContentView(R.layout.activity_pefil_user);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            irALogin();
            return;
        }

        targetUserId = getIntent().getStringExtra("targetUid");
        if (TextUtils.isEmpty(targetUserId)) {
            targetUserId = currentUserId;
        }

        initViews();
        initFirebase();
        setupNavigation();
        loadUserProfile();
        loadUserPublicaciones();
        loadSeguidosYSeguidores();
        verificarYConfigurarBotonSeguir();
        NotificacionesHelper.iniciarServicioNotificaciones(this);
    }

    private void initViews() {
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        tvName = findViewById(R.id.tv_user_name_profile);
        tvEmail = findViewById(R.id.tv_user_handle_profile);
        tvCareer = findViewById(R.id.tv_career_profile);
        tvUniversity = findViewById(R.id.tv_university_profile);
        tvAbout = findViewById(R.id.tv_about_content);
        tvPublicationsCount = findViewById(R.id.tv_publications_count);
        tvSeguidosCount = findViewById(R.id.tv_seguidos_count);
        tvSeguidoresCount = findViewById(R.id.tv_seguidores_count);
        btnFollowProfile = findViewById(R.id.btn_follow_profile);
        rvProfilePosts = findViewById(R.id.rv_profile_posts);
        llEmptyProfilePosts = findViewById(R.id.ll_empty_profile_posts);
        ibSettings = findViewById(R.id.ib_settings);

        userPublicacionesList = new ArrayList<>();
        userPublicacionesAdapter = new PublicacionesAdapter(this, userPublicacionesList);

        if (rvProfilePosts != null) {
            rvProfilePosts.setLayoutManager(new LinearLayoutManager(this));
            rvProfilePosts.setAdapter(userPublicacionesAdapter);
            rvProfilePosts.setNestedScrollingEnabled(false);
        }

        View cardSeguidores = findViewById(R.id.tv_seguidores_count);
        if (cardSeguidores != null) {
            View parentCard = (View) cardSeguidores.getParent();
            View containerCard = parentCard != null ? (View) parentCard.getParent() : cardSeguidores;
            containerCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, SeguidoresSeguidosActivity.class);
                intent.putExtra("targetUid", targetUserId);
                intent.putExtra("initialTab", "SEGUIDORES");
                startActivity(intent);
            });
        }

        View cardSeguidos = findViewById(R.id.tv_seguidos_count);
        if (cardSeguidos != null) {
            View parentCard = (View) cardSeguidos.getParent();
            View containerCard = parentCard != null ? (View) parentCard.getParent() : cardSeguidos;
            containerCard.setOnClickListener(v -> {
                Intent intent = new Intent(this, SeguidoresSeguidosActivity.class);
                intent.putExtra("targetUid", targetUserId);
                intent.putExtra("initialTab", "SEGUIDOS");
                startActivity(intent);
            });
        }

        View llOtherUserActions = findViewById(R.id.ll_other_user_actions);
        View ibMessageProfile = findViewById(R.id.ib_message_profile);

        if (targetUserId.equals(currentUserId)) {
            ivProfilePic.setOnClickListener(v -> checkPermissionAndPickImage());
            if (ibSettings != null) {
                ibSettings.setVisibility(View.VISIBLE);
                ibSettings.setOnClickListener(this::toggleMenuConfiguracion);
            }
            if (llOtherUserActions != null) {
                llOtherUserActions.setVisibility(View.GONE);
            }
        } else {
            ivProfilePic.setOnClickListener(null);
            if (ibSettings != null) {
                ibSettings.setVisibility(View.GONE);
            }
            if (llOtherUserActions != null) {
                llOtherUserActions.setVisibility(View.VISIBLE);
            }
            if (btnFollowProfile != null) {
                btnFollowProfile.setOnClickListener(v -> toggleSeguirPerfil());
            }
            if (ibMessageProfile != null) {
                ibMessageProfile.setOnClickListener(v -> {
                    Intent intent = new Intent(this, chat.class);
                    intent.putExtra("targetUid", targetUserId);
                    startActivity(intent);
                });
            }
        }
    }

    private void toggleMenuConfiguracion(View anchorView) {
        if (settingsPopupWindow != null && settingsPopupWindow.isShowing()) {
            settingsPopupWindow.dismiss();
            return;
        }

        View popupView = getLayoutInflater().inflate(R.layout.popup_settings_menu, null);

        settingsPopupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        settingsPopupWindow.setElevation(20f);
        settingsPopupWindow.setOutsideTouchable(true);

        View itemConfig = popupView.findViewById(R.id.ll_menu_config);
        if (itemConfig != null) {
            itemConfig.setOnClickListener(v -> {
                settingsPopupWindow.dismiss();
                Toast.makeText(this, R.string.menu_configuration, Toast.LENGTH_SHORT).show();
            });
        }

        View itemPrivacy = popupView.findViewById(R.id.ll_menu_privacy);
        if (itemPrivacy != null) {
            itemPrivacy.setOnClickListener(v -> {
                settingsPopupWindow.dismiss();
                Toast.makeText(this, R.string.menu_privacy, Toast.LENGTH_SHORT).show();
            });
        }

        View itemHelp = popupView.findViewById(R.id.ll_menu_help);
        if (itemHelp != null) {
            itemHelp.setOnClickListener(v -> {
                settingsPopupWindow.dismiss();
                Toast.makeText(this, R.string.menu_help_support, Toast.LENGTH_SHORT).show();
            });
        }

        View itemLogout = popupView.findViewById(R.id.ll_menu_logout);
        if (itemLogout != null) {
            itemLogout.setOnClickListener(v -> {
                settingsPopupWindow.dismiss();
                cerrarSesion();
            });
        }

        settingsPopupWindow.showAsDropDown(anchorView, -320, 10);
    }

    private void cerrarSesion() {
        NotificacionesHelper.detenerServicioNotificaciones(this);
        mAuth.signOut();

        SharedPreferences pref = getSharedPreferences("UniVibePrefs", MODE_PRIVATE);
        pref.edit().clear().apply();

        Toast.makeText(this, R.string.msg_logout_success, Toast.LENGTH_SHORT).show();
        irALogin();
    }

    private void irALogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initFirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(targetUserId);
        userPostsRef = FirebaseDatabase.getInstance().getReference("userPublicaciones").child(targetUserId);
        seguidosRef = FirebaseDatabase.getInstance().getReference("seguidos").child(targetUserId);
        seguidoresRef = FirebaseDatabase.getInstance().getReference("seguidores").child(targetUserId);
        storageRef = FirebaseStorage.getInstance().getReference("imgPerfil").child(targetUserId).child("perfil.jpg");
        isFollowingRef = FirebaseDatabase.getInstance().getReference("seguidos").child(currentUserId).child(targetUserId);
    }

    private void verificarYConfigurarBotonSeguir() {
        if (targetUserId.equals(currentUserId) || btnFollowProfile == null) return;

        isFollowingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                boolean loSigue = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                if (loSigue) {
                    btnFollowProfile.setText(R.string.btn_following);
                    btnFollowProfile.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                    btnFollowProfile.setTextColor(Color.parseColor("#1A3E30"));
                } else {
                    btnFollowProfile.setText(R.string.btn_follow);
                    btnFollowProfile.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#36A477")));
                    btnFollowProfile.setTextColor(Color.WHITE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        isFollowingRef.addValueEventListener(isFollowingListener);
    }

    private void toggleSeguirPerfil() {
        if (targetUserId.equals(currentUserId) || TextUtils.isEmpty(currentUserId) || TextUtils.isEmpty(targetUserId)) return;

        isFollowingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean yaLoSigue = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
                Map<String, Object> updates = new HashMap<>();

                if (yaLoSigue) {
                    updates.put("/seguidos/" + currentUserId + "/" + targetUserId, null);
                    updates.put("/seguidores/" + targetUserId + "/" + currentUserId, null);
                } else {
                    updates.put("/seguidos/" + currentUserId + "/" + targetUserId, true);
                    updates.put("/seguidores/" + targetUserId + "/" + currentUserId, true);
                    NotificacionesHelper.registrarNotificacion(targetUserId, "FOLLOW", "", "", "", "te comenzó a seguir");
                }

                rootRef.updateChildren(updates);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void pickImage() {
        pickImageLauncher.launch("image/*");
    }

    private void uploadImage(Uri imageUri) {
        if (imageUri == null) return;

        Toast.makeText(this, "Subiendo imagen...", Toast.LENGTH_SHORT).show();
        storageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                
                userRef.child("imgPerfil").setValue(downloadUrl).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!isFinishing() && !isDestroyed()) {
                            Glide.with(PefilUser.this)
                                    .load(downloadUrl)
                                    .circleCrop()
                                    .into(ivProfilePic);
                            Toast.makeText(PefilUser.this, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(PefilUser.this, "Error al subir imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserProfile() {
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    String photoUrl = snapshot.child("imgPerfil").getValue(String.class);
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(PefilUser.this)
                                .load(photoUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.ic_menu_report_image)
                                .circleCrop()
                                .into(ivProfilePic);
                    }
                    
                    String name = snapshot.child("fullName").getValue(String.class);
                    String email = snapshot.child("institutionalEmail").getValue(String.class);
                    String university = snapshot.child("university").getValue(String.class);
                    String career = snapshot.child("career").getValue(String.class);
                    String aboutMe = snapshot.child("aboutMe").getValue(String.class);
                    
                    if (name != null) tvName.setText(name);
                    if (email != null) tvEmail.setText(email);
                    if (university != null) tvUniversity.setText(university);
                    
                    if (career != null && !career.isEmpty()) {
                        tvCareer.setText(career);
                    } else {
                        tvCareer.setText(R.string.placeholder_career);
                    }
                    
                    if (aboutMe != null && !aboutMe.isEmpty()) {
                        tvAbout.setText(aboutMe);
                    } else {
                        tvAbout.setText(R.string.placeholder_about_me);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PefilUser.this, "Error al cargar perfil", Toast.LENGTH_SHORT).show();
            }
        };

        userRef.addValueEventListener(userListener);
    }

    private void loadUserPublicaciones() {
        userPostsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                userPublicacionesList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot postSnap : snapshot.getChildren()) {
                        try {
                            Publicacion post = postSnap.getValue(Publicacion.class);
                            if (post != null) {
                                userPublicacionesList.add(post);
                            }
                        } catch (Exception e) {
                        }
                    }
                }

                // Sort timestamp descending
                Collections.sort(userPublicacionesList, (p1, p2) -> Long.compare(p2.getTimestamp(), p1.getTimestamp()));

                if (tvPublicationsCount != null) {
                    tvPublicationsCount.setText(String.valueOf(userPublicacionesList.size()));
                }

                if (userPublicacionesList.isEmpty()) {
                    if (rvProfilePosts != null) rvProfilePosts.setVisibility(View.GONE);
                    if (llEmptyProfilePosts != null) llEmptyProfilePosts.setVisibility(View.VISIBLE);
                } else {
                    if (rvProfilePosts != null) rvProfilePosts.setVisibility(View.VISIBLE);
                    if (llEmptyProfilePosts != null) llEmptyProfilePosts.setVisibility(View.GONE);
                    userPublicacionesAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(PefilUser.this, "Error al cargar publicaciones del perfil", Toast.LENGTH_SHORT).show();
            }
        };

        userPostsRef.addValueEventListener(userPostsValueEventListener);
    }

    private void loadSeguidosYSeguidores() {
        if (seguidosRef != null) {
            seguidosListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvSeguidosCount != null) {
                        tvSeguidosCount.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            seguidosRef.addValueEventListener(seguidosListener);
        }

        if (seguidoresRef != null) {
            seguidoresListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvSeguidoresCount != null) {
                        tvSeguidoresCount.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            seguidoresRef.addValueEventListener(seguidoresListener);
        }
    }

    private void setupNavigation() {
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
        
        View btnCreatePost = findViewById(R.id.btn_create_post);
        if (btnCreatePost != null) {
            btnCreatePost.setOnClickListener(v -> {
                Intent intent = new Intent(this, PublicacionContenido.class);
                startActivity(intent);
            });
        }
        
        View navAlerts = findViewById(R.id.ll_nav_alertas);
        if (navAlerts != null) {
            navAlerts.setOnClickListener(v -> {
                Intent intent = new Intent(this, alertas.class);
                startActivity(intent);
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
        if (userPostsRef != null && userPostsValueEventListener != null) {
            userPostsRef.removeEventListener(userPostsValueEventListener);
        }
        if (seguidosRef != null && seguidosListener != null) {
            seguidosRef.removeEventListener(seguidosListener);
        }
        if (seguidoresRef != null && seguidoresListener != null) {
            seguidoresRef.removeEventListener(seguidoresListener);
        }
        if (isFollowingRef != null && isFollowingListener != null) {
            isFollowingRef.removeEventListener(isFollowingListener);
        }
        if (settingsPopupWindow != null && settingsPopupWindow.isShowing()) {
            settingsPopupWindow.dismiss();
        }
    }
}
