package com.gamevault.univibe;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class NotificacionesBackgroundService extends Service {

    public static final String CHANNEL_ALERTS_ID = "univibe_notifications_channel";
    public static final String CHANNEL_MESSAGES_ID = "univibe_messages_channel";

    private DatabaseReference notifRef, conversationsRef;
    private ChildEventListener childEventListener, conversationsChildListener;
    private String currentUserId = "";
    private long serviceStartTime = 0;
    private final Map<String, Long> lastNotifiedMessageTimestamps = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        serviceStartTime = System.currentTimeMillis() - 5000; // allow 5s leeway
        crearCanalesNotificaciones();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        iniciarOyentesFirebase();
        return START_STICKY;
    }

    private void iniciarOyentesFirebase() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            stopSelf();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        iniciarOyenteAlertas();
        iniciarOyenteMensajes();
    }

    private void iniciarOyenteAlertas() {
        if (notifRef != null && childEventListener != null) {
            notifRef.removeEventListener(childEventListener);
        }

        notifRef = FirebaseDatabase.getInstance().getReference("notificaciones").child(currentUserId);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    Notificacion notif = snapshot.getValue(Notificacion.class);
                    if (notif != null && notif.getTimestamp() >= serviceStartTime) {
                        mostrarNotificacionAlertaSistema(notif);
                        reproducirSonidoAlerta();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        notifRef.addChildEventListener(childEventListener);
    }

    private void iniciarOyenteMensajes() {
        if (conversationsRef != null && conversationsChildListener != null) {
            conversationsRef.removeEventListener(conversationsChildListener);
        }

        conversationsRef = FirebaseDatabase.getInstance().getReference("conversaciones").child(currentUserId);

        conversationsChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                procesarNotificacionMensaje(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                procesarNotificacionMensaje(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        conversationsRef.addChildEventListener(conversationsChildListener);
    }

    private void procesarNotificacionMensaje(DataSnapshot snapshot) {
        if (!snapshot.exists()) return;

        try {
            Conversacion conv = snapshot.getValue(Conversacion.class);
            if (conv == null || conv.getUnreadCount() <= 0) return;

            String fromUid = conv.getChatUserId();
            long timestamp = conv.getLastMessageTimestamp();

            Long lastNotified = lastNotifiedMessageTimestamps.get(fromUid);
            if (lastNotified != null && lastNotified >= timestamp) {
                return;
            }

            if (timestamp < serviceStartTime) return;

            lastNotifiedMessageTimestamps.put(fromUid, timestamp);

            mostrarNotificacionMensajeSistema(conv);
            reproducirSonidoMensaje();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarNotificacionAlertaSistema(Notificacion notif) {
        if (notif == null) return;

        String senderName = !TextUtils.isEmpty(notif.getFromUserName()) ? notif.getFromUserName() : "Un usuario";
        String messageText = notif.getMessage();
        String fullText = senderName + " " + messageText;

        Intent targetIntent;
        if ("FOLLOW".equals(notif.getType()) && !TextUtils.isEmpty(notif.getFromUid())) {
            targetIntent = new Intent(this, PefilUser.class);
            targetIntent.putExtra("targetUid", notif.getFromUid());
        } else if (!TextUtils.isEmpty(notif.getPostId())) {
            targetIntent = new Intent(this, listaComentarios.class);
            targetIntent.putExtra("postId", notif.getPostId());
        } else {
            targetIntent = new Intent(this, panelPrincipal.class);
        }

        targetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                targetIntent,
                flags
        );

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonido_notificacion);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ALERTS_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setContentTitle("UniVibe")
                .setContentText(fullText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(fullText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void mostrarNotificacionMensajeSistema(Conversacion conv) {
        if (conv == null) return;

        String senderName = !TextUtils.isEmpty(conv.getChatUserName()) ? conv.getChatUserName() : "Un usuario";
        String lastMsg = conv.getLastMessage();
        String messageText;

        if ("📷 Foto".equals(lastMsg) || (lastMsg != null && lastMsg.startsWith("http"))) {
            messageText = senderName + " te envió una foto";
        } else if (!TextUtils.isEmpty(lastMsg)) {
            messageText = lastMsg;
        } else {
            messageText = senderName + " te envió un mensaje";
        }

        Intent intent = new Intent(this, chat.class);
        intent.putExtra("targetUid", conv.getChatUserId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int notificationId = conv.getChatUserId().hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                intent,
                flags
        );

        Uri soundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mensaje);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setContentTitle(senderName)
                .setContentText(messageText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri)
                .setGroup("univibe_chat_group")
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());

            // Group summary notification
            NotificationCompat.Builder summaryBuilder = new NotificationCompat.Builder(this, CHANNEL_MESSAGES_ID)
                    .setSmallIcon(R.drawable.logo)
                    .setContentTitle("UniVibe - Mensajes")
                    .setContentText("Tienes nuevos mensajes")
                    .setStyle(new NotificationCompat.InboxStyle().setSummaryText("Mensajes sin leer"))
                    .setGroup("univibe_chat_group")
                    .setGroupSummary(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            notificationManager.notify(99999, summaryBuilder.build());
        }
    }

    private void reproducirSonidoAlerta() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sonido_notificacion);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reproducirSonidoMensaje() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.mensaje);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void crearCanalesNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Channel 1: General Alerts
            Uri alertSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.sonido_notificacion);
            AudioAttributes alertAudioAttr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel alertChannel = new NotificationChannel(
                    CHANNEL_ALERTS_ID,
                    "Notificaciones UniVibe",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertChannel.setDescription("Canal para alertas y notificaciones de UniVibe");
            alertChannel.setSound(alertSoundUri, alertAudioAttr);
            alertChannel.enableVibration(true);

            manager.createNotificationChannel(alertChannel);

            // Channel 2: Chat Messages
            Uri msgSoundUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.mensaje);
            AudioAttributes msgAudioAttr = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            NotificationChannel msgChannel = new NotificationChannel(
                    CHANNEL_MESSAGES_ID,
                    "Mensajes de UniVibe",
                    NotificationManager.IMPORTANCE_HIGH
            );
            msgChannel.setDescription("Canal para mensajes de chat de UniVibe");
            msgChannel.setSound(msgSoundUri, msgAudioAttr);
            msgChannel.enableVibration(true);

            manager.createNotificationChannel(msgChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notifRef != null && childEventListener != null) {
            notifRef.removeEventListener(childEventListener);
        }
        if (conversationsRef != null && conversationsChildListener != null) {
            conversationsRef.removeEventListener(conversationsChildListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
