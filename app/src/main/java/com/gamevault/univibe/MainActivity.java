package com.gamevault.univibe;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivShowPassword;
    private TextView tvForgotPassword;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Check for active session immediately
        if (mAuth.getCurrentUser() != null) {
            irAPanelPrincipal();
            return;
        }

        // Initialize UI components
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        ivShowPassword = findViewById(R.id.iv_show_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);

        if (ivShowPassword != null) {
            ivShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        }
        
        Button btnLogin = findViewById(R.id.btn_login);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> iniciarSesion());
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> 
                Toast.makeText(this, "Función para recuperar contraseña próximamente", Toast.LENGTH_SHORT).show()
            );
        }

        View btnGoogleLogin = findViewById(R.id.card_google_login);
        if (btnGoogleLogin != null) {
            btnGoogleLogin.setOnClickListener(v -> iniciarSesionSocial("google.com"));
        }

        View btnMicrosoftLogin = findViewById(R.id.card_microsoft_login);
        if (btnMicrosoftLogin != null) {
            btnMicrosoftLogin.setOnClickListener(v -> iniciarSesionSocial("microsoft.com"));
        }

        TextView tvRegisterLink = findViewById(R.id.tv_register_link);
        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, registroRegistro.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Maintain active session check on app return
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            irAPanelPrincipal();
        }
    }

    private void iniciarSesion() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.msg_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Login with Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, R.string.welcome_back, Toast.LENGTH_SHORT).show();
                irAPanelPrincipal();
            } else {
                Toast.makeText(MainActivity.this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void iniciarSesionSocial(String providerId) {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(providerId);

        if ("google.com".equals(providerId)) {
            provider.addCustomParameter("prompt", "select_account");
            List<String> scopes = new ArrayList<>();
            scopes.add("email");
            scopes.add("profile");
            provider.setScopes(scopes);
        } else if ("microsoft.com".equals(providerId)) {
            provider.addCustomParameter("prompt", "select_account");
            provider.addCustomParameter("tenant", "common");
        }

        Task<AuthResult> pendingResultTask = mAuth.getPendingAuthResult();
        if (pendingResultTask != null) {
            pendingResultTask
                    .addOnSuccessListener(this::manejarResultadoAutenticacionExitosa)
                    .addOnFailureListener(this::manejarErrorAutenticacion);
        } else {
            mAuth.startActivityForSignInWithProvider(this, provider.build())
                    .addOnSuccessListener(this::manejarResultadoAutenticacionExitosa)
                    .addOnFailureListener(this::manejarErrorAutenticacion);
        }
    }

    private void manejarResultadoAutenticacionExitosa(AuthResult authResult) {
        if (isFinishing() || isDestroyed()) return;

        if (authResult != null && authResult.getUser() != null) {
            FirebaseUser user = authResult.getUser();
            guardarDatosUsuarioSocialSiNoExisten(user);
            Toast.makeText(this, R.string.msg_auth_success, Toast.LENGTH_SHORT).show();
            irAPanelPrincipal();
        }
    }

    private void manejarErrorAutenticacion(Exception e) {
        if (isFinishing() || isDestroyed()) return;

        if (e != null && e.getMessage() != null && (e.getMessage().contains("canceled") || e.getMessage().contains("cancelled"))) {
            Toast.makeText(this, R.string.msg_auth_canceled, Toast.LENGTH_SHORT).show();
        } else {
            String errorMsg = (e != null && e.getLocalizedMessage() != null) ? e.getLocalizedMessage() : getString(R.string.msg_auth_google_error);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        }
    }

    private void guardarDatosUsuarioSocialSiNoExisten(FirebaseUser firebaseUser) {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    String name = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "Usuario";
                    String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
                    String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

                    User newUser = new User(name, email, "Sin especificar");
                    if (!photoUrl.isEmpty()) {
                        newUser.setImgPerfil(photoUrl);
                    }
                    userRef.setValue(newUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void irAPanelPrincipal() {
        Intent intent = new Intent(this, panelPrincipal.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            ivShowPassword.setImageResource(android.R.drawable.ic_lock_idle_lock);
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            ivShowPassword.setImageResource(android.R.drawable.ic_menu_view);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }
}
