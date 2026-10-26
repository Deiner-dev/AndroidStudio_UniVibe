package com.gamevault.univibe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class RecortarImagenActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private ImageButton ibBack;
    private Button btnCancel, btnAccept;
    private String imageUriString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        setContentView(R.layout.activity_recortar_imagen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        imageUriString = getIntent().getStringExtra("imageUri");
        if (TextUtils.isEmpty(imageUriString)) {
            Toast.makeText(this, "Error al cargar la foto", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        cargarImagen();
    }

    private void initViews() {
        ivPreview = findViewById(R.id.iv_crop_preview);
        ibBack = findViewById(R.id.ib_back_crop);
        btnCancel = findViewById(R.id.btn_cancel_crop);
        btnAccept = findViewById(R.id.btn_accept_crop);
    }

    private void setupListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
        if (btnCancel != null) btnCancel.setOnClickListener(v -> finish());

        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> {
                Intent intent = new Intent(RecortarImagenActivity.this, PublicacionContenido.class);
                intent.putExtra("capturedImageUri", imageUriString);
                startActivity(intent);
                finish();
            });
        }
    }

    private void cargarImagen() {
        if (ivPreview != null) {
            Glide.with(this)
                    .load(imageUriString)
                    .fitCenter()
                    .into(ivPreview);
        }
    }
}
