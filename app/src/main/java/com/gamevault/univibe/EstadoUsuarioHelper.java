package com.gamevault.univibe;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class EstadoUsuarioHelper {

    public static void setOnline(boolean online) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference userOnlineRef = FirebaseDatabase.getInstance()
                .getReference("registroUsuario")
                .child(uid)
                .child("online");

        if (online) {
            userOnlineRef.setValue(true);
            userOnlineRef.onDisconnect().setValue(false);
        } else {
            userOnlineRef.setValue(false);
        }
    }
}
