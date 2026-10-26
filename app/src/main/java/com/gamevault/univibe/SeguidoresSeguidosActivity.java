package com.gamevault.univibe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

public class SeguidoresSeguidosActivity extends AppCompatActivity {

    public static final int TAB_SEGUIDORES = 0;
    public static final int TAB_SEGUIDOS = 1;

    private int activeTab = TAB_SEGUIDORES;

    private ImageButton ibBack;
    private LinearLayout llTabSeguidores, llTabSeguidos;
    private ImageView ivIconSeguidores, ivIconSeguidos;
    private TextView tvTitleSeguidores, tvTitleSeguidos;
    private TextView tvCountSeguidores, tvCountSeguidos;
    private EditText etSearchUsers;
    private RecyclerView rvUsersList;
    private TextView tvEmptyUsers;

    private FirebaseAuth mAuth;
    private DatabaseReference seguidoresRef, seguidosRef, usersRef;
    private ValueEventListener seguidoresListener, seguidosListener;

    private String currentUserId = "";
    private String targetUserId = "";

    private List<User> masterUsersList;
    private List<User> displayUsersList;
    private SeguidoresSeguidosAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_seguidores_seguidos);

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

        targetUserId = getIntent().getStringExtra("targetUid");
        if (TextUtils.isEmpty(targetUserId)) {
            targetUserId = currentUserId;
        }

        String initialTabExtra = getIntent().getStringExtra("initialTab");
        if ("SEGUIDOS".equalsIgnoreCase(initialTabExtra)) {
            activeTab = TAB_SEGUIDOS;
        } else {
            activeTab = TAB_SEGUIDORES;
        }

        initViews();
        initFirebase();
        setupListeners();
        cargarContadoresTabs();
        seleccionarTab(activeTab);
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_followers);

        llTabSeguidores = findViewById(R.id.ll_tab_seguidores);
        llTabSeguidos = findViewById(R.id.ll_tab_seguidos);

        ivIconSeguidores = findViewById(R.id.iv_icon_tab_seguidores);
        ivIconSeguidos = findViewById(R.id.iv_icon_tab_seguidos);

        tvTitleSeguidores = findViewById(R.id.tv_title_tab_seguidores);
        tvTitleSeguidos = findViewById(R.id.tv_title_tab_seguidos);

        tvCountSeguidores = findViewById(R.id.tv_count_tab_seguidores);
        tvCountSeguidos = findViewById(R.id.tv_count_tab_seguidos);

        etSearchUsers = findViewById(R.id.et_search_users);
        rvUsersList = findViewById(R.id.rv_users_list);
        tvEmptyUsers = findViewById(R.id.tv_empty_users);

        masterUsersList = new ArrayList<>();
        displayUsersList = new ArrayList<>();
        adapter = new SeguidoresSeguidosAdapter(this, displayUsersList);

        if (rvUsersList != null) {
            rvUsersList.setLayoutManager(new LinearLayoutManager(this));
            rvUsersList.setAdapter(adapter);
        }
    }

    private void initFirebase() {
        seguidoresRef = FirebaseDatabase.getInstance().getReference("seguidores").child(targetUserId);
        seguidosRef = FirebaseDatabase.getInstance().getReference("seguidos").child(targetUserId);
        usersRef = FirebaseDatabase.getInstance().getReference("registroUsuario");
    }

    private void cargarContadoresTabs() {
        if (seguidoresRef != null) {
            seguidoresListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvCountSeguidores != null) {
                        tvCountSeguidores.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            seguidoresRef.addValueEventListener(seguidoresListener);
        }

        if (seguidosRef != null) {
            seguidosListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (isFinishing() || isDestroyed()) return;
                    if (tvCountSeguidos != null) {
                        tvCountSeguidos.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            seguidosRef.addValueEventListener(seguidosListener);
        }
    }

    private void setupListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        if (llTabSeguidores != null) {
            llTabSeguidores.setOnClickListener(v -> seleccionarTab(TAB_SEGUIDORES));
        }

        if (llTabSeguidos != null) {
            llTabSeguidos.setOnClickListener(v -> seleccionarTab(TAB_SEGUIDOS));
        }

        if (etSearchUsers != null) {
            etSearchUsers.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filtrarUsuarios(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void seleccionarTab(int tabIndex) {
        activeTab = tabIndex;
        actualizarEstilosTabs(tabIndex);
        cargarUsuariosPorTab(tabIndex);
    }

    private void actualizarEstilosTabs(int selectedTab) {
        if (selectedTab == TAB_SEGUIDORES) {
            if (llTabSeguidores != null) llTabSeguidores.setBackgroundResource(R.drawable.bg_fab_green);
            if (ivIconSeguidores != null) ivIconSeguidores.setColorFilter(Color.WHITE);
            if (tvTitleSeguidores != null) tvTitleSeguidores.setTextColor(Color.WHITE);
            if (tvCountSeguidores != null) {
                tvCountSeguidores.setTextColor(Color.BLACK);
                tvCountSeguidores.setBackgroundResource(R.drawable.bg_status_dot);
            }

            if (llTabSeguidos != null) llTabSeguidos.setBackgroundResource(android.R.color.transparent);
            if (ivIconSeguidos != null) ivIconSeguidos.setColorFilter(Color.parseColor("#7A8C85"));
            if (tvTitleSeguidos != null) tvTitleSeguidos.setTextColor(Color.parseColor("#7A8C85"));
            if (tvCountSeguidos != null) {
                tvCountSeguidos.setTextColor(Color.BLACK);
                tvCountSeguidos.setBackgroundResource(R.drawable.bg_status_dot);
            }
        } else {
            if (llTabSeguidos != null) llTabSeguidos.setBackgroundResource(R.drawable.bg_fab_green);
            if (ivIconSeguidos != null) ivIconSeguidos.setColorFilter(Color.WHITE);
            if (tvTitleSeguidos != null) tvTitleSeguidos.setTextColor(Color.WHITE);
            if (tvCountSeguidos != null) {
                tvCountSeguidos.setTextColor(Color.BLACK);
                tvCountSeguidos.setBackgroundResource(R.drawable.bg_status_dot);
            }

            if (llTabSeguidores != null) llTabSeguidores.setBackgroundResource(android.R.color.transparent);
            if (ivIconSeguidores != null) ivIconSeguidores.setColorFilter(Color.parseColor("#7A8C85"));
            if (tvTitleSeguidores != null) tvTitleSeguidores.setTextColor(Color.parseColor("#7A8C85"));
            if (tvCountSeguidores != null) {
                tvCountSeguidores.setTextColor(Color.BLACK);
                tvCountSeguidores.setBackgroundResource(R.drawable.bg_status_dot);
            }
        }
    }

    private void cargarUsuariosPorTab(int tabIndex) {
        DatabaseReference relationRef = (tabIndex == TAB_SEGUIDORES) ? seguidoresRef : seguidosRef;
        if (relationRef == null) return;

        relationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                masterUsersList.clear();
                if (snapshot.exists()) {
                    long totalKeys = snapshot.getChildrenCount();
                    if (totalKeys == 0) {
                        filtrarUsuarios(etSearchUsers != null ? etSearchUsers.getText().toString() : "");
                        return;
                    }

                    for (DataSnapshot child : snapshot.getChildren()) {
                        String userUid = child.getKey();
                        if (!TextUtils.isEmpty(userUid)) {
                            usersRef.child(userUid).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot userSnap) {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (userSnap.exists()) {
                                        User user = userSnap.getValue(User.class);
                                        if (user != null) {
                                            user.setUid(userUid);
                                            masterUsersList.add(user);
                                        }
                                    }
                                    filtrarUsuarios(etSearchUsers != null ? etSearchUsers.getText().toString() : "");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                        }
                    }
                } else {
                    filtrarUsuarios(etSearchUsers != null ? etSearchUsers.getText().toString() : "");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filtrarUsuarios(String query) {
        displayUsersList.clear();
        String lowerQuery = query.toLowerCase().trim();

        for (User user : masterUsersList) {
            String name = user.getFullName() != null ? user.getFullName().toLowerCase() : "";
            String career = user.getCareer() != null ? user.getCareer().toLowerCase() : "";
            String university = user.getUniversity() != null ? user.getUniversity().toLowerCase() : "";

            if (TextUtils.isEmpty(lowerQuery) || name.contains(lowerQuery) || career.contains(lowerQuery) || university.contains(lowerQuery)) {
                displayUsersList.add(user);
            }
        }

        if (displayUsersList.isEmpty()) {
            if (tvEmptyUsers != null) tvEmptyUsers.setVisibility(View.VISIBLE);
            if (rvUsersList != null) rvUsersList.setVisibility(View.GONE);
        } else {
            if (tvEmptyUsers != null) tvEmptyUsers.setVisibility(View.GONE);
            if (rvUsersList != null) rvUsersList.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (seguidoresRef != null && seguidoresListener != null) {
            seguidoresRef.removeEventListener(seguidoresListener);
        }
        if (seguidosRef != null && seguidosListener != null) {
            seguidosRef.removeEventListener(seguidosListener);
        }
    }
}
