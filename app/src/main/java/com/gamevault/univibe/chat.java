package com.gamevault.univibe;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class chat extends AppCompatActivity {

    private ImageButton ibBack, ibCall, ibVideo;
    private ShapeableImageView ivHeaderAvatar;
    private TextView tvHeaderName, tvHeaderStatus;
    private EditText etChatInput;
    private ImageView ibAttachMedia;
    private FloatingActionButton btnSendMessage;
    private RecyclerView rvChatMessages;

    private FirebaseAuth mAuth;
    private DatabaseReference targetUserRef, chatRoomRef, myConversationsRef, targetConversationsRef, currentUserRef;
    private ValueEventListener targetUserListener, chatRoomListener, currentUserListener;

    private String currentUserId = "";
    private String currentUserName = "";
    private String currentUserPhotoUrl = "";
    private String targetUserId = "";
    private String targetUserName = "";
    private String targetPhotoUrl = "";
    private String chatRoomId = "";

    private List<Mensaje> mensajeList;
    private ChatAdapter chatAdapter;

    private final ActivityResultLauncher<String> pickChatImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    subirImagenYEnviarMensaje(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_chat);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomPadding = Math.max(systemBars.bottom, ime.bottom);
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding);
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
            Toast.makeText(this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatRoomId = currentUserId.compareTo(targetUserId) < 0
                ? currentUserId + "_" + targetUserId
                : targetUserId + "_" + currentUserId;

        initViews();
        initFirebase();
        setupListeners();
        cargarDatosUsuarioActual();
        cargarDatosTargetUser();
        cargarHistorialMensajes();
        marcarChatComoLeido();
    }

    private void initViews() {
        ibBack = findViewById(R.id.ib_back_chat);
        ibCall = findViewById(R.id.ib_call_chat);
        ibVideo = findViewById(R.id.ib_video_chat);

        ivHeaderAvatar = findViewById(R.id.iv_chat_header_avatar);
        tvHeaderName = findViewById(R.id.tv_chat_header_name);
        tvHeaderStatus = findViewById(R.id.tv_chat_header_status);

        etChatInput = findViewById(R.id.et_chat_input);
        ibAttachMedia = findViewById(R.id.ib_attach_media);
        btnSendMessage = findViewById(R.id.btn_send_message);
        rvChatMessages = findViewById(R.id.rv_chat_messages);

        mensajeList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, mensajeList, targetPhotoUrl);

        if (rvChatMessages != null) {
            LinearLayoutManager llm = new LinearLayoutManager(this);
            llm.setStackFromEnd(true);
            rvChatMessages.setLayoutManager(llm);
            rvChatMessages.setAdapter(chatAdapter);
        }
    }

    private void initFirebase() {
        currentUserRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(currentUserId);
        targetUserRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(targetUserId);
        chatRoomRef = FirebaseDatabase.getInstance().getReference("chats").child(chatRoomId).child("mensajes");
        myConversationsRef = FirebaseDatabase.getInstance().getReference("conversaciones").child(currentUserId).child(targetUserId);
        targetConversationsRef = FirebaseDatabase.getInstance().getReference("conversaciones").child(targetUserId).child(currentUserId);
    }

    private void setupListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        if (btnSendMessage != null) {
            btnSendMessage.setOnClickListener(v -> enviarMensajeTexto());
        }

        if (ibAttachMedia != null) {
            ibAttachMedia.setOnClickListener(v -> pickChatImageLauncher.launch("image/*"));
        }

        if (ibCall != null) {
            ibCall.setOnClickListener(v -> Toast.makeText(this, "Llamadas no disponibles por ahora", Toast.LENGTH_SHORT).show());
        }

        if (ibVideo != null) {
            ibVideo.setOnClickListener(v -> Toast.makeText(this, "🎥 Videollamadas no disponibles por ahora", Toast.LENGTH_SHORT).show());
        }
    }

    private void cargarDatosUsuarioActual() {
        currentUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;
                if (snapshot.exists()) {
                    currentUserName = snapshot.child("fullName").getValue(String.class);
                    currentUserPhotoUrl = snapshot.child("imgPerfil").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        currentUserRef.addValueEventListener(currentUserListener);
    }

    private void cargarDatosTargetUser() {
        targetUserListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                if (snapshot.exists()) {
                    targetUserName = snapshot.child("fullName").getValue(String.class);
                    targetPhotoUrl = snapshot.child("imgPerfil").getValue(String.class);
                    Boolean isOnline = snapshot.child("online").getValue(Boolean.class);

                    if (tvHeaderName != null) {
                        tvHeaderName.setText(!TextUtils.isEmpty(targetUserName) ? targetUserName : "Usuario");
                    }

                    if (tvHeaderStatus != null) {
                        if (Boolean.TRUE.equals(isOnline)) {
                            tvHeaderStatus.setText("En línea");
                            tvHeaderStatus.setVisibility(View.VISIBLE);
                        } else {
                            tvHeaderStatus.setVisibility(View.GONE);
                        }
                    }

                    if (!TextUtils.isEmpty(targetPhotoUrl) && ivHeaderAvatar != null) {
                        Glide.with(chat.this)
                                .load(targetPhotoUrl)
                                .circleCrop()
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .into(ivHeaderAvatar);
                    }

                    chatAdapter = new ChatAdapter(chat.this, mensajeList, targetPhotoUrl);
                    rvChatMessages.setAdapter(chatAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        targetUserRef.addValueEventListener(targetUserListener);
    }

    private void cargarHistorialMensajes() {
        chatRoomListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed()) return;

                mensajeList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        try {
                            Mensaje mensaje = child.getValue(Mensaje.class);
                            if (mensaje != null) {
                                mensajeList.add(mensaje);
                            }
                        } catch (Exception e) {}
                    }
                }

                // Sort timestamp ascending (chronological order)
                Collections.sort(mensajeList, (m1, m2) -> Long.compare(m1.getTimestamp(), m2.getTimestamp()));

                chatAdapter.notifyDataSetChanged();
                if (!mensajeList.isEmpty() && rvChatMessages != null) {
                    rvChatMessages.smoothScrollToPosition(mensajeList.size() - 1);
                }
                marcarChatComoLeido();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        chatRoomRef.addValueEventListener(chatRoomListener);
    }

    private void enviarMensajeTexto() {
        String texto = etChatInput.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) return;

        String messageId = chatRoomRef.push().getKey();
        if (messageId == null) return;

        long timestamp = System.currentTimeMillis();

        Mensaje nuevoMensaje = new Mensaje(
                messageId,
                currentUserId,
                targetUserId,
                texto,
                "",
                timestamp,
                "TEXT"
        );

        chatRoomRef.child(messageId).setValue(nuevoMensaje).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                etChatInput.setText("");
                actualizarResumenConversacion(texto, timestamp);
            }
        });
    }

    private void subirImagenYEnviarMensaje(Uri imageUri) {
        if (imageUri == null) return;

        Toast.makeText(this, R.string.msg_publishing, Toast.LENGTH_SHORT).show();

        String messageId = chatRoomRef.push().getKey();
        if (messageId == null) return;

        StorageReference chatImageRef = FirebaseStorage.getInstance().getReference("chats").child(chatRoomId).child(messageId + ".jpg");

        chatImageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                chatImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    String imageUrl = downloadUri.toString();
                    long timestamp = System.currentTimeMillis();

                    Mensaje nuevoMensaje = new Mensaje(
                            messageId,
                            currentUserId,
                            targetUserId,
                            "",
                            imageUrl,
                            timestamp,
                            "IMAGE"
                    );

                    chatRoomRef.child(messageId).setValue(nuevoMensaje).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            actualizarResumenConversacion("📷 Foto", timestamp);
                        }
                    });
                })
        );
    }

    private void marcarChatComoLeido() {
        if (myConversationsRef != null) {
            myConversationsRef.child("unreadCount").setValue(0);
        }
    }

    private void actualizarResumenConversacion(String ultimoMensajeText, long timestamp) {
        Conversacion miResumen = new Conversacion(
                targetUserId,
                targetUserName,
                targetPhotoUrl,
                ultimoMensajeText,
                timestamp,
                0,
                true
        );

        myConversationsRef.setValue(miResumen);

        targetConversationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int currentUnread = 0;
                if (snapshot.exists() && snapshot.hasChild("unreadCount")) {
                    Integer val = snapshot.child("unreadCount").getValue(Integer.class);
                    if (val != null) currentUnread = val;
                }
                int newUnread = currentUnread + 1;

                Conversacion suResumen = new Conversacion(
                        currentUserId,
                        currentUserName,
                        currentUserPhotoUrl,
                        ultimoMensajeText,
                        timestamp,
                        newUnread,
                        true
                );

                targetConversationsRef.setValue(suResumen);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
        if (targetUserRef != null && targetUserListener != null) {
            targetUserRef.removeEventListener(targetUserListener);
        }
        if (currentUserRef != null && currentUserListener != null) {
            currentUserRef.removeEventListener(currentUserListener);
        }
        if (chatRoomRef != null && chatRoomListener != null) {
            chatRoomRef.removeEventListener(chatRoomListener);
        }
    }
}
