package com.gamevault.univibe;

public class Conversacion {
    private String chatUserId;
    private String chatUserName;
    private String chatUserPhotoUrl;
    private String lastMessage;
    private long lastMessageTimestamp;
    private int unreadCount;
    private boolean isOnline;

    public Conversacion() {
        // Required empty constructor for Firebase DataSnapshot.getValue(Conversacion.class)
    }

    public Conversacion(String chatUserId, String chatUserName, String chatUserPhotoUrl, String lastMessage, long lastMessageTimestamp, int unreadCount, boolean isOnline) {
        this.chatUserId = chatUserId;
        this.chatUserName = chatUserName;
        this.chatUserPhotoUrl = chatUserPhotoUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.unreadCount = unreadCount;
        this.isOnline = isOnline;
    }

    public String getChatUserId() { return chatUserId != null ? chatUserId : ""; }
    public void setChatUserId(String chatUserId) { this.chatUserId = chatUserId; }

    public String getChatUserName() { return chatUserName != null ? chatUserName : ""; }
    public void setChatUserName(String chatUserName) { this.chatUserName = chatUserName; }

    public String getChatUserPhotoUrl() { return chatUserPhotoUrl != null ? chatUserPhotoUrl : ""; }
    public void setChatUserPhotoUrl(String chatUserPhotoUrl) { this.chatUserPhotoUrl = chatUserPhotoUrl; }

    public String getLastMessage() { return lastMessage != null ? lastMessage : ""; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) { this.lastMessageTimestamp = lastMessageTimestamp; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
}
