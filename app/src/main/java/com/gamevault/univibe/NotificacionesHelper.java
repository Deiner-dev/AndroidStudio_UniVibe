package com.gamevault.univibe;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificacionesHelper {

    public static void registrarNotificacion(
            String targetUid,
            String type,
            String postId,
            String commentId,
            String replyId,
            String message
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String fromUid = auth.getCurrentUser().getUid();

        // Do not send notification to oneself
        if (TextUtils.isEmpty(targetUid) || targetUid.equals(fromUid)) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("registroUsuario").child(fromUid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fromUserName = snapshot.child("fullName").getValue(String.class);
                String fromUserPhotoUrl = snapshot.child("imgPerfil").getValue(String.class);

                if (TextUtils.isEmpty(fromUserName)) {
                    fromUserName = "Un usuario";
                }

                DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(targetUid);
                String notifId = notifRef.push().getKey();
                if (notifId == null) return;

                Notificacion notificacion = new Notificacion(
                        notifId,
                        targetUid,
                        fromUid,
                        fromUserName,
                        fromUserPhotoUrl,
                        type,
                        postId,
                        commentId,
                        replyId,
                        message,
                        System.currentTimeMillis(),
                        false
                );

                notifRef.child(notifId).setValue(notificacion);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void iniciarServicioNotificaciones(Context context) {
        if (context == null) return;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Intent intent = new Intent(context, NotificacionesBackgroundService.class);
            try {
                context.startService(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void detenerServicioNotificaciones(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, NotificacionesBackgroundService.class);
        try {
            context.stopService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
