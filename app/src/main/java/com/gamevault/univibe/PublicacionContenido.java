package com.gamevault.univibe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class PublicacionContenido extends AppCompatActivity {

    private ShapeableImageView ivUserAvatar;
    private TextView tvUserName, tvUserSubInfo;
    private EditText etPostText, etPostLocation;
    private FrameLayout flImagePreview;
    private ImageView ivImagePreview;
    private ImageButton ibRemoveImage;
    private LinearLayout llOptionImage, llOptionVideo;
    private Button btnPublish, btnCancel;
    private ProgressBar pbPublishing;

    private DatabaseReference userRef, postsRef;
    private StorageReference storagePostsRef;
    private ValueEventListener userValueEventListener;

    private String currentUserId;
    private String userName = "";
    private String userPhotoUrl = "";
    private String university = "";
    private String career = "";
    private String semester = "";

    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    mostrarVistaPreviaImagen();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_publicacion_contenido);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Debe iniciar sesión para publicar", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initFirebase();
        setupListeners();
        cargarDatosUsuario();

        String capturedUriString = getIntent().getStringExtra("capturedImageUri");
        if (!TextUtils.isEmpty(capturedUriString)) {
            selectedImageUri = Uri.parse(capturedUriString);
            mostrarVistaPreviaImagen();
        }
    }

    private void initViews() {
        ivUserAvatar = findViewById(R.id.iv_user_avatar_post);
        tvUserName = findViewById(R.id.tv_user_name_post);
        tvUserSubInfo = findViewById(R.id.tv_user_sub_post);
        etPostText = findViewById(R.id.et_post_text);
        etPostLocation = findViewById(R.id.et_post_location);
        flImagePreview = findViewById(R.id.fl_image_preview);
        ivImagePreview = findViewById(R.id.iv_image_preview);
        ibRemoveImage = findViewById(R.id.ib_remove_image);
        llOptionImage = findViewById(R.id.ll_option_image);
        llOptionVideo = findViewById(R.id.ll_option_video);
        btnPublish = findViewById(R.id.btn_publish);
        btnCancel = findViewById(R.id.btn_cancel);
        pbPublishing = findViewById(R.id.pb_publishing);
    }

    private void initFirebase() {
        userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUserId);
        postsRef = FirebaseDatabase.getInstance().getReference("publicaciones");
        storagePostsRef = FirebaseStorage.getInstance().getReference("publicaciones").child(currentUserId);
    }

    private void cargarDatosUsuario() {
        userValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    userName = snapshot.child("fullName").getValue(String.class);
                    userPhotoUrl = snapshot.child("imgPerfil").getValue(String.class);
                    university = snapshot.child("university").getValue(String.class);
                    career = snapshot.child("career").getValue(String.class);
                    semester = snapshot.child("semester").getValue(String.class);

                    if (userName != null) tvUserName.setText(userName);

                    String carreraTexto = (career != null && !career.isEmpty()) ? career : getString(R.string.placeholder_career);
                    String semestreTexto = (semester != null && !semester.isEmpty()) ? semester : getString(R.string.default_semester);
                    
                    tvUserSubInfo.setText(getString(R.string.user_academic_info_format, carreraTexto, semestreTexto));

                    if (userPhotoUrl != null && !userPhotoUrl.isEmpty()) {
                        Glide.with(PublicacionContenido.this)
                                .load(userPhotoUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .circleCrop()
                                .into(ivUserAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        userRef.addValueEventListener(userValueEventListener);
    }

    private void setupListeners() {
        llOptionImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        llOptionVideo.setOnClickListener(v -> 
            Toast.makeText(this, R.string.msg_videos_not_supported, Toast.LENGTH_SHORT).show()
        );

        ibRemoveImage.setOnClickListener(v -> quitarImagen());

        btnCancel.setOnClickListener(v -> finish());

        btnPublish.setOnClickListener(v -> validarYPublicar());
    }

    private void mostrarVistaPreviaImagen() {
        if (selectedImageUri != null && flImagePreview != null && ivImagePreview != null) {
            flImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(ivImagePreview);
        }
    }

    private void quitarImagen() {
        selectedImageUri = null;
        if (flImagePreview != null) {
            flImagePreview.setVisibility(View.GONE);
        }
    }

    private void validarYPublicar() {
        String texto = etPostText.getText().toString().trim();
        String ubicacion = etPostLocation.getText().toString().trim();

        boolean tieneTexto = !TextUtils.isEmpty(texto);
        boolean tieneImagen = (selectedImageUri != null);

        if (!tieneTexto && !tieneImagen) {
            Toast.makeText(this, R.string.msg_post_empty_error, Toast.LENGTH_SHORT).show();
            return;
        }

        setBotonEstadoCarga(true);

        // Generate unique key using push()
        String postId = postsRef.push().getKey();
        if (postId == null) {
            setBotonEstadoCarga(false);
            Toast.makeText(this, R.string.msg_post_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (tieneImagen) {
            subirImagenYGuardarPublicacion(postId, texto, ubicacion);
        } else {
            guardarPublicacionEnDatabase(postId, texto, "", ubicacion);
        }
    }

    private void subirImagenYGuardarPublicacion(String postId, String texto, String ubicacion) {
        StorageReference imageFileRef = storagePostsRef.child(postId + ".jpg");

        imageFileRef.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot -> 
            imageFileRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                String imageUrl = downloadUri.toString();
                guardarPublicacionEnDatabase(postId, texto, imageUrl, ubicacion);
            }).addOnFailureListener(this::manejarErrorPublicacion)
        ).addOnFailureListener(this::manejarErrorPublicacion);
    }

    private void guardarPublicacionEnDatabase(String postId, String texto, String imageUrl, String ubicacion) {
        long timestamp = System.currentTimeMillis();

        String carreraFinal = (career != null) ? career : "";
        String semestreFinal = (semester != null) ? semester : "";
        String universidadFinal = (university != null) ? university : "";
        String nombreFinal = (userName != null) ? userName : "";
        String fotoFinal = (userPhotoUrl != null) ? userPhotoUrl : "";

        Publicacion publicacion = new Publicacion(
                postId,
                currentUserId,
                nombreFinal,
                fotoFinal,
                universidadFinal,
                carreraFinal,
                semestreFinal,
                texto,
                imageUrl,
                ubicacion,
                timestamp
        );

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/publicaciones/" + postId, publicacion);
        childUpdates.put("/userPublicaciones/" + currentUserId + "/" + postId, publicacion);

        rootRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
            setBotonEstadoCarga(false);
            if (task.isSuccessful()) {
                Toast.makeText(PublicacionContenido.this, R.string.msg_post_success, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String errorMsg = (task.getException() != null) ? task.getException().getMessage() : getString(R.string.msg_post_error);
                Toast.makeText(PublicacionContenido.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void manejarErrorPublicacion(Exception e) {
        setBotonEstadoCarga(false);
        String mensaje = (e != null && e.getMessage() != null) ? e.getMessage() : getString(R.string.msg_post_error);
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }

    private void setBotonEstadoCarga(boolean cargando) {
        if (btnPublish != null) btnPublish.setEnabled(!cargando);
        if (btnCancel != null) btnCancel.setEnabled(!cargando);
        if (pbPublishing != null) {
            pbPublishing.setVisibility(cargando ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userRef != null && userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
        }
    }
}
