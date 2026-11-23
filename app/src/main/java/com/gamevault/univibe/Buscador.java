package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Buscador extends AppCompatActivity {

    private EditText etSearchQuery;
    private ImageView ivClearSearch;
    private RecyclerView rvSearchResults;
    private ProgressBar pbSearchLoading;
    private MaterialCardView cardEmptySearch;
    private View clInitialSearchState;

    private LinearLayout llRecentSearchesContainer;
    private RecyclerView rvRecentSearches;
    private TextView tvClearAllHistory;
    private HistorialBusquedaAdapter historialAdapter;
    private final List<String> historialList = new ArrayList<>();

    private LinearLayout llNavInicio, llNavBuscar, llNavCrear, llNavAlertas, llNavPerfil;

    private SeguidoresSeguidosAdapter adapter;
    private final List<User> allUsersList = new ArrayList<>();
    private final List<User> filteredUsersList = new ArrayList<>();

    private DatabaseReference usersRef;
    private ValueEventListener usersListener;
    private String currentUid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_buscador);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUid = auth.getCurrentUser().getUid();
        }

        initViews();
        setupRecyclerViews();
        setupListeners();
        setupBottomNav();
        cargarYMostrarHistorial();
        cargarUsuariosFirebase();
    }

    private void initViews() {
        etSearchQuery = findViewById(R.id.et_search_query);
        ivClearSearch = findViewById(R.id.iv_clear_search);
        rvSearchResults = findViewById(R.id.rv_search_results);
        pbSearchLoading = findViewById(R.id.pb_search_loading);
        cardEmptySearch = findViewById(R.id.card_empty_search);
        clInitialSearchState = findViewById(R.id.cl_initial_search_state);

        llRecentSearchesContainer = findViewById(R.id.ll_recent_searches_container);
        rvRecentSearches = findViewById(R.id.rv_recent_searches);
        tvClearAllHistory = findViewById(R.id.tv_clear_all_history);

        llNavInicio = findViewById(R.id.ll_nav_inicio);
        llNavBuscar = findViewById(R.id.ll_nav_buscar);
        View flCenterNav = findViewById(R.id.fl_nav_center);
        if (flCenterNav instanceof LinearLayout) {
            llNavCrear = (LinearLayout) flCenterNav;
        }
        llNavAlertas = findViewById(R.id.ll_nav_alertas);
        llNavPerfil = findViewById(R.id.ll_nav_perfil);
    }

    private void setupRecyclerViews() {
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SeguidoresSeguidosAdapter(this, filteredUsersList);
        rvSearchResults.setAdapter(adapter);

        if (rvRecentSearches != null) {
            rvRecentSearches.setLayoutManager(new LinearLayoutManager(this));
            historialAdapter = new HistorialBusquedaAdapter(historialList, new HistorialBusquedaAdapter.OnHistorialItemClickListener() {
                @Override
                public void onTermClick(String term) {
                    if (etSearchQuery != null) {
                        etSearchQuery.setText(term);
                        etSearchQuery.setSelection(term.length());
                        ocultarTeclado();

                        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);
                        if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
                        if (cardEmptySearch != null) cardEmptySearch.setVisibility(View.GONE);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                            filtrarUsuarios(term);
                        }, 400);
                    }
                }

                @Override
                public void onTermDelete(String term) {
                    HistorialBusquedaManager.eliminarTermino(Buscador.this, term);
                    cargarYMostrarHistorial();
                }
            });
            rvRecentSearches.setAdapter(historialAdapter);
        }
    }

    private void setupListeners() {
        if (etSearchQuery != null) {
            etSearchQuery.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filtrarUsuarios(query);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
                boolean isEnter = (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN);
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || isEnter) {
                    ocultarTeclado();
                    String query = etSearchQuery.getText().toString().trim();
                    if (!TextUtils.isEmpty(query)) {
                        HistorialBusquedaManager.guardarTermino(Buscador.this, query);

                        if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.VISIBLE);
                        if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
                        if (cardEmptySearch != null) cardEmptySearch.setVisibility(View.GONE);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (pbSearchLoading != null) pbSearchLoading.setVisibility(View.GONE);
                            filtrarUsuarios(query);
                        }, 400);
                    } else {
                        filtrarUsuarios("");
                    }
                    return true;
                }
                return false;
            });
        }

        if (ivClearSearch != null) {
            ivClearSearch.setOnClickListener(v -> {
                if (etSearchQuery != null) {
                    etSearchQuery.setText("");
                }
            });
        }

        if (tvClearAllHistory != null) {
            tvClearAllHistory.setOnClickListener(v -> {
                HistorialBusquedaManager.limpiarHistorial(Buscador.this);
                cargarYMostrarHistorial();
            });
        }
    }

    private void ocultarTeclado() {
        View view = getCurrentFocus();
        if (view == null) {
            view = etSearchQuery;
        }
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void cargarYMostrarHistorial() {
        List<String> reciente = HistorialBusquedaManager.obtenerHistorial(this);
        historialList.clear();
        if (reciente != null) {
            historialList.addAll(reciente);
        }

        if (historialAdapter != null) {
            historialAdapter.notifyDataSetChanged();
        }

        String currentQuery = etSearchQuery != null ? etSearchQuery.getText().toString().trim() : "";

        if (TextUtils.isEmpty(currentQuery)) {
            if (!historialList.isEmpty()) {
                if (llRecentSearchesContainer != null) llRecentSearchesContainer.setVisibility(View.VISIBLE);
                if (clInitialSearchState != null) clInitialSearchState.setVisibility(View.GONE);
            } else {
                if (llRecentSearchesContainer != null) llRecentSearchesContainer.setVisibility(View.GONE);
                if (clInitialSearchState != null) clInitialSearchState.setVisibility(View.VISIBLE);
            }
            if (rvSearchResults != null) rvSearchResults.setVisibility(View.GONE);
            if (cardEmptySearch != null) cardEmptySearch.setVisibility(View.GONE);
        } else {
            if (llRecentSearchesContainer != null) llRecentSearchesContainer.setVisibility(View.GONE);
            if (clInitialSearchState != null) clInitialSearchState.setVisibility(View.GONE);
        }
    }

    private void cargarUsuariosFirebase() {
        pbSearchLoading.setVisibility(View.VISIBLE);
        usersRef = FirebaseDatabase.getInstance().getReference("registroUsuario");

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                pbSearchLoading.setVisibility(View.GONE);
                allUsersList.clear();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    if (uid != null && !uid.equals(currentUid)) {
                        User user = userSnap.getValue(User.class);
                        if (user != null) {
                            user.setUid(uid);
                            allUsersList.add(user);
                        }
                    }
                }

                String currentQuery = etSearchQuery != null ? etSearchQuery.getText().toString().trim() : "";
                filtrarUsuarios(currentQuery);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isFinishing() || isDestroyed()) return;
                pbSearchLoading.setVisibility(View.GONE);
            }
        };

        usersRef.addValueEventListener(usersListener);
    }

    private void filtrarUsuarios(String query) {
        String normalizedQuery = desacentuar(query.toLowerCase(Locale.ROOT));

        if (TextUtils.isEmpty(normalizedQuery)) {
            filteredUsersList.clear();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            cargarYMostrarHistorial();
            return;
        }

        if (llRecentSearchesContainer != null) {
            llRecentSearchesContainer.setVisibility(View.GONE);
        }
        if (clInitialSearchState != null) {
            clInitialSearchState.setVisibility(View.GONE);
        }

        filteredUsersList.clear();

        for (User user : allUsersList) {
            if (user == null) continue;

            String name = desacentuar((user.getFullName() != null ? user.getFullName() : "").toLowerCase(Locale.ROOT));
            String email = desacentuar((user.getInstitutionalEmail() != null ? user.getInstitutionalEmail() : "").toLowerCase(Locale.ROOT));
            String uni = desacentuar((user.getUniversity() != null ? user.getUniversity() : "").toLowerCase(Locale.ROOT));
            String career = desacentuar((user.getCareer() != null ? user.getCareer() : "").toLowerCase(Locale.ROOT));

            if (name.contains(normalizedQuery) ||
                    email.contains(normalizedQuery) ||
                    uni.contains(normalizedQuery) ||
                    career.contains(normalizedQuery)) {
                filteredUsersList.add(user);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        if (filteredUsersList.isEmpty()) {
            cardEmptySearch.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
        } else {
            cardEmptySearch.setVisibility(View.GONE);
            rvSearchResults.setVisibility(View.VISIBLE);
        }
    }

    private String desacentuar(String texto) {
        if (texto == null) return "";
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private void setupBottomNav() {
        if (llNavInicio != null) {
            llNavInicio.setOnClickListener(v -> {
                Intent intent = new Intent(Buscador.this, panelPrincipal.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
            });
        }

        View flCenterNav = findViewById(R.id.fl_nav_center);
        if (flCenterNav != null) {
            flCenterNav.setOnClickListener(v -> {
                Intent intent = new Intent(Buscador.this, PublicacionContenido.class);
                startActivity(intent);
            });
        }

        if (llNavAlertas != null) {
            llNavAlertas.setOnClickListener(v -> {
                Intent intent = new Intent(Buscador.this, alertas.class);
                startActivity(intent);
            });
        }

        if (llNavPerfil != null) {
            llNavPerfil.setOnClickListener(v -> {
                Intent intent = new Intent(Buscador.this, PefilUser.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentQuery = etSearchQuery != null ? etSearchQuery.getText().toString().trim() : "";
        if (TextUtils.isEmpty(currentQuery)) {
            cargarYMostrarHistorial();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
}
