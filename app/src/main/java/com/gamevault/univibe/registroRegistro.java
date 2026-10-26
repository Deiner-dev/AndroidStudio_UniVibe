package com.gamevault.univibe;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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

public class registroRegistro extends AppCompatActivity {

    private EditText etFullName, etInstitutionalEmail, etPassword;
    private Spinner spUniversity;
    private ImageView ivShowPassword;
    private boolean isPasswordVisible = false;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_registro_registro);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupNavigation();
        setupSpinner();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("registroUsuario");
    }

    private void initViews() {
        etFullName = findViewById(R.id.et_full_name);
        etInstitutionalEmail = findViewById(R.id.et_institutional_email);
        etPassword = findViewById(R.id.et_password_reg);
        spUniversity = findViewById(R.id.sp_university);
        ivShowPassword = findViewById(R.id.iv_show_password_reg);

        if (ivShowPassword != null) {
            ivShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        }

        Button btnRegister = findViewById(R.id.btn_register_me);
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> validarRegistro());
        }

        View btnGoogleReg = findViewById(R.id.card_google_reg);
        if (btnGoogleReg != null) {
            btnGoogleReg.setOnClickListener(v -> iniciarSesionSocial("google.com"));
        }

        View btnMicrosoftReg = findViewById(R.id.card_microsoft_reg);
        if (btnMicrosoftReg != null) {
            btnMicrosoftReg.setOnClickListener(v -> iniciarSesionSocial("microsoft.com"));
        }
    }

    private void setupNavigation() {
        TextView tvLoginLink = findViewById(R.id.tv_login_link);
        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(registroRegistro.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupSpinner() {
        if (spUniversity != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.universities_array, R.layout.spinner_selected_item);
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spUniversity.setAdapter(adapter);
        }

        View ivArrow = findViewById(R.id.iv_university_arrow);
        LinearLayout llUniversity = findViewById(R.id.ll_university);
        
        if (ivArrow != null && spUniversity != null) {
            ivArrow.setOnClickListener(v -> spUniversity.performClick());
        }
        
        if (llUniversity != null && spUniversity != null) {
            llUniversity.setOnClickListener(v -> spUniversity.performClick());
        }
    }

    private void validarRegistro() {
        if (etFullName == null || etInstitutionalEmail == null || etPassword == null || spUniversity == null) return;

        String name = etFullName.getText().toString().trim();
        String email = etInstitutionalEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String university = spUniversity.getSelectedItem().toString();
        int universityPos = spUniversity.getSelectedItemPosition();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || universityPos <= 0) {
            Toast.makeText(this, R.string.msg_fill_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Firebase User
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                String uid = mAuth.getCurrentUser().getUid();
                User newUser = new User(name, email, university);
                
                myRef.child(uid).setValue(newUser).addOnCompleteListener(dbTask -> {
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(registroRegistro.this, R.string.msg_registered_success, Toast.LENGTH_SHORT).show();
                        irAPanelPrincipal();
                    } else {
                        Toast.makeText(registroRegistro.this, "Error al guardar datos", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                String errorMsg = (task.getException() != null) ? task.getException().getMessage() : "Error desconocido";
                Toast.makeText(registroRegistro.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
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
